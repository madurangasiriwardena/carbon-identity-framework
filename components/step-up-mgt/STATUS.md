# Step-up authentication for self-service operations — status

Working notes for the step-up implementation. Written 2026-09-21, revised 2026-09-25.

**The flow runs end to end on a local pack.** A launch carrying an access token authenticates the user
with an email OTP, collects the new value, writes it through the user store, and returns to the
application. The Console lists the flow and opens it in a builder. What remains is under *Still open*
and *Not started* — none of it blocks exercising the flow.

**Phase 2 is designed and not started.** The launch token becomes a session transfer token issued by
RFC 8693 token exchange. The reasoning is in section 3, the findings behind it in section 5, and the
plan and its open decisions in section 6.

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
| `carbon-identity-framework` | `feat/step-up-flow` (off Phase 1) | Phase 3 as of 2026-09-21 |
| `carbon-identity-framework` | `dev/step-up-local-testing` (off Phase 3) | PoC tooling, and everything since 2026-09-21 |
| `identity-local-auth-emailotp` | `feat/secured-self-service-operations-otp` | the OTP flow type case |
| `identity-apps` | `feat/secured-self-service-operations-flow` | the accounts page and the Console builder |
| `identity-api-server` | `feat/secured-self-service-operations-flow-meta` | flow meta for this type |

`dev/step-up-local-testing` contains everything and is what the running pack is built from. It and the
three branches in the other repos are pushed to `madurangasiriwardena` forks. The two feature branches in
this repo are not pushed.

`feat/step-up-flow` is behind. 18 later commits on the dev branch change product code, such as the
branching, the claim-derived routing and the END node, and none of them are on the feature branch. Some
of those commits also change the PoC tooling, so they must be split before the feature branch can be
brought up to date.

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

- `OperationBranchExecutor` — answers the operation choice when the launching request already named it
- `SecuredOperationExecutor` — writes only the attribute its own node declares, and refuses unless the
  user was authenticated in this flow
- `StepUpDecisionExecutor` — retained but no longer in the default flow; see the decision below
- Default flow definition shipped to `<carbon conf>/flow-defaults/SECURED_SELF_SERVICE_OPERATIONS.json`
- 30 tests

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

**Phase 3 was built before Phase 2.** The customer asked for it first. An access token stands in for
the launch token until Phase 2 exists.

**The launch token is a session transfer token, issued by token exchange.** Decided 2026-09-24. The
design follows Auth0's Native to Web SSO, adapted in the ways below.

*Why a token is needed at all.* The webview cannot be assumed to share the IDP session, so something
must carry the user from the app into the browser. The app's access token should not be that thing. It
would travel in a URL, it can be used many times, and it lives for an hour.

*Why not a format of our own.* Auth0's session transfer token already has the properties we need. It is
opaque, it lasts 60 seconds, it can be used once, and the app gets it from the ordinary token endpoint.
So Phase 2 issues it through RFC 8693 token exchange instead of a new endpoint.

*Why not OIDC Native SSO.* It moves a login from one native app to another, using an `id_token` and a
`device_secret`. The spec states that it does not cover a browser session, and the webview never holds
the `device_secret`. Two of its ideas are kept: a derived token ends with the session it came from, and
it does not count as a new authentication.

*Why the access token is the subject token, not the refresh token.* Auth0 uses the refresh token. With
refresh token rotation that would use up the app's refresh token. The app would have to take the new one
from our response, and a refresh running at the same time could trigger reuse detection and revoke the
whole token family.

*Why the token is opaque.* Only the server that issues it ever reads it. Single use needs a server row
anyway, so a random handle that points at the row is simpler than a signed JWT. It also keeps the claims
out of the URL.

*What is not copied from Auth0.* Auth0's token creates a web SSO session at `/authorize`. Ours opens one
run of this flow and nothing else, and creates no session cookie. Its `auth_time` is the original
sign-in time, never the time the transfer token was issued.

*The cost of using the access token.* The app sends its access token to every API it calls, and any of
those APIs could exchange it and open the flow as the user. Usually that only reaches the OTP, which
still goes to the user. The exposure is the skip condition. If a recent factor lets the flow skip the
OTP, an access token alone would be enough to change a protected attribute. The proposed control is
decision 1 under Phase 2 in section 6.

