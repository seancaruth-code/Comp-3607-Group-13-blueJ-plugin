# BlueJ Telemetry Plugin — COMP 3607 Group 13

A BlueJ extension that records project, class, compilation, editor-document,
method-signature, and method-invocation activity. It produces timestamped JSON
and PDF session reports and also supports manual JSON/PDF exports from the
BlueJ Tools menu.

## Requirements

- BlueJ 6.0.1 (provides the BlueJ Extensions API)
- JDK 21
- Maven 3.9+
- Internet access on the first Maven build so PDFBox and JUnit can be downloaded

## Build and test

The BlueJ API and JavaFX runtime libraries are supplied by the local BlueJ
installation and are not bundled in the plugin JAR. The default installation
location is `C:/Program Files/BlueJ`. Override it when BlueJ is installed
elsewhere:

```shell
mvn -Dbluej.installation="D:/Apps/BlueJ" clean verify
```

On Windows, if Maven is on your PATH:

```powershell
mvn -Dbluej.installation="C:/Program Files/BlueJ" clean verify
```

Run only the tests:

```shell
mvn test
```

The automated tests cover event models, event factories, observer notification,
export strategies, editor interval calculations, session statistics, and PDF/JSON
report generation. `mvn verify` also creates the JaCoCo report under
`target/site/jacoco/`.

A successful Maven build still needs to be verified against the BlueJ
installation used for the final demonstration.

## Install in BlueJ

1. Run `mvn package`.
2. Copy `target/bluej-telemetry-plugin-1.0.0.jar` into BlueJ's `lib/extensions2`
   directory or the user's BlueJ `extensions2` directory (`%USERPROFILE%\\bluej\\extensions2` on Windows).
3. Restart BlueJ.
4. Open a project and use the editor, compiler, and object bench normally.
5. Use **Tools > Telemetry Reports > Export JSON Report** or **Export PDF
   Report** to save a report at any time.
6. Close BlueJ normally so the extension can write the final session reports.

## Session reports

When the extension terminates, it writes one timestamped `.json` file and one
`.pdf` report if the session contains events. The default output directory is:

```text
<user-home>/bluej-telemetry
```

Automatic files use names similar to:

```text
telemetry-My_Project-20261118-194241-123.json
telemetry-My_Project-20261118-194241-123.pdf
```

Set this optional JVM system property to change the automatic-report directory
and the initial directory shown by the manual export file chooser:

- `bluej.telemetry.output` — output directory for generated reports.

The JSON contains session metadata, event counts, compilation and execution
summaries, active/idle estimates, error frequencies, class and method timelines,
and the complete event list. The PDF presents the same key metrics in a readable
multi-page report. Manual exports ensure the selected `.json` or `.pdf`
extension and display a success or error dialog.

## Design patterns

### Observer

`ObserverSubject` maintains observers and broadcasts telemetry events.
`ObserverManager` stores the event history, while `CompilationStatistics`
aggregates compilation outcomes. The subject takes a snapshot of its observer
list before notifying it, so observers can be changed safely during notification.
A failing observer is logged and does not prevent other observers receiving the event.

### Factory Method

`TelemetryEventFactory` defines the event-creation contract. Concrete factories
such as `CompilationEventFactory`, `ClassEventFactory`, and
`ExecutionEventFactory` construct their corresponding event types. This keeps
event construction and validation out of the BlueJ callback methods.

### Strategy

`ExportStrategy` defines a common export contract. `JsonExportStrategy`,
`CsvExportStrategy`, and `TextExportStrategy` provide interchangeable formats.
`EventExporter` delegates to the selected strategy and allows it to be changed
at runtime.

No Singleton pattern is used.

## Telemetry scope and limitations

- Project events are based on BlueJ package-open and package-closing callbacks.
- Class events are collected from package opening and BlueJ class lifecycle callbacks.
- Compilation events include success/failure, elapsed time, error text, error line,
  source-file names, and whether compilation was user-generated when the API supplies it.
- Execution events include the invoked class/method, parameters, termination type,
  success/failure and exception message when exposed by BlueJ.
- Editor metrics use document replacement callbacks and a background editor scan.
  The extension does not record source code, individual physical keystrokes,
  mouse activity, or keyboard focus. Active/idle time is an estimate based on
  the interval between document changes and the default 30-second threshold.
- The BlueJ API used by this project does not expose a reliable console-output
  summary or execution duration in the invocation-finished callback. Those values
  are therefore left unavailable rather than fabricated.
- Method progression is captured by comparing the declared method signatures
  exposed by BlueJ when a class is opened or its state changes. Added, removed and
  initially discovered methods are included in the JSON and PDF timelines.
- Reports are generated asynchronously. If BlueJ is terminated abruptly, the
  final automatic reports may not be written.

## Repository structure

```text
src/main/java/com/comp3607/telemetry/
  bluej/       BlueJ extension integration
  editor/      Editor activity interval tracking
  events/      Immutable telemetry event models
  factory/     Factory Method implementations
  observer/    Observer interfaces and event distribution
  report/      Session snapshot, statistics, JSON and PDF reports
  statistics/  Live compilation statistics
  strategy/    JSON, CSV and text export strategies
src/test/java/  JUnit 5 unit and integration tests
```

## Privacy

The plugin records event metadata and editor activity measurements, not source
code contents. Generated reports are written locally to the configured output
directory.

## Additional documentation

- [Design patterns, class diagram and SOLID mapping](docs/DESIGN.md)
- [Setup, report usage and three demo use cases](docs/USER_GUIDE.md)
