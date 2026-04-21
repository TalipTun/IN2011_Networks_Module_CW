Build Instructions
==================

Prerequisites
-------------
- Java JDK installed (Java 17+ recommended)
- VS Code with "Extension Pack for Java" (optional but recommended)

Project Layout
--------------
- Source files are in `src/`
- Compiled class files are generated into `out/`
- VS Code workspace settings are in `.vscode/`

Compile and Run (Terminal)
--------------------------
From the project root:

    javac -d out src/*.java
    java -cp out LocalTest 2

Command notes:
- `-d out` tells `javac` to place compiled `.class` files in `out/`
- `-cp out` tells `java` where to find compiled classes
- `2` is the number of nodes passed to `LocalTest` (valid range is 2 to 10)

Run in VS Code
--------------
1. Open this folder in VS Code.
2. Use `Terminal -> Run Build Task` and run `Java: Compile`.
3. Use `Terminal -> Run Task` and run `Java: Run LocalTest`.

You can also open `src/LocalTest.java` and run it directly from VS Code.

Manual UDP Testing (Current Progress)
-------------------------------------
Use this to test protocol handling without relying on `LocalTest` end-to-end.

1. Start the receiver node in one terminal:

    javac -d out src/*.java
    java -cp out PersonalTest

`PersonalTest` opens port `12345` and calls `handleIncomingMessages(...)`.

2. In a second terminal, send test packets with `nc`.

Name request (`G`) test:

    printf '\x01\x02 G' | nc -u -w1 127.0.0.1 12345

Expected response includes `H0 N:test0` (same transaction ID, type `H`, encoded node name).

Nearest request (`N`) test:

    printf '\x01\x03 N0 c22e1d650c0b6ff53d9f72bc5dbeb06e07dadba6dde7ae554fe5904cad31a518 ' | nc -u -w1 127.0.0.1 12345

Current expected response starts with `O`. If no known nodes are populated yet, response may contain no node pairs.

Relay (`V`) test:

    printf '\x01\x05 V0 N:test0 \x0A\x0B G' | nc -u -w1 127.0.0.1 12345

Current debug output in `Node.handleIncomingMessages(...)` should show:
- parsed relay target (`N:test0`)
- embedded message length/type
- forwarding destination

3. Optional raw-byte inspection:

    printf '\x01\x03 N0 c22e1d650c0b6ff53d9f72bc5dbeb06e07dadba6dde7ae554fe5904cad31a518 ' | nc -u -w1 127.0.0.1 12345 | xxd -g1


Working Functionality
=====================

`LocalTest` currently starts but will fail with "Not implemented" until methods in
`src/Node.java` are implemented.