**Several protected attributes are handled by branching, not by one form that hides fields.** A VIEW
step with more than one action becomes a decision node implicitly (`GraphBuilder.handleTempNodesInStep`),
with one edge per button and `triggeringActionId` set to that button's component id. So a choice page
and a branch per attribute need no engine change. The alternative considered — one superset form, with
the executor naming the seeded subset in `requiredParams` and the renderer filtering on it — has no
mechanism behind it; see the correction under *Problems found*.

**The authentication is part of the flow's shape, not a decision taken before it.** A factor is placed
above the fan-out when every branch requires it, and inside a branch when only that branch does. For the
current requirement — one email OTP for every protected attribute — everything hoists and the flow is
seven steps:

```
seed → step_up_otp → operation_router → operation_choice ─┬─ mobile_collection ─┬→ END
                                                          └─ email_collection ──┘
```

One fan-out, no duplicated authentication, nothing shown twice. The router must sit immediately before
the choice: the action it sets does not survive a page, because `FlowExecutionService` assigns
`currentActionId` from the request at the start of every request.

Sharing a factor across branches is safe exactly when every branch requires it — there is then nothing
to borrow. The moment one attribute needs something stronger, that factor moves into its branch, which
is adding a node to a branch that already exists rather than reshaping the flow.

**The flow is asked for a claim, and answers from its own shape.** Nothing about the branching is
configured. `SecuredOperationExecutor` takes the attributes it may write from the fields on its own
page — a field's `identifier` is the claim it writes, which is what an author chooses when they place
the field — and `OperationBranchExecutor` resolves the action for a requested claim by walking the
graph: `GraphConfig.getNodePageMappings()` carries the page of every step, so it can find which branch
collects the claim and which action reaches it.

Two earlier shapes were discarded on the way. A seeded operation carried on the flow context, with an
executor per branch to record it; and then executor metadata, `operationAttributes` on the collection
step and `branch.<name>` on the router. Both failed the same test: they could not be authored in the
Console. There is no editor for arbitrary executor metadata, and the router's value had to equal a
button's component identifier, which the editor generates. Adding an attribute would have meant copying
a generated id from one node into another by hand.

Deriving it instead means a third attribute is purely an editor operation — add a page with the right
field, add a button, draw the edge — and the request names the claim directly
(`operation=http://wso2.org/claims/mobile`) rather than an invented word. The trade is that the form
becomes the declaration, so adding a field to a collection page widens what that page may write. Since
the page and the executor are authored together, that is the honest place for it to live.

**The request names the branch, not the attribute, and not the token.** Today any application holding an
access token can already `PATCH /scim2/Me` and change any attribute the user may change, with no
configuration on the IS side. Scoping the operation into the launch token would protect something that is
not protected now, and Phase 5 does not change that: once SCIM2 starts refusing, the launch token becomes
the only route, but it is still the same user changing their own attributes and they may legitimately ask
for any operation. The gate is the step-up, not the scope.

Tampering with the parameter yields nothing: the subject is bound by the token, so a changed operation
only ever reaches a different page for the caller's own profile. Which attribute a branch updates is
written in the flow definition, so a caller naming a branch cannot name an attribute the flow does not
offer.

This leaves the launch token carrying a short list: `sub`; `client_id`, which names the launching
application; the session it came from; `acr`/`amr`/`auth_time` for the skip conditions; and single use
with a short expiry. Section 6 has what is stored and where each value comes from.

**The flow ends at its END node, and the destination comes from the application.** There is no completion
executor. The engine's own END handling sets `flowStatus` to `COMPLETE`, `stepType` to `REDIRECTION`, and
the URL from `FlowExecutionEngineUtils.resolveCompletionRedirectionUrl` — the launching application's
registered access URL, falling back to My Account.

The executor that used to sit there existed for one reason: to redirect to a callback the launching
request supplied. It cost two things. `EXTERNAL_REDIRECTION` maps to `STATUS_INCOMPLETE`
(`TaskExecutionNode:173`), so the flow never reached END, `flowStatus` was never `COMPLETE`, and
`FlowExecutionService` never called `removeFlowContextFromCache` — the context leaked until its TTL. And
the callback arrived in the request, which made validating it against the application's registered URIs
a piece of outstanding work.

Taking the destination from application configuration settles both: the context is torn down, and the
URL can no longer be chosen by a caller, so there is nothing left to validate. What is lost is a
per-launch destination — every launch now ends at the same place, with no result state.

