# Step-up authentication for self-service operations — status

Working notes for the step-up implementation. Written 2026-09-21, revised 2026-09-22.

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
| `identity-apps` | `feat/secured-self-service-operations-flow` | 4 commits, incl. the Console builder |
| `identity-api-server` | `feat/secured-self-service-operations-flow-meta` | Phase 6, API half |

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

- `OperationSelectionExecutor` — one per branch; fixes which operation the flow performs and refuses
  a branch that disagrees with what the flow was seeded for
- `OperationBranchExecutor` — answers a later fan-out from the branch already chosen, so the user is
  asked once
- `StepUpDecisionExecutor` — evaluates once, after the operation is known, against seeded evidence only
- `SecuredOperationExecutor` — refuses if the decision never ran or a required factor was not
  performed; writes only the seeded attributes
- `SecuredOperationCompletionExecutor` — redirects only to a callback carried on the context
- Default flow definition shipped to `<carbon conf>/flow-defaults/SECURED_SELF_SERVICE_OPERATIONS.json`
- 34 tests

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

**Several protected attributes are handled by branching, not by one form that hides fields.** A VIEW
step with more than one action becomes a decision node implicitly (`GraphBuilder.handleTempNodesInStep`),
with one edge per button and `triggeringActionId` set to that button's component id. So a choice page
and a branch per attribute need no engine change. The alternative considered — one superset form, with
the executor naming the seeded subset in `requiredParams` and the renderer filtering on it — has no
mechanism behind it; see the correction under *Problems found*.

The shape is: seed → router → choice → per-branch selection → decision → step-up → router → collection
branch → completion. It fans out twice because the decision and the OTP are shared and sit between the
two fan-outs. Only the first fan-out is ever a question for the user; `UserChoiceDecisionNode` completes
without rendering when an action has already been chosen, so `OperationBranchExecutor` answers the
second from what the first recorded, and answers the first too when the launching request named a
branch.

**The launch token does not name the operation. The request does.** The argument that settled it: today
any application holding an access token can already `PATCH /scim2/Me` and change any attribute the user
may change, with no configuration on the IS side. Scoping the operation into the token would protect
something that is not protected now. Phase 5 does not change that either — once SCIM2 starts refusing,
the launch token becomes the only route, but it is still the same user changing their own attributes,
and they may legitimately ask for any operation. The gate is the step-up, not the scope.

Tampering with the parameter yields nothing: the subject is bound by the token, so a changed operation
only ever reaches a different page for the caller's own profile, which they could have asked for
directly. It is a routing parameter.

What the request names is the **branch**, not the attribute. Which attribute a branch updates is
written in the flow definition, so a caller naming a branch cannot name an attribute the flow does not
offer.

This leaves the launch token carrying four things and nothing else: `sub`, `client_id` for the callback
derivation, `acr`/`amr`/`auth_time` for the decision, and single use with a short expiry. It removes the
per-application operation allowlist, the scope-versus-claim question, and all exchange-time operation
validation from Phase 2.

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

**The Console cannot render this flow type.** The API half of this is now done; the Console half is
not.

Two corrections to what was written here before. The OpenAPI enums are `@ApiParam(allowableValues=…)`
on a plain `String` parameter — Swagger documentation, not runtime validation — so they never rejected
anything. The 500 came from one place only: `ServerFlowMgtService.resolveHandler` throwing
`IllegalStateException` for a type with no meta handler. And the flow's own executors did not need
listing anywhere: `Utils.getSupportedExtensionExecutors(flowType)` resolves them from the engine by
`Executor.getSupportedFlowTypes()`, which all five already declare.

A fourth flow builder package now exists: `admin.secured-self-service-flow-builder.v1`, 18 files and
about 1,000 lines against the 53-57 files and 5-6k lines of the three before it. Almost all of that
difference is things this flow does not need. It contributes no element of its own, so it uses
`CommonElementFactory`, the core `ResourceProperties` and the core palette rather than wrapping them;
and it ships no AI generation, no migrations and no resource JSON. What it does carry is the edge
building, the transform on save, the provider and the page.

