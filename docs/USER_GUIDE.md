# User and Demonstration Guide

## Before the demonstration

1. Install JDK 21 and Maven 3.9+.
2. Confirm BlueJ 6.0.1 is installed.
3. Build with the correct BlueJ installation path:
   `mvn -Dbluej.installation="C:/Program Files/BlueJ" clean verify`.
4. Confirm the test suite passes and inspect `target/site/jacoco/index.html`.
5. Copy the shaded plugin JAR from `target` into BlueJ's `extensions2` folder.
6. Restart BlueJ.

## Three suggested telemetry demonstrations

### Use case 1 — High activity

1. Open a BlueJ project and create or open a Java class.
2. Type and edit the class repeatedly, making several changes less than 30 seconds apart.
3. Compile the class successfully.
4. Expected telemetry: project/class events, editor typing events, active-time estimates,
   and a successful compilation event.

### Use case 2 — Compilation errors

1. Introduce a deliberate syntax error, such as omitting a semicolon.
2. Compile and observe the compilation failure.
3. Correct the error and compile again.
4. Expected telemetry: failed and successful compilation events, elapsed times,
   error message, error line and source-file metadata when BlueJ supplies them.
   The PDF and JSON reports include an error-frequency breakdown.

### Use case 3 — Execution and idle interval

1. Invoke a method from the BlueJ object bench.
2. Leave the editor unchanged for at least 30 seconds, then make another edit.
3. Invoke the method again, including one invocation that produces an exception if
   appropriate for the demo.
4. Expected telemetry: execution success/failure and method metadata, plus an idle
   interval estimate between editor changes.

## Reports

Reports are written when the extension is terminated. By default they are saved
under the user's home directory in `bluej-telemetry`. Set
`bluej.telemetry.studentId` and `bluej.telemetry.output` as JVM system properties
if different values are required for the demonstration.

## Important interpretation notes

- Editor events count document replacements, not physical keystrokes.
- Active/idle time is estimated from the time between document replacements.
- Execution duration and console output are not available from the invocation-finished
  callback used by this project; the report does not invent those values.
- Method progression is determined by comparing declared method signatures exposed
  by BlueJ when classes are opened or their state changes.