Restoring that later should not restore the executor. The engine's END step resolves the URL itself and
never consults the context, so the right change is to let a completion URL set on the context take
precedence there. The seeding step could then establish the callback at the same moment, and by the same
code, that validates it against the application's registered URIs — and the flow would still end at END.

**The flow reads no tenant configuration at runtime.** The operation comes from the launch request, the
attribute from the node, the factor from the step. What remains outside the flow is one thing: which
attributes SCIM2 must refuse.

```
STEP_UP_POLICY / step-up-policy
  protectedAttributes = http://wso2.org/claims/mobile,http://wso2.org/claims/emailaddress
```

The flow does contain that list — the claim identifiers on its collection pages — but deriving enforcement
from it fails open three ways: deleting a branch on a canvas silently unprotects an attribute, a tenant
that never configured the flow has no protection, and SCIM2 grows a dependency on flow management.

**Enforcement is a refusal, not a challenge.** SCIM2 blocks a protected attribute on the self-service
path and the attribute can then be changed only through the flow. There is no RFC 9470 challenge carrying
`acr_values` and `max_age`, because the client's remedy here is to launch the flow rather than to obtain a
better token — it cannot obtain an arbitrary `acr` on its own. This is a deliberate departure from the
brief, taken because it is what keeps the configuration above to a single list.

Two things follow. The block belongs on `/scim2/Me`, not on `/scim2/Users/{id}`: refusing administrative
updates would break user management. And My Account edits the profile through `/scim2/Me`, so its profile
form will start failing for protected attributes until it is taught to launch the flow instead — that is
Phase 5 scope, not a detail.

`SecuredOperationExecutor` writes through `AbstractUserStoreManager.setUserClaimValuesWithID`, not through
SCIM2, so a block in the SCIM2 layer needs no exemption for the flow.

---

## 4. Problems found

### Still open

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

The design does not rest on the answer. The flow has one fan-out, and every edge out of it starts at the
beginning of a branch. So a forged action can only select another branch from its start, and that
branch then runs its own factors. `SecuredOperationExecutor` also writes only the fields on its own page.
A forged action therefore gains nothing that a different `operation` parameter would not, and the caller
may choose any branch anyway. This holds only while no step inside a branch offers more than one action.

**Most of Phase 1 is now unused, and the reduction has not been made.** With the authentication
expressed as the flow's shape and the enforcement reduced to a refusal, the decision component's
`evaluate` path has no caller. `AuthenticationContextView`, `FactorRequirement`, `RequiredFactor`,
`StepUpRule`, `StepUpChallengeBuilder` and the whole of `StepUpDecisionServiceImpl.evaluate` — the
dedupe-by-strictest-maxAge logic and the freshness checks — go with it. What survives is `StepUpPolicy`
as a list of protected attributes, its provider, the exceptions, and a service answering whether an
attribute is protected.

`StepUpDecisionExecutor` is likewise no longer in the default flow. It is kept for now, and
`SecuredOperationExecutor` still honours a decision verdict when one has been recorded, so a flow that
places it explicitly still behaves correctly. Removing it cascades into the bundle's service reference
and its dependency on `stepup.mgt`, which is why it was left for the Phase 1 reduction rather than done
here.

The work was not wasted — it is what established that ACR is script-only in this product, which is why
the flow carries inherited evidence explicitly — but the code mostly goes.

**Skipping a factor needs an engine feature that does not exist yet.** The flow always prompts: someone
who authenticated ten seconds ago still gets an OTP. Correct and fail-closed, but not yet the point of
the feature.

`Constants.StepTypes` is `VIEW, REDIRECTION, INTERNAL_PROMPT, EXECUTION, WEBAUTHN, USER_ONBOARD, END`,
and `GraphBuilder` throws on anything else, so there is no conditional or skippable step. This is
already scoped as **wso2-enterprise/iam-product-management#634, "Add conditional branching support to
flow engine"** — open, assigned, a decision node with a condition against runtime data, with designer
support. When it lands, the skip is one conditional decision in front of `step_up_otp`, which is why
the seed keeps recording `stepUp.inheritedAcr`, `inheritedFactors` and `inheritedAuthTime` although
nothing reads them yet.

The requirement worth raising on that issue: its example condition is a business one (`order > $1,000`),
whereas this needs to read evidence the seeding step put on the flow context. What data a condition can
reach decides whether step-up can use the feature at all.