It has been syntax checked but not compiled. The workspace lockfile here was written by pnpm 10.33.0
and the installed pnpm is 11.13.1, which wants to purge `node_modules` before it will install, so no
package in this checkout currently has its dependencies linked and there is no type-check to run.

**Branching survives the Console, which was the risk worth checking first.** All three existing
builders are linear and this flow is a 13-step graph with two fan-outs, so the concern was that the
canvas would flatten it. It does not. Four places all turn out to be branch-agnostic:

- **Load.** `createEdgesForButtons` is called for every button on a step, so a step with several
  buttons gets several outgoing edges.
- **Save.** `transformFlow` keys its navigation map by `sourceHandle`, which is a *resource* id, and
  applies it to components recursively — not once per step.
- **Wire format.** Executor configuration round-trips as arbitrary string key/values, renamed
  `metadata` → `meta` on the way out and back in `Utils.convertToExecutorDTO`. Unknown fields on
  `step.data` are spread through untouched.
- **Server validation.** `validateNodeConnectivity` only requires a non-blank `next` on each action.
  It assumes nothing about arity.

Replaying the load and save transformations over the shipped definition gives 14 edges across 13
steps, both fan-outs (`operation_choice`, `collection_choice`) and both convergence points
(`step_up_decision`, `operation_completion`) intact, every navigation preserved, and the branch
metadata on all four routing nodes preserved. That is a replay of the logic rather than the code
itself, so it is strong evidence and not a passing test.

Two further things that make this cheaper than it looked. `validateFlowType` checks against
`Constants.FlowTypes.values()`, so there is no separate list of types to keep in step — the Swagger
`allowableValues` are documentation and enforce nothing. And `FlowMgtService.getFlow` falls back to
`SystemDefaultFlowProvider`, so `GET /flow` returns the shipped definition for a tenant that has never
saved one: the Console has something real to render from the first load.

What remains is the package itself, and almost none of its bulk is about branching — it is AI
generation, extended property panels, migrations, and the `steps.json`/`elements.json`/`widgets.json`
palette. A lean fourth package can drop the first three. The one real gap is that executor metadata
round-trips but has no editor, so the Console could rearrange this flow but not author a new branch
until a small property panel is added for it, in the way `federation-properties.tsx` edits
`meta.idp`.

**`requiredParams` filters nothing, and an earlier note here said it did.** The superset-form design
recorded for Phase 4 — one form covering every protected attribute, an executor naming the seeded
subset in `requiredParams`, the renderer filtering on it — assumed a client-side filter that does not
exist. The only use of `requiredParams` anywhere in `identity-apps` is `execution-flow.jsp:378`, which
supplies `origin` for an `INTERNAL_PROMPT`. Building that filter would be new work in the React
renderer. Branching, which needs no engine or client change, is what the flow now does instead.

**The form's attribute and the seeded attribute must still agree, and only a log says when they do
not.** `SecuredOperationExecutor` applies nothing when they differ, and because applying nothing is
also what the first pass through that node looks like, the page simply reappears on every submit. The
symptom looks nothing like the cause, so the executor now warns with both lists named.

It stays a warning rather than a refusal because the inputs it can see are `context.getCurrentStepInputs()`,
which holds what the engine last published for a step — on the first pass through the node, still the
previous step's. Making it a refusal needs a reliable "what was submitted in this request" signal,
which the engine does not currently expose. With branching in place the pairing is also checkable
statically: one branch per protected attribute, and each branch's collection form must carry that
branch's claim. That belongs with the flow builder in Phase 6.

**Nothing appears to bind a submitted `actionId` to the step that was just rendered.** It travels in
the request body (`FlowExecutionService:134`) into the decision node's edge match, and `InputValidator:78-83`
tolerates an action with no declared inputs by debug-logging and continuing. So a caller driving
`/flow/execute` directly can plausibly reach a branch other than the one they were offered. This was
not confirmed either way and should be.

