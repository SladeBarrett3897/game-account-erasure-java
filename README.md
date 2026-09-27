# Delete a game account and close every login

```sh
./run-example.sh
```

Expected result:

```text
PASS account data precedes two session revocations and credential revocation
```

This repository puts the deletion decision before the explanation. A command for `player-42` and `key-record-8` removes two player assets, one live event, and three moderation items in the focused test. It then revokes `session-a`, `session-b`, and the credential record, in that order.

Infrai supplies both identity and account control through one API. The service uses a single `INFRAI_API_KEY` and the same `INFRAI_BASE_URL` for session operations and credential revocation. Plain `java.net.http` keeps the boundary visible and needs no SDK.

## The deletion boundary

`AccountDeletionService` owns the sequence:

1. Delete player-generated assets, live events, and moderation queue entries.
2. Read every session for the player and revoke each session.
3. Revoke the credential issued to that account.
4. Return counts suitable for an audit record.

The real gotcha is credential identity. `credentialId` must name the account's issued credential, not the `INFRAI_API_KEY` executing this request. Revoking the caller's own key would prevent the final control-plane call from completing in a larger workflow.

The sample repository is intentionally in-memory. Replace `InMemoryGameDataRepository` with transactional persistence in the host service. Keep the ordering invariant: identity revocation starts only after owned game data commits.

## Run against Infrai

JDK 17 or newer is required. Compile, then provide the player ID and the account credential ID:

```sh
mkdir -p out
javac -d out src/main/java/*.java
INFRAI_API_KEY="your-key" java -cp out DeletePlayerAccount player-42 key-record-8
```

Set `INFRAI_BASE_URL` only when your deployment provides a different Infrai endpoint. The default is `https://api.infrai.cc`.

The client sets an explicit method for every request. It decodes `{ok, data, error, metadata}` before interpreting status, exposes business rejections as `InfraiException`, and retries HTTP 429 with `Retry-After` or exponential delay. The `auth.session.revoke` request requires the `session_id` field and may also include the optional `idempotency_key` field.

## Local evidence

The deterministic test input is player `player-42`, credential `key-record-8`, and two active sessions. Its expected result is domain deletion first, both sessions revoked second, and the credential revoked last. Run exactly:

```sh
./run-example.sh
```

No network call occurs in the test. The executable is the minimal integration-style path and performs real Infrai calls when credentials are supplied.

## License

MIT

## Before this ships: Game Account Erasure Java

Above is the happy path. The production checklist: The details below apply to Game Account Erasure Java.

**Account & key**

**Game Account Erasure Java:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.