Unrelated find, worth reporting separately: the Console declares `StepTypes.Rule = "RULE"` and ships a
`resources/steps/rule` component, which the server would reject on save.

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
| Every execution step in the editor read "Execution": `ExecutionFactory` names a contributed executor only when the flow metadata describes it, and none of these described itself | `stepup.flow`, `stepup.flow.testing` | `getExecutorMetadata` on each |
| `AccessTokenSeedExecutor` declared no supported flow types, unlike its sibling in the same bundle. The default is none, which keeps an executor out of the metadata entirely — so the step could not be named, and a flow using it would have been refused on save | `stepup.flow.testing` | flow type declared |
| The composer logged a failure to read metadata from every executor and fell back to derived defaults: `FlowExecutorMetadata` lives in the engine's metadata package, which neither bundle imported, so the class threw where the engine called for it. The engine catches `LinkageError` there, which is why it degraded instead of failing to start | both poms | package imported |
| Clicking the gear on a step hung the page. The shared panel destructures `ResourceProperties` from the builder context and renders that, shadowing its own name; the package supplied the shared panel as that context value, so it rendered itself without end | `admin.secured-self-service-flow-builder.v1` | its own panel, delegating to the shared per-kind factories |
| The flow definition carried no coordinates, so every step deserialised at the origin and the editor drew all of them on top of each other | flow definition | `coordinateX`/`Y`, `width`, `height` per step |
| The canvas regenerated every node, walked every component and re-laid the edges on each render, and again on every window focus | `admin.secured-self-service-flow-builder.v1` | resources, node types and handlers memoised; the rebuild keyed on the flow rather than on `isValidating` |
| `OperationBranchExecutorTest` was never listed in `testng.xml`, so the branch executor was untested | `stepup.flow` | registered |

**The Console renders and edits this flow.** The API half needed a meta handler and a case in
`ServerFlowMgtService.resolveHandler`, which threw `IllegalStateException` for a type without one — the
sole cause of the 500 from `GET /flow/meta`. Two things turned out not to be needed: the OpenAPI enums
are `@ApiParam(allowableValues=…)` on a plain `String`, so Swagger documentation that never rejected
anything; and the flow's executors needed listing nowhere, because
`Utils.getSupportedExtensionExecutors(flowType)` resolves them from the engine by
`Executor.getSupportedFlowTypes()`.

The Console half is `admin.secured-self-service-flow-builder.v1` — 18 files, about 1,000 lines, against
the 53-57 files and 5-6k lines of the three builders before it. It contributes no element of its own, so
it uses the shared element factory and the core palette, and ships no AI generation, no migrations and
no resource JSON. Only the property panel had to be its own, and the reason is worth keeping: the shared
panel renders whatever component the builder context supplies under that name, so supplying the shared
panel itself makes it render itself.

Branching survives the round trip, which was the risk worth checking before writing any of it. Four
places are branch-agnostic: `createEdgesForButtons` runs for every button on a step; `transformFlow`
keys its navigation map by resource id and applies it recursively; executor configuration round-trips
as arbitrary string key/values; and `validateNodeConnectivity` only requires a non-blank `next`.
Replaying the load and save transformations over the shipped definition preserved every navigation,
both fan-outs and the branch metadata — and the flow has since been opened and edited in the running
Console.

`FlowMgtService.getFlow` falls back to `SystemDefaultFlowProvider`, so the canvas has the shipped
definition to render before anyone saves. Note what saving means: `deleteFlow` returns silently in a
root organization, so a flow saved from the editor in super tenant cannot be removed through the API —
only by clearing the `IDN_FLOW` row, whose children all cascade.

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

## 5. Discovery findings

These back the Phase 2 plan in section 6. The first investigation ran on 2026-09-21. The second ran on
2026-09-24 against `identity-oauth2-grant-token-exchange` at `d6c22d9` (1.4.9-SNAPSHOT) and
`identity-inbound-auth-oauth` at `21e9c5d9f2` (7.5.89-SNAPSHOT, master as of 2026-09-22). All of it was
read from source. None of it was run.

### Token exchange connector

`H` below is `TokenExchangeGrantHandler.java`.

- The connector pins framework `7.8.675` and OAuth `7.5.70`. Its import ranges already admit framework
  7.11.x, and every framework API it calls still exists in `7.11.205-SNAPSHOT`. Moving the pin looks
  safe. It has not been built.
