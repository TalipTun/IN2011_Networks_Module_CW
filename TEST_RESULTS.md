# CRN Test Results Log

Use this file after each test run. Keep old runs; do not overwrite history.

## Run Header Template

Copy this block for each run:

```text
Run ID: RUN-YYYYMMDD-XX
Date: YYYY-MM-DD
Tester: <name>
Branch/Commit: <git branch + short hash or N/A>
Build Command: javac -d out src/*.java
Build Result: PASS | FAIL
Scope: Smoke | Relay | Robustness | Full Regression
Notes:
- 
```

Add this section at the end of every run:

```text
## Cases To Pass (Full Project)
- [ ] B-01 Build gate
- [ ] GH-01..GH-04 Name request/response family
- [ ] NO-01..NO-04 Nearest request/response family
- [ ] V-01..V-08 Relay forwarding + tx rewrite + non-blocking
- [ ] RS-01..RS-05 Relay stack routing policy
- [ ] R-01..R-04 Retry/timeout reliability
- [ ] API-01 `isActive`
- [ ] API-04 `read`
- [ ] API-05 `write`
- [ ] API-06 `CAS`
- [ ] A-01..A-06 Robustness/adversarial suite
- [ ] PERF-01..PERF-03 Performance sanity
Release Ready: YES | NO
```

## Status Legend

- `PASS`: test met expected result
- `FAIL`: test did not meet expected result
- `KNOWN-FAIL`: expected to fail due to unimplemented feature
- `BLOCKED`: could not execute (environment/setup issue)
- `N/A`: not applicable in this run

## Per-Test Result Template

Use test IDs from [`TEST_CASES.md`](/Users/taliptun/Desktop/CRN/TEST_CASES.md).

```text
[TEST_ID] <short title>
Status: PASS | FAIL | KNOWN-FAIL | BLOCKED | N/A
Observed:
- 
Expected:
- 
Evidence:
- Terminal output snippet:
- Packet evidence (pcap/hex):
Follow-up:
- 
```

## Run Record

## RUN-20260422-01

Run ID: RUN-20260422-01  
Date: 2026-04-22  
Tester: Talip Tun  
Branch/Commit: N/A  
Build Command: `javac -d out src/*.java`  
Build Result: PASS  
Scope: Smoke  
Notes:
- Template run created. Replace placeholders with your actual observations.

### Results

[B-01] Project compiles  
Status: PASS  
Observed:
- `javac -d out src/*.java` completed with exit code 0.
Expected:
- Compile succeeds with no errors.
Evidence:
- Terminal output: no compiler errors.
- Packet evidence: N/A
Follow-up:
- Continue with GH-01, P-05, P-01 smoke tests.

[GH-01] Basic `G` response  
Status: N/A  
Observed:
- 
Expected:
- Response txid matches request, response type is `H`.
Evidence:
- 
Follow-up:
- Run with `scripts/send_udp_hex.sh`.

[V-01] Relay forward request  
Status: N/A  
Observed:
- 
Expected:
- Relay forwards embedded request to named target.
Evidence:
- 
Follow-up:
- Execute once multi-node test harness is ready.

## Regression Summary

Update this after each run:

```text
Total tests considered:
PASS:
FAIL:
KNOWN-FAIL:
BLOCKED:
N/A:
```

## Cases To Pass (Full Project)

- [x] B-01 Build gate
- [ ] GH-01..GH-04 Name request/response family
- [ ] NO-01..NO-04 Nearest request/response family
- [ ] V-01..V-08 Relay forwarding + tx rewrite + non-blocking
- [ ] RS-01..RS-05 Relay stack routing policy
- [ ] R-01..R-04 Retry/timeout reliability
- [ ] API-01 `isActive`
- [ ] API-04 `read`
- [ ] API-05 `write`
- [ ] API-06 `CAS`
- [ ] A-01..A-06 Robustness/adversarial suite
- [ ] PERF-01..PERF-03 Performance sanity
Release Ready: NO

## RUN-20260428-01

Run ID: RUN-20260428-01  
Date: 2026-04-28  
Tester: Codex + Talip Tun  
Branch/Commit: local workspace (uncommitted)  
Build Command: `javac -d out src/*.java`  
Build Result: PASS  
Scope: Protocol families beyond LocalTest (`G/H`, `N/O`, `V`)  
Notes:
- Started `PersonalTest` on UDP `12345`.
- Sent raw packets with `scripts/send_udp_hex.sh`.
- Captured sender outputs and receiver logs from `/tmp/crn_proto_run.log` and `/tmp/crn_v_run.log`.

