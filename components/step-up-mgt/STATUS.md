# Step-up authentication for self-service operations — status

Working notes for the step-up implementation. Written 2026-09-21.

Covers what exists, what was decided and why, what is known to be wrong, and what has not been
started. Everything here was verified against the code unless it says otherwise.

---

## 1. Goal

When a user changes a protected attribute — email address, username, password, the mobile number used
for MFA — they must complete an additional authentication factor first.

The customer's apps open a webview at a product-provided UI. The IDP session cannot be assumed present
in that webview; the app holds a long-lived refresh token gated behind a device biometric, and a valid
access token reflecting the original authentication.

Enforcement is by `acr` plus `auth_time` freshness. Failure is an RFC 9470 `401` challenge. The
requirement lives in tenant configuration, not in flow definitions, so that adding an attribute to the
protected set takes effect on every path at once.

---

## 2. Where the code is

| Repo | Branch | Commits |
|---|---|---|
| `carbon-identity-framework` | `feat/step-up-decision-component` | Phase 1 |
| `carbon-identity-framework` | `feat/step-up-flow` (off Phase 1) | Phase 3 + fixes |
| `carbon-identity-framework` | `dev/step-up-local-testing` (off Phase 3) | PoC tooling, not for merge |
| `identity-local-auth-emailotp` | `feat/secured-self-service-operations-otp` | 2 commits |
| `identity-apps` | `feat/secured-self-service-operations-flow` | 2 commits |

Nothing is pushed. `dev/step-up-local-testing` contains everything and is what the running pack is
built from.

### Phase 1 — decision component (`9df818c`)

`components/step-up-mgt/org.wso2.carbon.identity.stepup.mgt`, plus its feature.

- `StepUpDecisionService.evaluate(StepUpOperation, AuthenticationContextView) → List<FactorRequirement>`
- Tenant policy in one configuration-store resource (`STEP_UP_POLICY` / `step-up-policy`),
  index-keyed attributes, validated whole when read
- RFC 9470 challenge builder
- 40 tests

No dependency on OAuth or on the authentication framework. Its whole `Import-Package` is commons,
OSGi, `org.wso2.carbon.context` and `configuration.mgt.core`.

### Phase 3 — flow (`cfc5e3d` and later)

`components/step-up-mgt/org.wso2.carbon.identity.stepup.flow`, plus `FlowTypes.SECURED_SELF_SERVICE_OPERATIONS`
in `flow.mgt` and its arm in `FlowExecutionService.enterFlowInIdentityContext`.

- `StepUpDecisionExecutor` — evaluates once, at seed time, against seeded evidence only
- `SecuredOperationExecutor` — refuses if the decision never ran or a required factor was not
  performed; writes only the seeded attributes
- `SecuredOperationCompletionExecutor` — redirects only to a callback carried on the context
- Default flow definition shipped to `<carbon conf>/flow-defaults/SECURED_SELF_SERVICE_OPERATIONS.json`
- 26 tests

### PoC tooling — `dev/step-up-local-testing` only

`components/step-up-mgt/org.wso2.carbon.identity.stepup.flow.testing`, deliberately **not in any
feature** and refusing to run unless `-DStepUp.AllowUnverifiedSeed=true`.

- `AccessTokenSeedExecutor` — parses a JWT access token, verifies the signature against the tenant's
  OAuth key, checks expiry, resolves the subject, carries `acr`/`amr`/`auth_time` only if present
- `UnverifiedInputSeedExecutor` — earlier stand-in that seeds from a typed username
- `testing/step-up-test.html` — browser harness driving `POST /flow/execute` directly

---

## 3. Decisions settled, and why

**Factors, not ACR alone.** `AuthenticationContextView` exposes achieved factors as well as ACR.
In this product `AuthenticationContext.selectedAcr` is written from exactly one place —
adaptive-authentication JavaScript — so in a tenant without a script it is null everywhere, including
on the issued token. Matching on ACR alone would have made every `maxAge > 0` configuration
unreachable while still appearing to work, because fail-closed plus the customer's `maxAge: 0` default
produces the right behaviour for the wrong reason.

**One configuration resource, not one per rule.** A resource per rule would have to be read with
`getResourcesByType`, which `CachedBackedConfigurationDAO` does not cache and which costs `3 + 2N`
queries per evaluation. A single resource is one cached read, and validating the policy as a whole is
possible only when it is written as a whole.