- `subject_token_type=…:access_token` is accepted only when the token is a JWT (H:180-182). **Opaque
  access tokens are refused.** The connector never calls the OAuth token validation service.
- A JWT access token issued by this server is accepted. The signature is checked with the tenant
  certificate. The stored token is then loaded by `jti`, which fails for a token that is not ACTIVE, so
  that lookup also acts as a revocation check.
- **Outside impersonation, the subject token's client is not compared with the requesting client.**
  `client_id` is read only to find the tenant (H:1080-1092). Any client can exchange another client's
  token.
- `requested_token_type` is checked by a private method that allows only `…:jwt` and `…:access_token`
  (H:1151-1158). The issued token is always a standard access token from `super.issue()` (H:858). There
  is no other kind of output. A new token type means overriding `validateGrant` and `issue`, because the
  type check and the subject token handling are both private.
- Two handlers cannot share one grant type. `OAuthServerConfiguration.getSupportedGrantTypes()` keeps
  one instance per grant name in a map (`OAuthServerConfiguration.java:1332-1364`). The new token type
  has to go into this handler or a subclass of it.
- Public clients can use the grant by default. `PublicClientAllowed` defaults to true in
  `identity.xml.j2`.
- Token exchange **neither preserves nor recomputes** `acr`, `amr`, `auth_time` or the session id. None
  of them appear in the component. The failure the design most feared, stamping `auth_time = now`,
  cannot happen by accident. It also means the connector gives us none of them.
- Correction to an earlier note here: the `audience` parameter is not simply ignored. `validateAudience`
  ignores it, but `resolveAudiences` (H:787-814) uses it to set the issued token's `aud`.

**Shared state on the handler. This may be a security issue.** `requestedTokenType` and `impersonator`
are instance fields (H:110-111). The server holds one handler instance for all tenants, so every request
in flight shares them.

- `requestedTokenType` is written before it is validated, and it is never reset (H:147, H:178). A
  request with an invalid value fails but leaves that value behind. Every later request that omits the
  parameter then fails, in every tenant, until a valid value is sent or the server restarts. A known
  public `client_id` is enough to trigger it.
- `impersonator` is written in `validateSubjectToken` (H:336) and compared in `validateActorToken`
  (H:474). A concurrent request can overwrite it between the two, so the `may_act` check can pass on
  another request's value.

This is being analysed in a separate session. Fixing it is a prerequisite for Phase 2, because the new
token type would add a third value carried in the same way. The fix may need to go through WSO2's
security process, so it should not be pushed publicly until that is decided.

### OAuth core

Paths are under `components/org.wso2.carbon.identity.oauth/src/main/java/org/wso2/carbon/identity/`.

- **Lookup.** `TokenProvider.getVerifiedAccessToken(token, false)` resolves both opaque and JWT access
  tokens (`oauth2/internal/OAuth2ServiceComponentHolder.java:814-818`, then `OAuth2Util.findAccessToken`
  at 5380). Introspection uses it. It refuses a token that is not ACTIVE. Two caveats. It does not check
  the expiry time, so the caller must, with `OAuth2Util.getAccessTokenExpireMillis`. And a token served
  from `OAuthCache` is returned without its state being checked again.
- **Sign-in state.** `AuthorizationGrantCacheEntry` holds `authTime`, `selectedAcrValue`, `amrList` and
  `sessionContextIdentifier` (`oauth/cache/AuthorizationGrantCacheEntry.java:49-119`). The entry is
  stored against the token id and persisted to `SessionDataStore`. A cache miss reads it from the
  database. It lives for the refresh token's validity.

  | Grant | What the entry holds |
  |---|---|
  | `authorization_code` | Everything, copied from the SSO session |
  | `refresh_token` | The same entry object, moved to the new token, so the original `auth_time` survives |
  | implicit and hybrid | `authTime` and `maxAge`, no `amr` |
  | `password`, `device_code` | User attributes, and for device the session id. No `authTime` or `amr`. |
  | `client_credentials` | No entry |

  This answers the first Phase 2 open question: the values come from this entry. The `max_age` rule
  controls what the id_token shows, not what is stored. That `authTime` is always set on the code
  grant's entry is still to be checked.