### Results

[GH-01] Basic `G` response  
Status: PASS  
Observed:
- Request `01022047` returned `0102204830204e3a746573743020`.
- Txid preserved (`0102`), response type `H`, payload decodes to `N:test0`.
Expected:
- `H` response with same txid and encoded node name.
Evidence:
- Sender output (`/tmp/gh.out`) shows:
  - `--- response hex ---`
  - `0102204830204e3a746573743020`
Follow-up:
- None.

[NO-01] Valid `N` response  
Status: PASS  
Observed:
- Request returned response type `O` with same txid.
- Response hex: `0103204f30204e3a74657374302030203132372e302e302e313a313233343520`.
Expected:
- `O` response preserving txid for valid nearest request.
Evidence:
- Sender output (`/tmp/no.out`) includes `0103204f...` and payload `N:test0 0 127.0.0.1:12345`.
Follow-up:
- Add additional cases for 0-neighbor and >3-neighbor trimming in multi-node topology.

[V-02] Relay response behavior (tx rewrite + response type)  
Status: FAIL  
Observed:
- Sent `V(target=N:test0, embedded=0A0B G)` using:
  - `0105205630204e3a7465737430200a0b2047`
- Returned packet was `01052047` (outer txid + type `G`) instead of expected relayed `H`.
Expected:
- Relay should return a valid response packet for embedded request semantics (`H` for embedded `G`) with correct outer txid rewrite.
Evidence:
- Sender output (`/tmp/v.out`, `/tmp/v2.out`) both show:
  - `--- response hex ---`
  - `01052047`
- Receiver log shows relay forwarding:
  - `[V] embeddedType=G, isRequest=true`
  - `[V] forwarding to 127.0.0.1:12345`
Follow-up:
- Investigate relay pending-map handling: forwarded embedded request currently gets rewritten and echoed back as request type instead of waiting for and relaying response type.

## Regression Summary (RUN-20260428-01)

```text
Total tests considered: 3
PASS: 2
FAIL: 1
KNOWN-FAIL: 0
BLOCKED: 0
N/A: 0
```

## RUN-20260428-02

Run ID: RUN-20260428-02  
Date: 2026-04-28  
Tester: Codex + Talip Tun  
Branch/Commit: local workspace (uncommitted)  
Build Command: `javac -d out src/*.java`  
Build Result: PASS  
Scope: Post-fix verification for protocol families (`G/H`, `N/O`, `V`)  
Notes:
- Patched relay pending-map handling in `Node.handleIncomingMessages(...)` so txid rewrite is applied only to response-type packets, not embedded request packets.
- Re-ran same raw packet suite against `PersonalTest` on UDP `12345`.

### Results

[GH-01] Basic `G` response  
Status: PASS  
Observed:
- Response hex: `0102204830204e3a746573743020`.
Expected:
- Same txid with `H` payload for `N:test0`.
Evidence:
- `/tmp/gh2.out` response dump.
Follow-up:
- None.

[NO-01] Valid `N` response  
Status: PASS  
Observed:
- Response hex: `0103204f30204e3a74657374302030203132372e302e302e313a313233343520`.
Expected:
- Same txid with response type `O`.
Evidence:
- `/tmp/no2.out` response dump.
Follow-up:
- None.

[V-02] Relay response behavior (tx rewrite + response type)  
Status: PASS  
Observed:
- Request: `0105205630204e3a7465737430200a0b2047`
- Response now: `0105204830204e3a746573743020` (outer txid + `H` response).
Expected:
- Relay returns embedded request response semantics with outer txid rewrite.
Evidence:
- `/tmp/v3.out` response dump.
- `/tmp/crn_proto_run2.log` shows forwarded embedded `G` (`0a0b`) and resulting embedded `H` before rewrite.
Follow-up:
- Keep dedicated relay regression test in future runs.

## Regression Summary (RUN-20260428-02)

```text
Total tests considered: 3
PASS: 3
FAIL: 0
KNOWN-FAIL: 0
BLOCKED: 0
N/A: 0
```

## RUN-20260428-03

