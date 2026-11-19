# User and Demonstration Guide

## Before the demonstration

1. Install JDK 21 and Maven 3.9+.
2. Confirm BlueJ 6.0.1 is installed.
3. Build with the correct BlueJ installation path. This path must contain
   `lib/bluej.jar` and the JavaFX JARs used by the build:
   `mvn -Dbluej.installation="C:/Program Files/BlueJ" clean verify`.
4. Confirm the test suite passes and inspect `target/site/jacoco/index.html`.
5. Copy the shaded plugin JAR from `target` into BlueJ's `extensions2` folder.
6. Restart BlueJ.

The plugin JAR contains the application dependencies such as PDFBox, but the
BlueJ API and JavaFX libraries remain provided by the local BlueJ installation.

## Three suggested telemetry demonstrations

### Use case 1 — High activity

1. Open a BlueJ project and create or open a Java class.
2. Type and edit the class repeatedly, making several document changes less than
   30 seconds apart.
3. Compile the class successfully.
4. Expected telemetry: project/class events, editor replacement events,
   active-time estimates, edit counts, and a successful compilation event.

### Use case 2 — Compilation errors

1. Introduce a deliberate syntax error, such as omitting a semicolon.
2. Compile and observe the compilation failure.
3. Correct the error and compile again.
4. Expected telemetry: failed and successful compilation events, elapsed times,
   error message, error line and source-file metadata when BlueJ supplies them.
   The PDF and JSON reports include compilation totals, success/error ratios,
   average compilation time when available, and an error-frequency breakdown.

### Use case 3 — Execution and idle interval

1. Invoke a method from the BlueJ object bench.
2. Leave the editor unchanged for at least 30 seconds, then make another edit.
3. Invoke the method again, including one invocation that produces an exception if
   appropriate for the demo.
4. Expected telemetry: execution success/failure and method metadata, method
   signature timeline entries, plus an idle interval estimate between editor
   changes.

## Reports

### Automatic reports

Reports are written when the extension is terminated, provided at least one
event was recorded. By default they are saved under the user's home directory
in `bluej-telemetry`, using timestamped names such as
`telemetry-My_Project-20261118-194241-123.json` and `.pdf`.

Set `bluej.telemetry.output` as a JVM system property if a different output
directory is required for the demonstration. The directory is created when
needed.

### Manual reports

While a BlueJ project is open, choose **Tools > Telemetry Reports** and select
**Export JSON Report** or **Export PDF Report**. Choose a destination in the
file chooser. The extension appends the matching file extension when necessary
and reports success or failure in a dialog.

## Important interpretation notes

- Editor events count document replacements and edit counts, not physical
  keystrokes.
- Active/idle time is estimated from the time between document replacements.
- The default idle threshold is 30 seconds.
- Reports include event counts, compilation and execution summaries, error
  frequencies, class lifecycle timelines, and method-signature timelines.
- Execution duration and console output are not available from the invocation-finished
  callback used by this project; the report does not invent those values.
- Method progression is determined by comparing declared method signatures exposed
  by BlueJ when classes are opened or their state changes.
- Source code contents, mouse activity, keyboard focus, and individual keystrokes
  are not recorded.

## Troubleshooting

- **Maven cannot resolve BlueJ or JavaFX:** pass the installation directory
  explicitly with `-Dbluej.installation="C:/path/to/BlueJ"` and confirm the
  expected JARs exist under its `lib` directory.
- **No automatic reports appear:** close BlueJ normally, confirm that events
  were recorded, and check the directory configured by `bluej.telemetry.output`
  or the default `<user-home>/bluej-telemetry` directory.
- **Manual export fails:** choose a writable destination and check the error
  dialog. The report is generated from the current in-memory session snapshot.
