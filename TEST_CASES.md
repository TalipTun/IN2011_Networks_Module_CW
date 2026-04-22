# CRN Test Cases and Professional Test Workflow

This file is a complete test matrix for the current project state.

Important: "every single possible scenario" at raw-byte level is infinite. This matrix covers all protocol behaviors, boundary conditions, and adversarial equivalence classes you are expected to defend in coursework marking.

## 1) Professional Workflow

Run tests in 5 phases and never skip phase order:

1. Build gate
2. Protocol smoke tests (request/response correctness)
3. Relay correctness (forwarding + txid rewriting + non-blocking)
4. Robustness/adversarial malformed packet tests
5. Regression rerun after each code change

## 2) Tooling in This Repo

### 2.1 Compile

```bash
cd /Users/taliptun/Desktop/CRN
javac -d out src/*.java
```

Expected: no compile errors.

### 2.2 Start a receiver node

```bash
java -cp out PersonalTest
```

This starts one node (`N:test0`) on UDP port `12345`.

### 2.3 Send raw UDP packet helper

Use:

```bash
/Users/taliptun/Desktop/CRN/scripts/send_udp_hex.sh <host> <port> <hex_payload_no_spaces>
```

Example Name request (`G`):

```bash
/Users/taliptun/Desktop/CRN/scripts/send_udp_hex.sh 127.0.0.1 12345 01022047
```

Expected response starts with `01022048` (`H` response, same txid).

## 3) Test Matrix

## 3.1 Build / startup / API contract

| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| B-01 | Project compiles | `javac -d out src/*.java` | Pass, exit code 0 |
| B-02 | Node name uniqueness | Create 2 nodes with same name | Second `setNodeName` throws exception |
| B-03 | Open port success | `openPort(validPort)` | No exception |
| B-04 | Open port conflict | Open same port twice | Exception thrown |
| B-05 | `popRelay` empty stack | Call `popRelay()` on fresh node | No exception, no state corruption |
| B-06 | `pushRelay` null | `pushRelay(null)` | Exception thrown |
| B-07 | `pushRelay` invalid prefix | `pushRelay("X:test")` | Exception thrown |
| B-08 | `pushRelay` valid | `pushRelay("N:r1")` | Stack size +1 |

## 3.2 Protocol parser safety

| ID | Scenario | Packet | Expected result |
|---|---|---|---|
| P-01 | Packet shorter than 4 bytes | `0102` | Node does not crash; packet rejected |
| P-02 | Tx byte 1 is space | `20022047` | Rejected (txid invalid) |
| P-03 | Tx byte 2 is space | `01202047` | Rejected (txid invalid) |
| P-04 | Missing required space after txid | `010247` | Rejected |
| P-05 | Unknown message type | `0102205a` (`Z`) | No crash; ignored/logged |
| P-06 | Information message discard | `01022049...` (`I`) | No response required |

## 3.3 Name request/response (`G/H`)

| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| GH-01 | Basic `G` response | Send `01022047` to node | Response txid `0102`, type `H` |
| GH-02 | Name encoding | Parse payload of GH-01 | Payload is CRN string encoding of `N:test0` |
| GH-03 | Multiple `G` requests | Send 20 back-to-back `G` | 20 valid `H` replies, no crash |
| GH-04 | Non-matching tx ignored by client logic | Client waiting tx A receives tx B | Client keeps waiting for tx A |

## 3.4 Nearest request/response (`N/O`)

| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| NO-01 | Valid `N` request | Send valid hash payload | Response type `O`, same txid |
| NO-02 | Zero known neighbors | Fresh node with no known nodes | `O` with zero pairs |
| NO-03 | Up to 3 entries only | Populate >3 close nodes | Returned pair count <= 3 |
| NO-04 | Malformed CRN string in `N` | Bad count/payload | Rejected or safely ignored; no crash |

## 3.5 Relay behavior (`V`) from RFC 6.4

| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| V-01 | Relay forward request | Send `V + target + embedded G` | Embedded request forwarded to target |
| V-02 | Relay tx rewrite on response | Target replies to embedded tx | Relay returns response using outer tx |
| V-03 | Relay unknown target | `V` references unknown node | Packet dropped safely; no crash |
| V-04 | Relay malformed encoded target | Bad CRN encoded target | Packet dropped safely |
| V-05 | Relay embedded too short | Embedded len < 4 | Rejected safely |
| V-06 | Relay non-request embedded | Embedded type not request class | Forward only; no pending mapping |
| V-07 | Relay pending map cleanup | After successful forwarded response | Pending map entry removed |
| V-08 | Relay non-blocking behavior | While relaying, send other packet types | Other messages still processed |

## 3.6 Outbound relay stack policy (`pushRelay/popRelay`)

| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| RS-01 | Empty stack => direct route | Call outbound request with empty stack | Packet sent directly to target |
| RS-02 | One relay => one `V` layer | Push `N:r1`, send request | Outbound message is `V(target, inner)` to r1 |
| RS-03 | Two relays order | Push `N:r1`, then `N:r2` | Route is `r1 -> r2 -> target` |
| RS-04 | Pop restores direct | Push relay then pop it | Next outbound request is direct |
| RS-05 | Pop top only | Push r1,r2 then pop | Remaining route uses only r1 |

## 3.7 Reliability (timeout/retry)

| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| R-01 | No response path | Request unreachable node | 3 retries max, then failure return |
| R-02 | Response on second try | Drop first response in test setup | Success on retry |
| R-03 | Keep processing unrelated packets | Flood unrelated txids during wait | Still returns correct result for matching txid |
| R-04 | Timeout budget | Verify each attempt waits <=5s | Matches RFC retry timing intent |

## 3.8 API method behavior matrix

| ID | Method | Current expected (today) | Final expected |
|---|---|---|---|
| API-01 | `isActive` direct | Works for reachable named node | Works + retry + relay aware |
| API-02 | `isActive` relayed | Should work through stack | Works for 1..N relay hops |
| API-03 | `exists` | HashMap lookup only | Correct network semantics if required by design |
| API-04 | `read` | Throws `Not implemented` | Returns value or `null` |
| API-05 | `write` | Throws `Not implemented` | Returns true/false with protocol codes |
| API-06 | `CAS` | Throws `Not implemented` | Atomic compare-and-swap semantics |

## 3.9 Robustness/adversarial cases

| ID | Scenario | Expected result |
|---|---|---|
| A-01 | Random bytes fuzz 10k packets | No crash, no deadlock |
| A-02 | Oversized UDP packet near max | No crash; reject or process safely |
| A-03 | Malformed CRN string count overflows | Safe rejection |
| A-04 | Invalid UTF-8 in string fields | No crash; safe handling |
| A-05 | Reused txid collision under load | No incorrect cross-response mapping |
| A-06 | Relay map stale entries | No unbounded growth (add cleanup policy) |

## 3.10 Performance sanity

| ID | Scenario | Expected result |
|---|---|---|
| PERF-01 | 1000 `G` requests sequential | Stable latency, no leaks |
| PERF-02 | 1000 mixed packet types | Node remains responsive |
| PERF-03 | 2 relay hops sustained | No significant blocking/backlog |

## 4) Immediate Smoke Commands You Can Run Now

Terminal A:

```bash
cd /Users/taliptun/Desktop/CRN
javac -d out src/*.java
java -cp out PersonalTest
```

Terminal B:

```bash
# GH-01
/Users/taliptun/Desktop/CRN/scripts/send_udp_hex.sh 127.0.0.1 12345 01022047

# P-05 unknown type
/Users/taliptun/Desktop/CRN/scripts/send_udp_hex.sh 127.0.0.1 12345 0102205a

# P-01 short packet
/Users/taliptun/Desktop/CRN/scripts/send_udp_hex.sh 127.0.0.1 12345 0102
```

## 5) What "Professional" Means in Submission

1. Keep test IDs stable (`GH-01`, `V-03`, etc.) so regressions are trackable.
2. Record pass/fail per test ID after each code change.
3. Capture at least one Wireshark trace per message family (`G/H`, `N/O`, `V`).
4. Keep failing tests in the matrix with status `KNOWN-FAIL` until implemented.
5. Re-run full smoke + relay + robustness suite before final zip.