Run ID: RUN-20260428-03  
Date: 2026-04-28  
Tester: Codex + Talip Tun  
Branch/Commit: local workspace (uncommitted)  
Build Command: `javac -d out src/*.java`  
Build Result: PASS  
Scope: Reliability/timeout tests (`R-01..R-04`)  
Notes:
- Executed dedicated reliability harnesses:
  - `/tmp/RetryHarness.java` for unreachable target + unrelated packet filtering.
  - `/tmp/RetryHarnessR02.java` for second-attempt success behavior.

### Results

[R-01] No response path (unreachable target)  
Status: PASS  
Observed:
- `isActive(\"N:ghost\")` returned `false`.
- Elapsed time was `15006 ms` (consistent with 3 attempts x ~5s timeout window).
Expected:
- Maximum retries then failure return for unreachable node.
Evidence:
- Harness output:
  - `R01_ACTIVE=false`
  - `R01_ELAPSED_MS=15006`
Follow-up:
- None.

[R-02] Response on second try  
Status: PASS  
Observed:
- Responder intentionally dropped first request and replied on second.
- Client succeeded with `H` response; elapsed `5004 ms`.
Expected:
- Retry logic should recover on later attempt and return success.
Evidence:
- Harness output:
  - `R02_SUCCESS=true`
  - `R02_ELAPSED_MS=5004`
Follow-up:
- None.

[R-03] Ignore unrelated packets while waiting  
Status: PASS  
Observed:
- Responder sent wrong-tx `H` packet first, then correct txid response.
- Client matched correct txid/response and succeeded.
Expected:
- Unrelated packets must not satisfy request wait loop.
Evidence:
- Harness output:
  - `R03_MATCHED_CORRECT=true`
Follow-up:
- None.

[R-04] Timeout budget sanity  
Status: PASS  
Observed:
- Unreachable path consumed about `15006 ms` across 3 attempts.
- Second-attempt path completed around `5004 ms`.
Expected:
- Each attempt waits up to ~5s and overall timing matches retry policy intent.
Evidence:
- `R01_ELAPSED_MS=15006`
- `R02_ELAPSED_MS=5004`
Follow-up:
- Optional: expose attempt counters/log lines for even clearer audit.

## Regression Summary (RUN-20260428-03)

```text
Total tests considered: 4
PASS: 4
FAIL: 0
KNOWN-FAIL: 0
BLOCKED: 0
N/A: 0
```

## RUN-20260428-04

Run ID: RUN-20260428-04  
Date: 2026-04-28  
Tester: Codex + Talip Tun  
Branch/Commit: local workspace (uncommitted)  
Build Command: `javac -d out src/*.java`  
Build Result: PASS  
Scope: Additional protocol families (`E/F`, `R/S`, `W/X`, `C`)  
Notes:
- Executed `/tmp/ProtocolERWC.java` harness against `Node` UDP behavior.
- Harness uses short CRN strings (`0 <text> ` form) for deterministic payload parsing.

### Results

[EF-01] `E` existing key -> `F Y`  
Status: PASS  
Observed:
- `EF_EXIST_TYPE=F CODE=Y`
- Hex: `f958204659`
Expected:
- Existing key returns `F Y`.
Evidence:
- Harness stdout.
Follow-up:
- None.

[EF-02] `E` missing key -> `F N`  
Status: PASS  
Observed:
- `EF_MISS_TYPE=F CODE=N`
- Hex: `4d9f20464e`
Expected:
- Missing key but within closest policy path returns `F N`.
Evidence:
- Harness stdout.
Follow-up:
- Add explicit far-node scenario later for `F X`.

[WX-01] Initial `W` accept -> `X A`  
Status: PASS  
Observed:
- `WX_FIRST_TYPE=X CODE=A`
- Hex: `cc84205841`
Expected:
- First write for eligible closest node returns `X A`.
Evidence:
- Harness stdout.
Follow-up:
- None.

[WX-02] Overwrite `W` -> `X R`  
Status: PASS  
Observed:
- `WX_OVERWRITE_TYPE=X CODE=R`
- Hex: `2328205852`
Expected:
- Existing key overwrite returns `X R`.
Evidence:
- Harness stdout.
Follow-up:
- None.

[RS-01] `R` hit -> `S Y` (+value)  
Status: PASS  
Observed:
- `RS_HIT_TYPE=S CODE=Y`
- Hex: `49c8205359302074776f20` (contains encoded value for `two`)
Expected:
- Read hit returns `S Y` and encoded value payload.
Evidence:
- Harness stdout.
Follow-up:
- None.