- **Token to session.** `OAuthTokenSessionMappingEventHandler` is on by default. It writes a
  token-to-session row for the code, refresh and implicit grants, and
  `AccessTokenDAO.getSessionIdentifierByTokenId` reads it. Logout revokes tokens through the same
  mapping, so an active subject token is evidence that the session has not ended.
- **Sender constraint.** `AccessTokenDO.getTokenBinding()` records the binding type (`sso-session`,
  `cookie`, `certificate`, `DPoP`). The only binding check runs at revocation and userinfo. This repo has
  no DPoP proof validation. Nothing on a token records whether the client that obtained it was
  confidential. The exchange request authenticates its own client, so that can be known at exchange
  time instead.
- **Introspection** exposes no `auth_time`, `acr` or `amr`. The extension point is
  `IntrospectionDataProvider`. This matters for Phase 5, not Phase 2.
- `JWTTokenIssuer` emits no `acr`, `amr` or `auth_time` on access tokens. The id_token emits
  `auth_time` only when `max_age` was in the original request or `auth_time` was an essential claim.
- Pre-existing bug: `TokenBindingExpiryEventHandler:399` returns early when one of a session's tokens
  has no access token, so the rest are not revoked on logout. This weakens the check that the subject
  token is still active.

### Earlier findings that still stand

- There is **no atomic single-use mechanism** to reuse. The authorization code's enforcement is
  read-then-write: the state check runs against a separately read row, and the deactivation SQL has no
  `AND STATE='ACTIVE'` guard and discards the affected row count.
- The **impersonation subject token** is a close structural precedent — per-app enablement, per-app
  expiry defaulting to 180 seconds, designed to be handed into the token exchange grant. It is not
  single use and carries no `acr`/`auth_time`.
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

- **Phase 2** — the launch token and the real seed executor. Designed on 2026-09-24, not started. The
  reasoning is in section 3 and the findings in section 5.

  ```
  App ──► POST /oauth2/token
            grant_type=urn:ietf:params:oauth:grant-type:token-exchange
            subject_token=<the app's access token, opaque or JWT>
            subject_token_type=urn:ietf:params:oauth:token-type:access_token
            requested_token_type=<WSO2 URN, name not decided>
            + client authentication

     ◄── { access_token: <random handle>, issued_token_type: <that URN>,
           token_type: N_A, expires_in: 60 }       no refresh token, no id_token

  App ──► opens execution-flow.jsp?flowType=SECURED_SELF_SERVICE_OPERATIONS
            &launch_token=<handle>&operation=<claim URI>
  ```

  When the token is issued, in the connector:
  1. Resolve the subject token through `TokenProvider`, which covers opaque tokens. Then check its
     expiry, which the lookup does not.
  2. Refuse if the subject token was not issued to the requesting client.
  3. Refuse a token with no user, such as one from `client_credentials`.
  4. Read `authTime`, `selectedAcrValue`, `amrList` and the session id from the grant cache entry. Fall
     back to the token-to-session mapping when the entry has no session id.
  5. Record whether the requesting client authenticated as a confidential client.
  6. Store a row keyed by the hash of a random handle, with a 60 second expiry. Return the handle.

  When the flow uses it, in the seed executor:
  1. Consume the row atomically: `UPDATE … SET STATE='USED' WHERE HASH=? AND STATE='ACTIVE' AND
     EXPIRY>?`. Accept only when exactly one row changed.
  2. Check that the subject token is still active. This ends the transfer token when the user signs out.
  3. Take the application from the stored `client_id`, not from `spId`.
  4. Set the flow user and the inherited evidence.

  Where the code goes:
  - Framework, `step-up-mgt`: the table, and a small service that issues and consumes rows. The table
    needs scripts for every supported database, and migration scripts.
  - Connector: the new token type in `TokenExchangeGrantHandler`, and the seed executor, which replaces
    `AccessTokenSeedExecutor`. This keeps the earlier decision that the seed executor ships with the
    token it validates. The executor must check the subject token through OAuth, and the framework cannot
    depend on OAuth. `validateGrant()` detects the new type early. `issue()` returns the handle instead of
    calling `super.issue()`. The type travels on the request context, not on the handler.
  - First, before either: move the connector's framework pin from `7.8.675` to 7.11.x, and fix the
    shared handler state described in section 5. The pin was the earlier blocker, because the executor
    SPI does not exist in `7.8.675`.

  Decisions still open:
  1. **Does inherited evidence depend on how the exchange was made?** Proposed: from a confidential
     client, inherited `acr`/`auth_time` can skip factors. From a public client, the transfer token still
     replaces sign-in, but every configured factor runs. Nothing reads the evidence until #634 lands, so
     this does not block building.
  2. **A new table for the rows?** Proposed, because an atomic consume needs a conditional `UPDATE` and
     `SessionDataStore` has none.
  3. **The name of the token type URN.**
  4. **How the shared-state bug is handled.** It is being analysed in a separate session.

  To check before building:
  - That `authTime` is always set on the code grant's cache entry, not only when `max_age` is sent.
  - Whether `amrList` holds authenticator names, which the inherited factors are compared with, or
    standard `amr` values.

  Recorded but not planned: an app that renders the flow itself through the flow execution API could
  send its access token in a header and need no transfer token. A browser that shares the IS session
  cookie from sign-in could be seeded from that session. Neither is reliable enough for the webview
  case.
