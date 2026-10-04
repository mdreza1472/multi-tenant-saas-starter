# Architecture Notes

## Tenancy model: shared pool, tenant claim in the token

One Cognito user pool serves every tenant. Each user carries an immutable
`custom:tenant_id` attribute (set by the backend at creation; the app client has no
write access to it), which appears as a claim in the ID token.

The alternative - a user pool per tenant - gives stronger isolation but doesn't scale
operationally (pool limits, per-tenant deployment work) and is overkill for the SMB
tenants this starter targets. The cost of the shared-pool choice is that isolation
rests on application discipline, which is why it's enforced structurally (below)
rather than by convention.

## Isolation, layer by layer

1. **API Gateway JWT authorizer** - rejects missing/invalid tokens before Lambda runs.
2. **Spring Security** - validates the token again (signature, expiry, issuer). 401s
   (bad/missing token) and 403s (failed `@PreAuthorize`) are rendered as `ApiError`
   bodies by `SecurityConfig`'s `authenticationEntryPoint`/`accessDeniedHandler` -
   `GlobalExceptionHandler` alone can't catch these, since Spring Security rejects
   the request before it reaches any controller.
3. **`TenantContextFilter`** - copies `custom:tenant_id` into `TenantContext`; an
   authenticated token without a valid tenant claim gets a 403. The id is validated
   against a strict pattern because it is concatenated into DynamoDB keys.
4. **`TenantScopedRepository`** - builds every partition key from `TenantContext`.
   No repository method accepts a tenant id, and `save()` overwrites any caller-set keys.
   `SingleTableKeys.tenantPk()` re-validates the tenant id itself (not just trusting
   `TenantContext`'s own check), so a key can never be built from a malformed id
   regardless of which caller forgot to validate first.
5. **(Roadmap) IAM leading-keys condition** - scope the Lambda's DynamoDB credentials
   per request to `TENANT#<id>` via STS session policies, so even a bug in layer 4
   can't read another tenant's partition. Distinct from the SnapStart credential
   handling below - that solves stale credentials after a restore, not per-tenant
   scoping.

### Intentional bypasses of `TenantContext`

Exactly two places legitimately run without a bound tenant, and both are explicit:

- **Tenant provisioning** (`TenantRepository.create`) - no tenant exists yet. The
  Cognito `AdminCreateUser`/`AdminAddUserToGroup` calls in `TenantService.provision()`
  happen inside this same bypass, not a separate one - the Lambda role is IAM-scoped
  to only these two actions on this one user pool (see the CDK stack).
- **Stripe webhook** - arrives with no JWT; the tenant is resolved from the verified
  event (metadata or a GSI lookup on the Stripe customer id).

Any new bypass should be treated as a design decision and listed here.

## DynamoDB key design (table `saas_main`)

| Entity | pk | sk | gsi1pk | gsi1sk |
|---|---|---|---|---|
| Tenant metadata | `TENANT#<tenantId>` | `META` | - | - |
| User | `TENANT#<tenantId>` | `USER#<cognitoSub>` | `EMAIL#<email>` (planned) | `TENANT#<tenantId>` |
| Note (sample) | `TENANT#<tenantId>` | `NOTE#<noteId>` | - | - |
| Subscription (planned) | `TENANT#<tenantId>` | `SUBSCRIPTION` | `STRIPE#<customerId>` | `TENANT#<tenantId>` |

`User`'s `gsi1pk` lets `UserService.invite()` check whether an email is already used
by another tenant with a single GSI query, before calling Cognito (which would
otherwise reject it pool-wide, but only after the external call is made).

Because every item shares the tenant's partition key, "everything for this tenant" is a
single query and tenant isolation falls out of key construction.

Trade-off: one partition key per tenant caps that tenant's throughput at DynamoDB's
per-partition limits. Fine for SMB-sized tenants; a very large tenant would need
write-sharding (e.g. `TENANT#<id>#<shard>`).

## Auth decisions

- **ID token vs access token.** The starter reads the tenant claim from the ID token,
  because Cognito puts custom attributes there by default. Access tokens don't carry
  them unless a pre-token-generation Lambda trigger injects the claim. Deciding which
  token the client sends is an open question (below).
- **Roles** are Cognito groups (`TENANT_ADMIN`, `MEMBER`) surfaced through the
  `cognito:groups` claim and checked with `@PreAuthorize`. Groups are pool-wide, so
  this only works while a user belongs to one tenant.

## Infrastructure decisions

- **HTTP API, not REST API** - cheaper and simpler, with a native JWT authorizer. The
  cost: HTTP APIs have no usage plans/API keys, so per-tenant throttling has to be
  built in the app (or the API moved to REST API later).
- **Lambda + SnapStart, not a container** - no idle cost, which suits a portfolio
  project and low-traffic MVPs. Spring cold starts are the price; SnapStart (with eager
  initialisation, so the snapshot contains the fully built context) mitigates it. A
  container (Fargate/App Runner) is the escape hatch if latency or long-lived
  connections matter.
- **SnapStart + DynamoDB client lifecycle.** `DynamoDbClientHolder` closes the
  DynamoDB client in a CRaC `beforeCheckpoint` hook and rebuilds it in `afterRestore`,
  so every execution environment re-resolves credentials after restore rather than
  reusing whatever was valid at snapshot time. Repositories resolve the table from the
  holder on every call rather than caching it, since a cached reference would still
  point at the client the hook closed.
- **One fat jar** - built with the shade plugin; the config merges `META-INF/services`
  and Spring's `spring.factories` / `AutoConfiguration.imports` files so registrations
  from different dependencies aren't overwritten.

## Resolved design decisions

- **Note deletion is author-only** - `NoteService.delete()` checks `createdBy` against
  the caller's id; no `TENANT_ADMIN` override.
- **Admin invites are unrestricted** - any `TENANT_ADMIN` can invite another user as
  `TENANT_ADMIN`; no separate permission tier for granting admin access.

## Open design questions

- **Users in several tenants?** (A consultant belonging to multiple client tenants.)
  The single `custom:tenant_id` attribute and pool-wide groups assume one tenant per user.
- **ID token or access token** for API calls (see Auth decisions).
- **Signup abuse protection** for the public `/signup` route (rate limiting, email
  verification, captcha).
- **CORS** - no configuration exists yet, at either API Gateway or Spring Security.
  Needed before any browser-based frontend can call this API; blocked on knowing the
  frontend's origin(s).
- **Tenant deletion / data export** - the table is `RETAIN`; the offboarding process
  isn't designed yet.
- **Per-tenant throttling and plan enforcement** beyond `Plan.maxUsers`.