[RS-02] `R` miss -> `S N`  
Status: PASS  
Observed:
- `RS_MISS_TYPE=S CODE=N`
- Hex: `092520534e`
Expected:
- Read miss within closest path returns `S N`.
Evidence:
- Harness stdout.
Follow-up:
- Add explicit far-node scenario later for `S X`.

[C-01] `CAS` match -> `R`  
Status: PASS  
Observed:
- `C_MATCH_TYPE=R`
- Hex: `39192052`
Expected:
- Matching current value performs swap and returns `R`.
Evidence:
- Harness stdout.
Follow-up:
- None.

[C-02] `CAS` mismatch -> `N`  
Status: PASS  
Observed:
- `C_MISMATCH_TYPE=N`
- Hex: `7fe6204e`
Expected:
- Non-matching current value returns `N`.
Evidence:
- Harness stdout.
Follow-up:
- None.

[C-03] `CAS` absent key at closest -> `A`  
Status: PASS  
Observed:
- `C_ABSENT_TYPE=A`
- Hex: `30172041`
Expected:
- Missing key at closest node returns `A`.
Evidence:
- Harness stdout.
Follow-up:
- Add explicit not-closest scenario later for `X`.

## Regression Summary (RUN-20260428-04)

```text
Total tests considered: 9
PASS: 9
FAIL: 0
KNOWN-FAIL: 0
BLOCKED: 0
N/A: 0
```

## RUN-20260428-05

Run ID: RUN-20260428-05  
Date: 2026-04-28  
Tester: Codex + Talip Tun  
Branch/Commit: local workspace (uncommitted)  
Build Command: `javac -d out src/*.java`  
Build Result: PASS  
Scope: Remaining edge/adversarial/performance cases  
Notes:
- Executed `/tmp/EdgeCasesHarness.java` for far-node protocol codes.
- Executed malformed packet + random-fuzz checks against `PersonalTest`.
- Executed sequential performance sanity loop for `G/H`.

### Results

[EF-03] `E` far/not-closest -> `F X`  
Status: PASS  
Observed:
- `EF_FAR_CODE=X`.
Expected:
- Not-closest path returns `F X`.
Evidence:
- Edge harness output.
Follow-up:
- None.

[RS-03] `R` far/not-closest miss -> `S X`  
Status: PASS  
Observed:
- `RS_FAR_CODE=X`.
Expected:
- Not-closest path returns `S X`.
Evidence:
- Edge harness output.
Follow-up:
- None.

[C-04] `CAS` absent/not-closest -> `X`  
Status: PASS  
Observed:
- `CAS_FAR_TYPE=X`.
Expected:
- Missing key on not-closest path returns `X`.
Evidence:
- Edge harness output.
Follow-up:
- None.

[P-02] Tx byte 1 is space  
Status: PASS  
Observed:
- Sent `20022047`, got `[no response]`.
- Receiver continued running.
Expected:
- Packet rejected without crash.
Evidence:
- `/tmp/a1.out` and `/tmp/crn_adv.log`.
Follow-up:
- None.

[P-03] Tx byte 2 is space  
Status: PASS  
Observed:
- Sent `01202047`, got `[no response]`.
- Receiver continued running.
Expected:
- Packet rejected without crash.
Evidence:
- `/tmp/a2.out` and `/tmp/crn_adv.log`.
Follow-up:
- None.

[P-04] Missing required space after txid  
Status: PASS  
Observed:
- Sent `010247`, got `[no response]`.
- Receiver logged malformed packet and continued.
Expected:
- Rejected safely.
Evidence:
- `/tmp/a3.out` and log entries.
Follow-up:
- None.

[A-01] Random bytes fuzz (sample run)  
Status: PASS  
Observed:
- Sent 30 random packets (`FUZZ_SENT=30`).
- Receiver logged malformed packets but stayed alive.
- Post-fuzz `G` request succeeded with valid `H` response.
Expected:
- No crash/deadlock under malformed/random input.
Evidence:
- `/tmp/a5.out`, `/tmp/a6.out`, `/tmp/crn_adv.log`.
Follow-up:
- Increase fuzz volume in future if needed.