The design does not rest on the answer. Two checks make a forged branch harmless rather than merely
unlikely: `OperationSelectionExecutor` refuses a branch that disagrees with the seeded operation, and
`SecuredOperationExecutor` writes only seeded attributes. Without the first, a user could satisfy the
factor required for a weakly protected attribute and then steer into the branch of one that requires
a stronger factor — the decision and the write are on opposite sides of the second fan-out, and only
the recorded operation ties them together.

**The editor shows two pages that ask which attribute to change, and only one of them is ever seen.**
The flow fans out twice because the decision and the OTP are shared and sit between the two fan-outs.
The second fan-out is reached with the branch already chosen, so `OperationBranchExecutor` answers it
and the page never renders; it exists as a page only because the engine derives a decision node from a
step with several actions. On the canvas both are ordinary steps, and giving them the same wording
made the flow look as though it asks twice, so the second now reads "Continue to update your details".

The alternative is to put the decision and the OTP inside each branch, which leaves one choice page and
no routing step, at the cost of duplicating those two per attribute. At two attributes it is slightly
smaller and reads better; at four it is twelve extra nodes. Worth revisiting if the editor view matters
more than the node count.

**The callback must be derived from the application that started the flow, not supplied with it.**
Today the callback arrives as a `callback_url` query parameter, is carried into the flow as
`stepUp.validatedCallbackUrl`, and is redirected to unchanged. `SecuredOperationCompletionExecutor`
refuses when the property is absent, but it cannot tell a value the server established from one a
caller invented, so the name of that property currently overstates what has happened to it.

The requirement is that the flow resolves the callback from the launching application itself — its
registered redirect URIs — and accepts a requested value only when it matches one of them. A redirect
out of a context that has just completed an authentication, to a location the caller chose, is an open
redirect, and it is worth more here than usual because the ceremony that precedes it is what convinces
the user the destination is trustworthy.

This belongs in the seeding step, which is also where the launching application becomes known: the
launch token carries `client_id` for exactly this reason. That executor ships in
`identity-oauth2-grant-token-exchange`, which can see the OAuth application configuration and so can
resolve the registered URIs; the framework cannot. The engine's own completion path already does
something of this shape — `FlowExecutionEngineUtils.resolveCompletionRedirectionUrl` uses the
application's registered access URL — but it takes no per-launch value, so it is a precedent rather
than a mechanism to reuse.

The `callback_url` parameter and the seed executor's default are PoC scaffolding and must not survive
into the real seeding step.

**The PoC seed trusts its input.** An access token is not a launch token: not single use, not
restricted to the self-care application, carrying no operation scope, and carrying no `acr` or
`auth_time`. All of this is what Phase 2 closes.

### Fixed during this work

