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


Working Functionality
=====================

`LocalTest` currently starts but will fail with "Not implemented" until methods in
`src/Node.java` are implemented.