[PERF-01] Sequential `G/H` throughput sanity  
Status: PASS  
Observed:
- 300 sequential `G` requests.
- `PERF_G_OK=300`, `PERF_G_FAIL=0`.
- `PERF_G_ELAPSED_MS=310741`, `PERF_G_REQ_PER_SEC=0.97`.
Expected:
- Stable success/no crashes during sustained request loop.
Evidence:
- Performance command output.
Follow-up:
- Optional: optimized sender harness for higher req/s measurement.

## Regression Summary (RUN-20260428-05)

```text
Total tests considered: 8
PASS: 8
FAIL: 0
KNOWN-FAIL: 0
BLOCKED: 0
N/A: 0
```

## RUN-20260422-04

Run ID: RUN-20260422-04  
Date: 2026-04-22  
Tester: Codex + Talip Tun  
Branch/Commit: local workspace (uncommitted)  
Build Command: `javac -d out src/*.java`  
Build Result: PASS  
Scope: Relay/sendRequest verification (direct + one-hop + two-hop + pop flow)  
Notes:
- Executed `/tmp/RelayHarness.java` against current `sendRequest` and `V` handling.
- Harness creates 4 nodes in-process: `client`, `r1`, `r2`, `dest`.

### Results

[API-01-DIRECT] `isActive` direct path  
Status: PASS  
Observed:
- Direct request/response succeeded.
Expected:
- `isActive(\"N:dest\")` returns true with no relays.
Evidence:
- Harness output: `API-01-DIRECT PASS`.
Follow-up:
- None.

[RS-02] Relay one-hop via `r1`  
Status: PASS  
Observed:
- `client -> r1 -> dest` path succeeded.
Expected:
- One-hop relay returns valid `H` response.
Evidence:
- Harness output: `RS-02 PASS`.
Follow-up:
- None.

[RS-03] Relay two-hop via `r1,r2`  
Status: FAIL  
Observed:
- `client -> r1 -> r2 -> dest` forwarding occurred, but response did not return as expected to client.
- Harness output: `RS-03 FAIL`.
Expected:
- Two-hop relay returns valid `H` response to original sender.
Evidence:
- Harness output logs and failure summary.
Follow-up:
- Investigate relay response mapping for embedded `V` messages (outer relay currently treats embedded `V` as non-request).

[RS-05] After pop top relay, remaining one-hop works  
Status: PASS  
Observed:
- After popping `r2`, route via `r1` succeeded.
Expected:
- Remaining stack relay path still works.
Evidence:
- Harness output: `RS-05 PASS`.
Follow-up:
- None.

[RS-04] After clearing stack, direct works  
Status: PASS  
Observed:
- After popping all relays, direct request succeeded again.
Expected:
- Reverts to direct path.
Evidence:
- Harness output: `RS-04 PASS`.
Follow-up:
- None.

## Regression Summary (RUN-20260422-04)

```text
Total tests considered: 5
PASS: 4
FAIL: 1
KNOWN-FAIL: 0
BLOCKED: 0
N/A: 0
```

## Cases To Pass (Full Project)

- [x] B-01 Build gate
- [ ] GH-01..GH-04 Name request/response family
- [ ] NO-01..NO-04 Nearest request/response family
- [ ] V-01..V-08 Relay forwarding + tx rewrite + non-blocking
- [ ] RS-01..RS-05 Relay stack routing policy
- [ ] R-01..R-04 Retry/timeout reliability
- [x] API-01 `isActive`
- [ ] API-04 `read`
- [ ] API-05 `write`
- [ ] API-06 `CAS`
- [ ] A-01..A-06 Robustness/adversarial suite
- [ ] PERF-01..PERF-03 Performance sanity
Release Ready: NO

## Open Defects

Track defects found by tests:

```text
DEFECT-ID: DEF-YYYYMMDD-XX
Found in Run: RUN-...
Related Test IDs: ...
Severity: High | Medium | Low
Description:
Repro Steps:
Expected:
Actual:
Status: Open | In Progress | Fixed | Verified
```

## RUN-20260422-02

Run ID: RUN-20260422-02  
Date: 2026-04-22  
Tester: Codex + Talip Tun  
Branch/Commit: local workspace (uncommitted)  
Build Command: `javac -d out src/*.java`  
Build Result: PASS  
Scope: Smoke + API baseline + parser safety  
Notes:
- Executed raw packet tests sequentially against `PersonalTest` on UDP `12345`.
- Executed Java API harness at `/tmp/ApiHarness.java` to validate interface-level behavior.
- Receiver crash on short packet is reproducible and logged as a defect.