**Evidence lives in the existing context property map, as primitives.** The flow context is persisted
as JSON, so a custom type placed in that map returns as a `LinkedHashMap` after a store round trip and
fails only on cache eviction or a node switch. The map is server-only: the flow execution API writes
request inputs into `userInputData`, never into `properties`.

**The seeding executor ships with the token it validates**, in `identity-oauth2-grant-token-exchange`,
not in the framework. Executors are OSGi services and need not live in the engine bundle. The
framework cannot depend on OAuth.

**Phase 2 deferred.** Phase 3 was built first at the customer's request, with an access token standing
in for the launch token.

---

## 4. Problems found

### Still open

**The flow context is not torn down on completion.** `SecuredOperationCompletionExecutor` returns
`EXTERNAL_REDIRECTION`, which the engine maps to `flowStatus: INCOMPLETE`, so the flow never reaches
its `END` node and `FlowExecutionService` never calls `removeFlowContextFromCache`. The context expires
by TTL instead. Returning `COMPLETE` would tear down correctly but hands the redirect to
`resolveCompletionRedirectionUrl`, which uses the application's registered access URL rather than a
per-launch callback with result state. Getting both needs the engine to honour an executor-supplied
URL on the `END` step.

**Stepping up with email OTP marks the email verified.** `EmailOTPExecutor` adds
`verifiedEmailAddresses` and `identity/emailVerified` as updated claims after a successful OTP, which
is right in a registration context where the OTP proves the address. Here it proves presence, and
promoting the address to verified is incidental. Harmless in the current shape; worth a deliberate
decision before it runs on an address the user has not otherwise verified.

**`ExecutorResponse(String result)` leaves its error object null.** `ExecutorResponse()` initialises
it; the single-argument constructor does not, so any executor that constructs with a status and then
calls `setErrorCode` gets an NPE. Worked around in this component by building error responses with the
no-argument constructor. Existing executors avoid it by accident. One line to fix in `flow.mgt`, not
done to keep the change out of this work.

**The Console cannot render this flow type.** `ServerFlowMgtService.resolveHandler` throws
`IllegalStateException` for any type without a meta handler, so `GET /flow/meta` returns 500;
`GET /flow/config` has an OpenAPI enum of `[REGISTRATION, PASSWORD_RECOVERY, ASK_PASSWORD]` and rejects
this type, though `PATCH` has no enum, which is why enabling works; and there is no generic flow
builder in `identity-apps` — the three that exist are per-type packages. The API half is small. The
Console half is not, and sizing it properly means deciding whether those three packages can be
generalised rather than a fourth copied.

**The attribute the form collects and the attribute the seed permits must agree, and nothing checks
they do.** They simply apply nothing when they differ. The superset-form design discussed for Phase 3
— one form covering every protected attribute, a declaration executor emitting the seeded subset as
`requiredParams`, and the renderer filtering on it — removes the possibility instead of logging it.
Not built.

**The PoC seed trusts its input.** An access token is not a launch token: not single use, not
restricted to the self-care application, carrying no operation scope, and carrying no `acr` or
`auth_time`. The callback URL is likewise taken on trust. All of this is what Phase 2 closes.

### Fixed during this work

| Problem | Where | Fix |
|---|---|---|
| `org.wso2.carbon.user.api` imported in the carbon kernel range; it is exported at `1.x` by its own bundle, so the bundles would not have resolved | `stepup.flow`, `stepup.flow.testing` poms | `${carbon.user.api.imp.pkg.version.range}` |
| `FlowConfigDTO.addAllFlowCompletionConfigs` NPEs on a null map; hidden until now because the management API's merge loop initialises the map as a side effect, and a flow type with no completion configs has nothing to merge | `flow.mgt` | null guard plus `FlowConfigDTOTest` |
| `EmailOTPExecutor` returned `(null, null)` for an unrecognised flow type — no OTP placeholder and no template, failing silently because a `HashMap` accepts a null key | `identity-local-auth-emailotp` | case added |
| The registration verification template told the user to verify their address to continue their registration | same | `EmailOTP`, the authenticator's own template; same `OTPCode` placeholder |
| Default flow definition used `config`, `name` and non-submit buttons | flow definition | `configs`, `identifier`, `"type": "submit"` — see below |
| `SecuredOperationExecutor` read the submitted value from `getUserInputData()` | `stepup.flow` | reads the flow user's updated claims |

