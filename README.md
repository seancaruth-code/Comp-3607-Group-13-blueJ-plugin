# BlueJ Telemetry Plugin

## Requirements

- Java 21
- Maven 3.9 or newer

## Build and test

```shell
mvn clean test
```

The extension targets Java 21, matching the JDK bundled with BlueJ 6.0.1. The
build reads the BlueJ API from `C:\Program Files\BlueJ\lib\bluej.jar` and does
not package that API into the extension JAR. Override the installation path
when necessary:

```shell
mvn -Dbluej.installation=C:/path/to/BlueJ clean package
```

Install the resulting JAR from `target` in one of these BlueJ `extensions2`
directories:

- BlueJ installation: `lib/extensions2`
- User configuration: `%USERPROFILE%\bluej\extensions2`
- A specific project: `<project>\extensions2`

Restart BlueJ, open a project, compile a class, and invoke a method. The
extension receives project, class, compilation, editor-document, and invocation
callbacks. Editor activity is based on `DocumentListener.textReplaced`: it
counts document replacements, estimates idle intervals using a configurable
30-second threshold, and reports replacement-to-replacement cadence. It does
not claim to observe individual keystrokes, keyboard focus, or mouse activity,
and it never records source contents.

BlueJ 6.0.1 exposes no supported project-created or project-deleted callback;
package-open and package-closing events are the reliable lifecycle boundary.
Invocation-finished events expose metadata and termination type, but no
duration or console-output API, so those values are explicitly unavailable.