### Results

[B-01] Project compiles  
Status: PASS  
Observed:
- `javac -d out src/*.java` exit code `0`.
Expected:
- Compile succeeds with no errors.
Evidence:
- Terminal: no compiler errors.
Follow-up:
- None.

[B-02] Node name uniqueness  
Status: PASS  
Observed:
- Second `setNodeName(\"N:dup\")` threw exception (`Name already exists` wrapped by current code path).
Expected:
- Duplicate name assignment is rejected.
Evidence:
- API harness output: `B-02 PASS threw Exception`.
Follow-up:
- Improve exception message clarity later (optional).

[B-05] `popRelay` on empty stack  
Status: PASS  
Observed:
- No exception thrown.
Expected:
- No-op behavior.
Evidence:
- API harness output: `B-05 PASS`.
Follow-up:
- None.

[B-06] `pushRelay(null)`  
Status: PASS  
Observed:
- Exception thrown.
Expected:
- Invalid relay input rejected.
Evidence:
- API harness output: `B-06 PASS threw Exception`.
Follow-up:
- None.

[B-07] `pushRelay` invalid prefix  
Status: PASS  
Observed:
- `pushRelay(\"X:notNode\")` threw exception.
Expected:
- Relay must start with `N:`.
Evidence:
- API harness output: `B-07 PASS threw Exception`.
Follow-up:
- None.

[B-08] `pushRelay` valid value  
Status: PASS  
Observed:
- `pushRelay(\"N:r1\")` accepted.
Expected:
- Valid relay added.
Evidence:
- API harness output: `B-08 PASS`.
Follow-up:
- Add observable stack-size accessor only for tests if needed.

[API-EX-01] `exists` true path  
Status: PASS  
Observed:
- `exists(\"N:dup\")` returned true.
Expected:
- Existing map key returns true.
Evidence:
- API harness output: `API-EX-01 PASS`.
Follow-up:
- None.

[API-EX-02] `exists` false path  
Status: PASS  
Observed:
- `exists(\"N:not-there\")` returned false.
Expected:
- Missing map key returns false.
Evidence:
- API harness output: `API-EX-02 PASS`.
Follow-up:
- None.

[API-04] `read` current behavior  
Status: KNOWN-FAIL  
Observed:
- Throws `Not implemented`.
Expected:
- Should eventually return value or null by protocol.
Evidence:
- API harness output: `API-04 PASS threw Exception: Not implemented`.
Follow-up:
- Implement `read` via shared `sendRequest`.

[API-05] `write` current behavior  
Status: KNOWN-FAIL  
Observed:
- Throws `Not implemented`.
Expected:
- Should eventually return boolean success.
Evidence:
- API harness output: `API-05 PASS threw Exception: Not implemented`.
Follow-up:
- Implement `write` via shared `sendRequest`.

[API-06] `CAS` current behavior  
Status: KNOWN-FAIL  
Observed:
- Throws `Not implemented`.
Expected:
- Should eventually perform atomic compare-and-swap and return boolean.
Evidence:
- API harness output: `API-06 PASS threw Exception: Not implemented`.
Follow-up:
- Implement `CAS` via shared `sendRequest`.

[GH-01] Basic `G` response  
Status: PASS  
Observed:
- Request `01022047` returned `0102204830204e3a746573743020`.
- Txid preserved (`0102`), response type `H`, payload decodes to `N:test0`.
Expected:
- `H` response with same txid and encoded node name.
Evidence:
- `send_udp_hex.sh` output and hexdump.
Follow-up:
- None.

[P-05] Unknown message type handling  
Status: PASS  
Observed:
- Request `0102205a` produced no response; receiver logged type byte `90` and continued.
Expected:
- Ignore/log unknown type without crashing.
Evidence:
- Sender output: `[no response]`
- Receiver log: `raw=[1, 2, 32, 90]` then `90`.
Follow-up:
- Consider structured logging for unknown types.

[P-01] Packet shorter than 4 bytes  
Status: FAIL  
Observed:
- Request `0102` caused exception and receiver process exit.
- Stack trace: `Packet is shorter than minimum required length` from `Node.handleIncomingMessages`.
Expected:
- Reject malformed packet safely and continue processing.
Evidence:
- Receiver log stack trace from sequential test run.
Follow-up:
- Fix parser to drop short packets instead of throwing fatal exception.