**The component shape trap is worth repeating.** A system default flow file deserialises straight into
`ComponentDTO`, whose field is `configs`. The execution API renames it to `config` on the way out, so
every example you would naturally copy — the Console's `steps.json`, the flow management API, the test
resources in `flow.mgt` — uses the singular. The difference is one key and the failure is silent: the
page renders with no labels and no text. Two further keys matter: form state is keyed on
`config.identifier`, not `name`, and a button whose `config.type` is not `"submit"` posts an empty
input map. A `@JsonAlias("config")` on `ComponentDTO.configs` would make both shapes work.

**Inputs do not reach executors as inputs.** `FlowExecutionEngine.triggerNode` validates before every
node, and `InputValidationService.handleUserInputs` moves every claim-URI-keyed input onto the flow
user and removes it from `userInputData`. An executor must read `getFlowUser().getUpdatedClaimUris()`,
not the raw inputs. Note the distinction between `addClaim` and `addUpdatedClaim`: only the latter
marks a claim as changed, so claims merely loaded onto the flow — the address an OTP was sent to, for
instance — are not written back.

---

## 5. Discovery findings not yet acted on

From the Phase 2 spike, all verified against `identity-oauth2-grant-token-exchange` and
`identity-inbound-auth-oauth`:

- Token exchange **neither preserves nor recomputes** `acr` and `auth_time` — the words do not appear
  in the component at all. The failure mode the design most feared, stamping `auth_time = now`, does
  not exist and cannot appear by accident.
- `requested_token_type` accepts only `…:jwt` and `…:access_token`. The `audience` parameter is parsed
  and then ignored by the default `validateAudience`, which checks the subject token's audience
  instead. There is an extension seam.
- The **refresh grant preserves** `auth_time`, reading it from the previous token's authorization grant
  cache entry. But `auth_time` is emitted only when `max_age` was in the original authorization request
  or it was requested as an essential claim, so by default the id_token carries none.
- `JWTTokenIssuer` emits no `acr`, `amr` or `auth_time` on access tokens at all.
- There is **no atomic single-use mechanism** to reuse. The authorization code's enforcement is
  read-then-write: the state check runs against a separately read row, and the deactivation SQL has no
  `AND STATE='ACTIVE'` guard and discards the affected row count.
- The **impersonation subject token** is a close structural precedent — per-app enablement, per-app
  expiry defaulting to 180 seconds, designed to be handed into the token exchange grant. It is not
  single use and carries no `acr`/`auth_time`.
- `requestedTokenType` is an instance field on a shared grant handler, assigned per request without
  reset, and echoed into the `issued_token_type` response parameter. Pre-existing concurrency bug.

Also: `AuthHistory` and `AuthenticatorConfig` carry **no timestamps**, and `SessionContext` holds a
single `CreatedTimestamp`/`UpdatedTimestamp` pair that is overwritten on each subsequent
authentication. The first factor's age is genuinely unrecoverable after a step-up, which is why the
decision is taken at seed time.

---

## 6. Not started

- **Phase 2** — launch token and the real seeding executor
- **Phase 4** — hosted UI beyond what the accounts portal already renders
- **Phase 5** — REST enforcement in SCIM2, and `acr`/`auth_time` on access tokens and introspection.
  Until this exists, protected attributes remain freely updatable over `/scim2/Me` while the flow
  prompts for an OTP. That asymmetry is expected at this stage and is the open scoping question in the
  brief.
- **Phase 6** — management API and Console
- **Phase 7** — integration tests in `product-is`

---

## 7. Running it

See the PoC harness and the flow definition under
`org.wso2.carbon.identity.stepup.flow.testing/testing/`.

Deploy: the two step-up bundles, the two changed `flow-orchestration-framework` bundles, the testing
bundle, and `org.wso2.carbon.identity.local.auth.emailotp`. Copy the flow definition to
`<IS_HOME>/repository/conf/flow-defaults/` and `execution-flow.jsp` to the deployed `accounts` webapp.
Start with `-DStepUp.AllowUnverifiedSeed=true`.

Enable the flow, remembering that `PATCH /flow/config` needs `"flowCompletionConfigs":{}` until the
`FlowConfigDTO` fix is deployed. Create the `STEP_UP_POLICY` resource type before the resource;
without a policy the decision returns an empty list and nothing is ever outstanding, so the refusal
path is not being exercised.

```
https://localhost:9443/accounts/execution-flow.jsp
  ?flowType=SECURED_SELF_SERVICE_OPERATIONS&spId=<appId>
  &launch_token=<jwt>&callback_url=<where to land>
```

The step-up prompt is the first screen. The OTP step needs two submits: the first with the field empty
sends the code.
