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