[API-01] `isActive` direct path  
Status: PASS  
Observed:
- In-process harness with `N:test0@12345` and `N:test1@12346` returned true for `isActive(\"N:test0\")`.
Expected:
- Reachable node responds with matching `H` payload.
Evidence:
- API harness output: `API-01 PASS isActive should succeed`.
Follow-up:
- Add explicit relay-stack test for `isActive` path (single/multi-hop).

## Regression Summary (RUN-20260422-02)

```text
Total tests considered: 14
PASS: 11
FAIL: 1
KNOWN-FAIL: 3
BLOCKED: 0
N/A: 0
```

## Cases To Pass (Full Project)

- [x] B-01 Build gate
- [ ] GH-01..GH-04 Name request/response family
- [ ] NO-01..NO-04 Nearest request/response family
- [ ] V-01..V-08 Relay forwarding + tx rewrite + non-blocking
- [ ] RS-01..RS-05 Relay stack routing policy
- [ ] R-01..R-04 Retry/timeout reliability
- [x] API-01 `isActive`
- [ ] API-04 `read`
- [ ] API-05 `write`
- [ ] API-06 `CAS`
- [ ] A-01..A-06 Robustness/adversarial suite
- [ ] PERF-01..PERF-03 Performance sanity
Release Ready: NO

## Open Defects

DEFECT-ID: DEF-20260422-01  
Found in Run: RUN-20260422-02  
Related Test IDs: P-01  
Severity: High  
Description:
- A malformed packet shorter than 4 bytes crashes the receiver loop instead of being safely ignored.
Repro Steps:
- Start `java -cp out PersonalTest`.
- Send `0102` using `scripts/send_udp_hex.sh`.
Expected:
- Receiver remains alive and ignores malformed packet.
Actual:
- `Node.handleIncomingMessages` throws exception and process exits.
Status: Verified (fixed in RUN-20260422-03)

## RUN-20260422-03

Run ID: RUN-20260422-03  
Date: 2026-04-22  
Tester: Codex + Talip Tun  
Branch/Commit: local workspace (uncommitted)  
Build Command: `javac -d out src/*.java`  
Build Result: PASS  
Scope: Targeted regression (P-01 fix verification + smoke sanity)  
Notes:
- Patched `handleIncomingMessages` to ignore malformed packets instead of throwing fatal exceptions.
- Re-ran only affected parser/smoke checks.

### Results

[P-01] Packet shorter than 4 bytes  
Status: PASS  
Observed:
- Sent `0102`; sender got `[no response]`.
- Receiver logged `Ignoring malformed packet: shorter than minimum required length`.
- Receiver remained alive and continued processing.
Expected:
- Malformed short packet is safely ignored with no crash.
Evidence:
- Sender output from `scripts/send_udp_hex.sh`.
- Receiver session log after packet handling.
Follow-up:
- Expand malformed parser tests (`tx contains space`, `missing tx delimiter`) now that loop is resilient.

[GH-01] Basic `G` response sanity after fix  
Status: PASS  
Observed:
- Sent `01022047`, got response hex `0102204830204e3a746573743020`.
Expected:
- `H` reply with same txid and encoded node name.
Evidence:
- Sender hexdump output.
Follow-up:
- None.

[P-05] Unknown type sanity after fix  
Status: PASS  
Observed:
- Sent `0102205a`, got `[no response]`.
- Receiver logged unknown type (`90`) without exiting.
Expected:
- Unknown type ignored/logged, no crash.
Evidence:
- Sender output and receiver log.
Follow-up:
- None.

## Regression Summary (RUN-20260422-03)

```text
Total tests considered: 3
PASS: 3
FAIL: 0
KNOWN-FAIL: 0
BLOCKED: 0
N/A: 0
```

## Cases To Pass (Full Project)

- [x] B-01 Build gate
- [ ] GH-01..GH-04 Name request/response family
- [ ] NO-01..NO-04 Nearest request/response family
- [ ] V-01..V-08 Relay forwarding + tx rewrite + non-blocking
- [ ] RS-01..RS-05 Relay stack routing policy
- [ ] R-01..R-04 Retry/timeout reliability
- [x] API-01 `isActive`
- [ ] API-04 `read`
- [ ] API-05 `write`
- [ ] API-06 `CAS`
- [ ] A-01..A-06 Robustness/adversarial suite
- [ ] PERF-01..PERF-03 Performance sanity
Release Ready: NO