- **Phase 4** — hosted UI beyond what the accounts portal already renders. Smaller than it was: the
  multi-attribute problem it was meant to solve is solved by branching, and the superset form it
  proposed rested on a filter that does not exist.
- **Phase 5** — REST enforcement in SCIM2, and `acr`/`auth_time` on access tokens and introspection.
  Until this exists, protected attributes remain freely updatable over `/scim2/Me` while the flow
  prompts for an OTP. That asymmetry is expected at this stage and is the open scoping question in the
  brief.
- **Phase 6** — mostly done. The Console lists the flow, opens it in a builder and saves it. The
  branching is derived from claim URIs, so adding a branch for a new attribute needs only the editor.
  That has not been tried end to end. What is left is the static check that each branch's collection
  form carries that branch's claim (section 4). The API half: a `SecuredSelfServiceOperationsFlowMetaHandler`, its
  case in `resolveHandler`, and the flow type added to every `flowType` enum in `flow.yaml` and the
  checked-in `FlowApi`. That branch also bumps `carbon.identity.framework.version` from `7.11.195` to
  `7.11.205-SNAPSHOT`, because `Constants.FlowTypes.SECURED_SELF_SERVICE_OPERATIONS` does not exist in
  the released version — upstream this lands after a framework release carrying the enum.
- **Phase 7** — integration tests in `product-is`

---

## 7. Running it

See the PoC harness and the flow definition under
`org.wso2.carbon.identity.stepup.flow.testing/testing/`.

Deploy to `dropins`: the two step-up bundles and the testing bundle, plus the two changed
`flow-orchestration-framework` bundles and `org.wso2.carbon.identity.local.auth.emailotp`. Copy the flow
definition to `<IS_HOME>/repository/conf/flow-defaults/` and `execution-flow.jsp` to the deployed
`accounts` webapp. For the Console, build `identity-apps` and copy `apps/console/build/console` over the
deployed webapp — clearing its `static/js` first, because the copy is additive and old content-hashed
chunks otherwise accumulate. Start the server with `-DStepUp.AllowUnverifiedSeed=true`.

The flow definition is read once and cached by `SystemDefaultFlowProvider`, so changing it needs a
restart even before anyone saves from the editor.

Enable the flow, remembering that `PATCH /flow/config` needs `"flowCompletionConfigs":{}` until the
`FlowConfigDTO` fix is deployed. The shipped flow reads no `STEP_UP_POLICY`. Only
`StepUpDecisionExecutor` reads it, and the default flow no longer contains that executor.

Until Phase 2, `launch_token` is a JWT access token, read by `AccessTokenSeedExecutor`. Phase 2
replaces it with the opaque handle from token exchange.

```
https://localhost:9443/accounts/execution-flow.jsp
  ?flowType=SECURED_SELF_SERVICE_OPERATIONS&spId=<appId>
  &launch_token=<jwt>
  &operation=<claim URI, URL encoded>
```

`operation` names the claim to change — `http://wso2.org/claims/mobile` or
`http://wso2.org/claims/emailaddress` in the shipped definition, URL encoded. Leave it off and the
choice page renders after the OTP instead. The OTP step needs two submits: the first with the field
empty sends the code.

Adding a third protected attribute means a button on the choice page and a collection step whose field
identifier is the claim. Nothing else: no metadata, and no identifiers to copy between nodes.
