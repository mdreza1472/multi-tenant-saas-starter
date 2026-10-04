from aws_cdk import (
    Stack,
    Duration,
    RemovalPolicy,
    CfnOutput,
    aws_dynamodb as dynamodb,
    aws_cognito as cognito,
    aws_lambda as _lambda,
    aws_apigatewayv2 as apigwv2,
    aws_apigatewayv2_integrations as integrations,
    aws_apigatewayv2_authorizers as authorizers,
    aws_iam as iam,
)
from constructs import Construct


class SaasStarterStack(Stack):
    """
    Client -> API Gateway (HTTP API, Cognito JWT authorizer) -> Lambda (Spring Boot)
           -> DynamoDB single table (every key prefixed TENANT#<tenantId>).

    Tenant identity is a custom:tenant_id attribute on the Cognito user (immutable,
    not writable by the client), which shows up as a claim in the ID token. See
    docs/architecture.md for how it is enforced at each layer.
    """

    def __init__(self, scope: Construct, construct_id: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        jar_path = "../target/multi-tenant-saas-starter.jar"

        # ---- Single table: pk = TENANT#<id>, sk = <ENTITY>#<id> ----
        table = dynamodb.Table(
            self,
            "MainTable",
            partition_key=dynamodb.Attribute(name="pk", type=dynamodb.AttributeType.STRING),
            sort_key=dynamodb.Attribute(name="sk", type=dynamodb.AttributeType.STRING),
            billing_mode=dynamodb.BillingMode.PAY_PER_REQUEST,
            point_in_time_recovery_specification=dynamodb.PointInTimeRecoverySpecification(
                point_in_time_recovery_enabled=True
            ),
            removal_policy=RemovalPolicy.RETAIN,  # never auto-delete tenant data on stack teardown
        )
        # Generic secondary access pattern (e.g. email -> tenant, Stripe customer -> tenant).
        table.add_global_secondary_index(
            index_name="gsi1",
            partition_key=dynamodb.Attribute(name="gsi1pk", type=dynamodb.AttributeType.STRING),
            sort_key=dynamodb.Attribute(name="gsi1sk", type=dynamodb.AttributeType.STRING),
        )

        # ---- Cognito: ONE shared user pool, tenant carried as a custom attribute ----
        user_pool = cognito.UserPool(
            self,
            "UserPool",
            self_sign_up_enabled=False,  # accounts are created by the backend (onboarding/invites)
            sign_in_aliases=cognito.SignInAliases(email=True),
            standard_attributes=cognito.StandardAttributes(
                email=cognito.StandardAttribute(required=True, mutable=True),
            ),
            # mutable=False: set once at user creation by the backend, can never be changed after.
            custom_attributes={
                "tenant_id": cognito.StringAttribute(min_len=8, max_len=64, mutable=False),
            },
            password_policy=cognito.PasswordPolicy(min_length=12),
            account_recovery=cognito.AccountRecovery.EMAIL_ONLY,
            removal_policy=RemovalPolicy.RETAIN,
        )

        # Groups double as roles inside a user's (single) tenant.
        for group_name in ("TENANT_ADMIN", "MEMBER"):
            cognito.CfnUserPoolGroup(
                self,
                f"{group_name}Group",
                user_pool_id=user_pool.user_pool_id,
                group_name=group_name,
            )

        web_client = user_pool.add_client(
            "WebClient",
            generate_secret=False,
            auth_flows=cognito.AuthFlow(user_srp=True),
            # The client may READ custom:tenant_id (so it lands in the token) but has no
            # write access to it - a user can't re-home themselves into another tenant.
            read_attributes=cognito.ClientAttributes()
            .with_standard_attributes(email=True, email_verified=True)
            .with_custom_attributes("tenant_id"),
            write_attributes=cognito.ClientAttributes().with_standard_attributes(email=True),
        )

        # ---- Spring Boot on Lambda ----
        api_fn = _lambda.Function(
            self,
            "ApiFunction",
            runtime=_lambda.Runtime.JAVA_21,
            code=_lambda.Code.from_asset(jar_path),
            handler="com.reza.saas.StreamLambdaHandler::handleRequest",
            memory_size=1024,  # Spring needs far more headroom than the pipeline's 512 MB handlers
            timeout=Duration.seconds(30),
            # Snapshots the initialised JVM/Spring context to cut cold starts; only applies
            # to published versions, hence the alias below.
            snap_start=_lambda.SnapStartConf.ON_PUBLISHED_VERSIONS,
            environment={
                "TABLE_NAME": table.table_name,
                "COGNITO_USER_POOL_ID": user_pool.user_pool_id,
            },
        )

		table.grant_read_write_data(api_fn)
		
		# Scoped to this ONE user pool's ARN, not "*" : TenantService.provision() and
		# UserService.invite() will call these to create Cognito users and assign them
		# to the TENANT_ADMIN/MEMBER groups on signup/invite.
		api_fn.add_to_role_policy(
		    iam.PolicyStatement(
		        actions=[
		            "cognito-idp:AdminCreateUser",
		            "cognito-idp:AdminAddUserToGroup",
		        ],
		        resources=[user_pool.user_pool_arn],
		    )
		)
		
		live_alias = _lambda.Alias(
            self,
            "LiveAlias",
            alias_name="live",
            version=api_fn.current_version,
        )

        # ---- API Gateway HTTP API: JWT authorizer by default, a few explicit public routes ----
        authorizer = authorizers.HttpJwtAuthorizer(
            "CognitoJwtAuthorizer",
            jwt_issuer=f"https://cognito-idp.{self.region}.amazonaws.com/{user_pool.user_pool_id}",
            jwt_audience=[web_client.user_pool_client_id],
        )
        integration = integrations.HttpLambdaIntegration("ApiIntegration", live_alias)

        http_api = apigwv2.HttpApi(self, "HttpApi", default_authorizer=authorizer)

        # Everything not listed below requires a valid Cognito token at the gateway
        # (Spring Security then validates it again inside the app).
        http_api.add_routes(
            path="/{proxy+}",
            methods=[apigwv2.HttpMethod.ANY],
            integration=integration,
        )
        public = apigwv2.HttpNoneAuthorizer()
        http_api.add_routes(path="/health", methods=[apigwv2.HttpMethod.GET], integration=integration, authorizer=public)
        http_api.add_routes(path="/signup", methods=[apigwv2.HttpMethod.POST], integration=integration, authorizer=public)
        # Stripe can't send a JWT; the app verifies the Stripe-Signature header instead.
        http_api.add_routes(path="/webhooks/stripe", methods=[apigwv2.HttpMethod.POST], integration=integration, authorizer=public)

        CfnOutput(self, "ApiUrl", value=http_api.api_endpoint)
        CfnOutput(self, "UserPoolId", value=user_pool.user_pool_id)
        CfnOutput(self, "UserPoolClientId", value=web_client.user_pool_client_id)
        CfnOutput(self, "TableName", value=table.table_name)