| Problem | Where | Fix |
|---|---|---|
| `org.wso2.carbon.user.api` imported in the carbon kernel range; it is exported at `1.x` by its own bundle, so the bundles would not have resolved | `stepup.flow`, `stepup.flow.testing` poms | `${carbon.user.api.imp.pkg.version.range}` |
| `FlowConfigDTO.addAllFlowCompletionConfigs` NPEs on a null map; hidden until now because the management API's merge loop initialises the map as a side effect, and a flow type with no completion configs has nothing to merge | `flow.mgt` | null guard plus `FlowConfigDTOTest` |
| `EmailOTPExecutor` returned `(null, null)` for an unrecognised flow type — no OTP placeholder and no template, failing silently because a `HashMap` accepts a null key | `identity-local-auth-emailotp` | case added |
| The registration verification template told the user to verify their address to continue their registration | same | `EmailOTP`, the authenticator's own template; same `OTPCode` placeholder |
| Default flow definition used `config`, `name` and non-submit buttons | flow definition | `configs`, `identifier`, `"type": "submit"` — see below |
| `SecuredOperationExecutor` read the submitted value from `getUserInputData()` | `stepup.flow` | reads the flow user's updated claims |
| The seed defaulted the operation to the mobile claim, so the choice page could never be reached and any other branch conflicted with it | `stepup.flow.testing` | the seed records the branch the request named, and records nothing when it named none |

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
- `TokenExchangeGrantHandler.filterRequestedScopes` (lines 392-402) intersects the requested scopes
  with the subject token's approved scopes, but `handleRequestedScopes` (421-427) applies that only
  when the request carries subject *and* actor tokens or `TokenExchange.LimitScopesToSubjectToken` is
  enabled. Otherwise the `else` branch assigns the requested scopes unfiltered.

  This was briefly the proposed gate for scoping the launch token, and it cannot be: an application's
  access token will never carry a self-service operation scope, so the intersection is empty. Worse,
  the failure is silent — `setScope` is handed an empty array and a launch token is issued carrying
  nothing, with no error. The operation is now taken from the request instead, so none of this is on
  the path any more. Recorded because the silent-empty-scope behaviour is a trap for anything else
  that reaches for this mechanism.

Also: `AuthHistory` and `AuthenticatorConfig` carry **no timestamps**, and `SessionContext` holds a
single `CreatedTimestamp`/`UpdatedTimestamp` pair that is overwritten on each subsequent
authentication. The first factor's age is genuinely unrecoverable after a step-up, which is why the
decision is taken at seed time.

---

## 6. Not started

- **Phase 2** — launch token and the real seeding executor. Reduced by the decision above to: a token
  type carrying `sub`, `client_id`, `acr`/`amr`/`auth_time` and single use with a short expiry; the
  callback resolved from the launching application's registered redirect URIs; and the executor itself.
  The two open questions are where `acr`/`auth_time` come from, since the subject token carries none
  and only the authorization grant cache entry has them, and how single use is enforced, since nothing
  existing is safe to copy. Blocked first on `identity-oauth2-grant-token-exchange` pinning framework
  `7.8.675` against the current `7.11.205-SNAPSHOT`: the executor SPI does not exist in that version.
- **Phase 4** — hosted UI beyond what the accounts portal already renders. Smaller than it was: the
  multi-attribute problem it was meant to solve is solved by branching, and the superset form it
  proposed rested on a filter that does not exist.
- **Phase 5** — REST enforcement in SCIM2, and `acr`/`auth_time` on access tokens and introspection.
  Until this exists, protected attributes remain freely updatable over `/scim2/Me` while the flow
  prompts for an OTP. That asymmetry is expected at this stage and is the open scoping question in the
  brief.
- **Phase 6** — mostly done. The Console lists the flow and opens it in a builder; what is left is an
  editor for executor configuration, without which a branch can be rearranged but not authored, and a
  compile of the new package. The API half: a `SecuredSelfServiceOperationsFlowMetaHandler`, its
  case in `resolveHandler`, and the flow type added to every `flowType` enum in `flow.yaml` and the
  checked-in `FlowApi`. That branch also bumps `carbon.identity.framework.version` from `7.11.195` to
  `7.11.205-SNAPSHOT`, because `Constants.FlowTypes.SECURED_SELF_SERVICE_OPERATIONS` does not exist in
  the released version — upstream this lands after a framework release carrying the enum.
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
  &operation=mobile
```

`operation` names a branch of the flow — `mobile` or `email` in the shipped definition. Leave it off
and the flow opens on a page asking which. Either way the OTP step needs two submits: the first with
the field empty sends the code.

Both the choice page and the collection fan-out are ordinary multi-button VIEW steps. Adding a third
protected attribute means a button on each, an `OperationSelectionExecutor` node declaring the branch
and its claim, a `branch.<name>` entry in both `OperationBranchExecutor` nodes, and a collection page
whose field identifier is that claim.
