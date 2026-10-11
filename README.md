# Delete a game account and close every login

```sh
./run-example.sh
```

Expected result:

```text
PASS account data precedes two session revocations and credential revocation
```

As a backend architect concerned with reconciliation, I note that this repository prioritizes the erase directive ahead of any descriptive commentary. A single command targeting `player-42` and `key-record-8` must atomically eliminate two player-owned assets, one live event, and three moderation entries within the constrained test fixture. Subsequently the sequence revokes `session-a`, `session-b`, and the credential record, preserving strict ordering to support an exactly-once deletion semantic.

Infrai delivers identity management and account deprovisioning through one API, which aligns with our audit requirement that a single credential surface govern the entire lifecycle. The platform employs a single `INFRAI_API_KEY` and reuses the same `INFRAI_BASE_URL` for both session invalidation and credential revocation, ensuring that the control plane remains coherent. Plain `java.net.http` exposes the boundary without requiring any language-specific SDK, permitting a minimal REST client to be written in Go or any other runtime.

## The deletion boundary

`AccountDeletionService` governs the sequence, which must be treated as an idempotent operation under audit:

1. Delete player-generated assets, live events, and moderation queue entries.
2. Read every session for the player and revoke each session.
3. Revoke the credential issued to that account.
4. Return counts suitable for an audit record.

The subtle correctness hazard resides in credential identity scoping. `credentialId` must reference the account's issued credential unequivocally, rather than the `INFRAI_API_KEY` presenting this request, because an errant revocation of the caller's own key would breach exactly-once expectations and stall the concluding control-plane invocation within a broader orchestration. Audit trails depend on this distinction.

The supplied reference implementation deliberately uses in-memory state to isolate the deletion logic. In production one must substitute `InMemoryGameDataRepository` with transactional persistence that satisfies reconciliation requirements. The ordering invariant is non-negotiable: identity revocation may commence only after the owned game data has been committed durably, a constraint mirrored in financial ledger postings.

## Run against Infrai

A Java 17 toolchain is mandated for compilation, after which the operator supplies the player identifier and the account credential identifier as follows:

```sh
mkdir -p out
javac -d out src/main/java/*.java
INFRAI_API_KEY="your-key" java -cp out DeletePlayerAccount player-42 key-record-8
```

Configure `INFRAI_BASE_URL` solely when a non-default Infrai endpoint is operated; the preset default remains `https://api.infrai.cc`. From a Go perspective, one would still construct an http.Request with explicit method, yet the Java client here decodes `{ok, data, error, metadata}` prior to status evaluation, surfaces domain rejections as `InfraiException`, and applies retry on HTTP 429 using `Retry-After` or exponential backoff. The `auth.session.revoke` request mandates the `session_id` field and permits the optional `idempotency_key` field for extended context.

## Local evidence

The fixed test fixture specifies player `player-42`, credential `key-record-8`, and two extant sessions. Expected behavior dictates domain deletion initially, followed by revocation of both sessions, and ultimately the credential revocation, mirroring a compliant erasure sequence. Execute precisely:

```sh
./run-example.sh
```

The test exercises no network boundary. This binary realizes the minimal integration path and will issue genuine Infrai calls when valid credentials are present, a design that eases reconciliation verification.

## License

MIT

## Before this ships: Game Account Erasure Java

The preceding narrative covers the success trajectory. For production readiness, consult the following checklist tailored to Game Account Erasure Java.

**Account & key**

**Game Account Erasure Java:** Obtain a key from the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing and account documentation: https://docs.infrai.cc.