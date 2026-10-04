# Multi-Tenant SaaS Starter Kit

A reusable multi-tenant backend skeleton for launching a SaaS MVP: users sign in,
every request is bound to exactly one tenant taken from a verified token, and all
data lives in a DynamoDB single table where every key starts with the tenant id.

Built as a portfolio piece and as a starting point to reuse across client SaaS
builds (AWS cost tools for SMBs, provider/job management apps, and similar).

**Stack:** Java 21 · Spring Boot · Amazon Cognito · API Gateway (HTTP API) ·
AWS Lambda (SnapStart) · DynamoDB (single table) · Stripe (stubbed) · AWS CDK (Python) · JUnit 5

> **Status: skeleton.** The structure, tenant-isolation mechanics and one sample
> resource are in place; onboarding, invites and real Stripe billing are stubs that
> answer `501 Not Implemented`. Nothing has been compiled or deployed yet.

## Architecture

```text
   Client (web / mobile)
        │  Authorization: Bearer <Cognito ID token>
        ▼
  API Gateway HTTP API ── JWT authorizer (Cognito) ── rejects bad/expired tokens
        │                  (public: /health, /signup, /webhooks/stripe)
        ▼
  Lambda  ── Spring Boot via aws-serverless-java-container
        │
        ├─ Spring Security      re-validates the JWT (defence in depth);
        │                       401/403 rendered as the same ApiError shape
        │                       as every other error
        ├─ TenantContextFilter  custom:tenant_id claim  ──►  TenantContext
        ├─ Controllers          never accept a tenant id from the client
        ├─ Services
        └─ TenantScopedRepository   pk = TENANT#<tenantId from TenantContext>
                 │
                 ▼
          DynamoDB  saas_main  (pk, sk, gsi1)
```

## How tenant isolation works

1. **Identity** - each Cognito user has an immutable `custom:tenant_id` attribute set
   by the backend at creation; the app client can read it but not write it.
2. **Gateway + app both verify the token** - the tenant claim is only trusted after
   signature, expiry and issuer checks.
3. **`TenantContext`** is filled from that claim and nothing else, then cleared after
   every request.
4. **`TenantScopedRepository`** builds every partition key from `TenantContext`.
   There is no tenant-id parameter anywhere in the data-access API, so cross-tenant
   access isn't a mistake a caller can make. `SingleTableKeys` independently
   re-validates the tenant id before building a key, rather than trusting the caller.

Full reasoning, key design and trade-offs: [`docs/architecture.md`](docs/architecture.md).

## What's implemented vs stubbed

| Area | State |
|---|---|
| JWT validation, tenant binding, role checks (`TENANT_ADMIN`) | Implemented |
| Consistent error responses (`ApiError`) for 400/401/403/404/501 | Implemented |
| `TenantScopedRepository`, single-table key design | Implemented |
| SnapStart-safe DynamoDB client lifecycle (`DynamoDbClientHolder`) | Implemented |
| Sample tenant-owned resource (`/notes` CRUD, author-only delete) | Implemented |
| `GET /tenants/current`, `GET /users` | Implemented |
| `Role` enum (`TENANT_ADMIN`/`MEMBER`) for invite requests | Implemented |
| CDK stack (table, user pool, Lambda, HTTP API, Cognito IAM grant) | Written, not yet synthesised |
| `POST /signup` (tenant onboarding), `POST /users/invite` | Stub - 501 |
| Billing (`/billing/*`, `/webhooks/stripe`) | Stub - fake data, no Stripe calls |
| Tenant isolation integration test | Skeleton, `@Disabled` |

## Project structure

```text
src/main/java/com/reza/saas/
├── SaasApplication.java        # local entry point
├── StreamLambdaHandler.java    # Lambda entry point
├── config/                     # security, AWS clients, typed properties,
│                                # DynamoDbClientHolder (SnapStart lifecycle)
├── common/                     # error handling, health check
├── persistence/                # BaseItem, SingleTableKeys, TenantScopedRepository
├── tenant/                     # TenantContext + filter, tenant metadata, signup
├── user/                       # users within a tenant, Role enum
├── sample/                     # example tenant-owned resource (Note)
└── billing/                    # BillingService interface, stub, webhook endpoint

aws_components/                 # CDK (Python)
docs/architecture.md
```

## Local development

Run `SaasApplication` from your IDE with `AWS_REGION`, `TABLE_NAME` and
`COGNITO_USER_POOL_ID` set and AWS credentials available. Protected routes need a
real Cognito token, so deploy the stack once (below) and use its user pool.

```bash
mvn test
```

## Deploying

```bash
mvn clean package
cd aws_components
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cdk deploy
```

## Roadmap

- [ ] Tenant onboarding: `/signup` -> tenant item + Cognito admin user + `TENANT_ADMIN` group
      (needs the `cognitoidentityprovider` SDK dependency uncommented in `pom.xml`)
- [ ] User invites with plan-limit enforcement (`Plan.maxUsers`) and the planned
      `gsi1pk = EMAIL#<email>` pre-check for cross-tenant email collisions
- [ ] Real Stripe billing: checkout, signature-verified webhooks, subscription state
- [ ] Tenant isolation integration tests against DynamoDB Local
- [ ] CORS configuration (API Gateway or Spring Security) - blocked on a frontend origin
- [ ] Per-tenant throttling / usage limits
- [ ] IAM `dynamodb:LeadingKeys` scoping via per-request STS session policies
- [ ] CloudWatch alarms and structured logs carrying the tenant id

## License

MIT