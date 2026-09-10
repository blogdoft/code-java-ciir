# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose

Build a Java application that statically analyzes Java source code (via Maven project files) and
produces a standardized intermediate representation called **CIIR — Code Intelligence Intermediate
Representation**.

CIIR is the core domain artifact of this project: a normalized, tool-agnostic representation of
analyzed Java code, intended to be consumed by downstream code-intelligence tooling. This is the
Java counterpart to the C#/Roslyn generator in the sibling `code-csharp-ciir` repo — same
conceptual contract, own copy of the JSON Schema and specification, adapted where Java's language
model genuinely differs (no properties/events, no MSBuild/NuGet, no Roslyn CFG API).

## Specifications

All project specifications are kept in `.specs/01-spec-inicial.md`. Check there for requirements
and design details before starting new work, and add new specs there rather than elsewhere.

## Architecture: Hexagonal (Ports & Adapters)

The application core owns the domain logic of analyzing Java source and producing CIIR. It has no
dependency on any specific delivery mechanism (CLI, HTTP, etc.) or specific analysis technology —
those live in adapters around the core, connected through ports (interfaces) defined by the core.
Dependency direction is enforced both by convention and by an ArchUnit architecture-boundary test
(`ciir-application`'s `ArchitectureBoundaryTest`) that fails the build if `ciir-core`/
`ciir-application` ever import `com.github.javaparser.*`.

| Module | Role |
|---|---|
| `ciir-core` | The CIIR model itself (records, enums, identity hashing, `embeddingText` generation). No dependency on JavaParser, the CLI, or any serialization technology. |
| `ciir-application` | Ports (`CodeAnalyzer`, `CiirWriter`, `InputResolver`, ...) and the main use case (`AnalyzeInputHandler`), plus Maven module discovery/POM inspection. Depends only on `ciir-core`. |
| `ciir-java` | The only module allowed to depend on `com.github.javaparser.*`. Implements `CodeAnalyzer` using JavaParser + Symbol Solver (syntax tree + resolved symbols) driven by each Maven module's own dependency classpath. |
| `ciir-serialization` | JSONL writer, `manifest.json`/`analysis-report.json` writer, and the packaged `ciir.schema.json` resource. No dependency on JavaParser. |
| `ciir-cli` | The composition root and command-line adapter (`ciir-cli/src/main/java/dev/ftathiago/ciir/cli/composition/Composition.java`, picocli). Contains no analysis logic — parses arguments, wires the concrete adapters, calls into `ciir-application`. |

- **Driving ports/adapters** (things that trigger CIIR generation): the **CLI** is built today. A
  **Web API (HTTP)** driving adapter is planned — when adding it, reuse `AnalyzeInputHandler`
  rather than duplicating analysis logic in the API layer.
- **Driven ports/adapters** (things the core depends on, e.g. writing CIIR output): kept behind
  interfaces defined in `ciir-application` so they can be swapped without touching domain logic.

When implementing new functionality, default to: define/extend a port (interface) in
`ciir-application`, implement the actual behavior in an adapter (`ciir-java` for analysis logic,
`ciir-serialization` for output formats), and keep adapters thin. A new source-language generator
follows the same pattern: implement `CodeAnalyzer` in a new adapter module analogous to
`ciir-java`, and register it in `Composition` — no other module needs to change.

See [`README.md`](README.md) for the full module/test breakdown, CLI usage/options, and generated
output format; [`.specs/01-spec-inicial.md`](.specs/01-spec-inicial.md) for what the CIIR
concepts mean; and [`schemas/ciir.schema.json`](schemas/ciir.schema.json) for the formal contract.

## Tech stack

- **Java 25** (SDKMAN!), **Maven** (SDKMAN!)
- **JUnit 5** for tests, with **AssertJ** (assertions), **Mockito** (mocking) and **ArchUnit**
  (the architecture-boundary test)
- **JavaParser + Symbol Solver** (`com.github.javaparser:javaparser-symbol-solver-core`) — the only
  analysis technology, confined to `ciir-java`
- **Jackson** for all JSON serialization (`ciir-serialization`)
- **picocli** for the CLI
- **networknt/json-schema-validator** for schema-conformance tests
- Only free/open-source libraries are allowed
- Always use the latest version of a library compatible with Java 25 — check Maven Central for
  updates rather than pinning to whatever version was scaffolded originally

## Known environment quirk

Mockito's bundled Byte Buddy needs `-Dnet.bytebuddy.experimental=true` (set via `argLine` in the
root `pom.xml`'s `maven-surefire-plugin` config) until it officially recognizes the Java 25 class
file version. Don't remove this without checking whether Mockito/Byte Buddy has caught up.

## `ciir-java` Symbol Solver gotchas

Non-obvious JavaParser + Symbol Solver failure modes hit (and fixed) while building the analyzer —
easy to silently reintroduce in a refactor of `JavaCodeAnalyzer`/`ResolutionClassifier`:

- `new ReflectionTypeSolver()` (no-arg) defaults to a JRE-only filter that only recognizes
  `java.*`-prefixed classes — it silently fails to resolve plenty of legitimate JDK classes outside
  that prefix (`org.w3c.dom.*`, `org.xml.sax.*`, `javax.*`), even though `Class.forName` finds them
  fine. Always construct it as `new ReflectionTypeSolver(false)`.
- Every `JavaParserTypeSolver` added to the combined solver must be constructed with the exact same
  `ParserConfiguration` instance that later gets `setSymbolResolver(...)` called on it (build the
  config first, pass it to each `JavaParserTypeSolver`, *then* mutate it with the resolver once the
  `CombinedTypeSolver` exists). A `JavaParserTypeSolver` parses files it discovers with its own
  internal `JavaParser`; without this shared, later-mutated config, resolving into another module's
  source (e.g. a record accessor) fails with `IllegalStateException: Symbol resolution not
  configured`.
- A `TypeSolver` (including a `CombinedTypeSolver`) may only ever belong to **one** parent
  `CombinedTypeSolver` for its whole lifetime (`add()` throws `IllegalStateException` on a second
  attempt) — solvers cannot be cached/reused across `analyze()` calls for different projects in the
  same run. Build a fresh `CombinedTypeSolver` (and fresh child solvers) per call; only the
  (pure-data) `sourceRootToProjectName` map is safe to compute once and reuse.
- JavaParser resolves lazily: a declaration's own `.resolve()` can succeed while a later step
  (e.g. `getReturnType()`) still throws. Wrap each entity's *entire* build-and-emit block in
  `CompilationUnitWalker` in one `try/catch (RuntimeException)` — not just the initial `.resolve()`
  call — so one bad symbol only skips that entity, never the whole file/project.

## Code quality

- **Zero warnings**: the build must be warning-free. Treat any compiler or analyzer warning as
  something to fix, not ignore.
- **Formatting is enforced, not just suggested**: `spotless-maven-plugin` (Google Java Style via
  `googleJavaFormat`, 2-space indent — see `.editorconfig`) runs `check` in the `validate` phase of
  every build, so an unformatted file fails `mvn test`/`package`/`verify` before compilation even
  starts. Run `mvn spotless:apply` to fix violations (never hand-format to a different style). The
  pre-commit hook in `.githooks/pre-commit` (enable once per clone with
  `git config core.hooksPath .githooks`) does this automatically and re-stages the result.
- A resolution failure (`UnsolvedSymbolException` or similar) in `ciir-java` must never be silently
  swallowed as a fabricated fact — it becomes an explicit `unresolved` relation (for explicit
  invocations/type references) or is skipped with a debug/warn log line (for the much noisier
  plain-name read/write scan, where most failures are ordinary local variables/parameters, not
  errors). Never guess a relation target without static evidence.

## Git commits

Always use [Conventional Commits](https://www.conventionalcommits.org/) (semantic commits):
`<type>(<optional scope>): <description>`, e.g. `fix(relations): resolve same-run cross-module
relations`. Common types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`, `perf`. Use a
`BREAKING CHANGE:` footer (or `!` after the type/scope) for any backward-incompatible change to the
CIIR schema/model or CLI behavior.

## Development commands

```bash
# Build (requires Java 25 + Maven; warning-free build enforced solution-wide)
mvn -q -DskipTests package

# Run all unit + integration tests
mvn test

# Also run the CLI black-box subprocess tests (needs `package` to have run first; bound to
# integration-test phase for exactly that reason)
mvn verify

# Run a single test
mvn test -pl <module> -Dtest=ClassName

# Fix formatting violations (Google Java Style via Spotless) after editing code
mvn spotless:apply

# Run the CLI against a leaf pom.xml, an aggregator pom.xml, or a directory
./bin/ciir <path> [--output <path>] [--verbose] [--include-source] [--fail-on-error]
```

Each module keeps its own tests in `src/test/java` (idiomatic Maven — the C# repo's separate test
*projects* have no direct Maven analogue and would just add reactor boilerplate).
`fixtures/basic-project` and `fixtures/multiple-projects` are the sample Maven projects
`ciir-java`'s `EndToEndIntegrationTest` analyzes — reuse them for new test scenarios rather than
adding new fixture projects, unless a genuinely new scenario (e.g. generics-heavy resolution,
annotations) isn't covered by either.
