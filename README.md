# CIIR — Code Intelligence Intermediate Representation (Java)

A Java application that statically analyzes Java source code (via Maven project files) and
produces **CIIR (Code Intelligence Intermediate Representation)**: a normalized,
language-independent representation of analyzed code, intended as the input contract for
downstream code-intelligence tooling — embedding generation, dependency/call graphs,
architectural analysis, impact analysis, and documentation generation.

This is the Java counterpart to the C#/Roslyn generator in the sibling `code-csharp-ciir` repo.
Both implement the same conceptual CIIR contract; this repo maintains its own copy of the JSON
Schema (`schemas/ciir.schema.json`) with a Java-specific vocabulary (accessibility, modifiers,
`typeKind`) rather than sharing a file between the two.

See [`.specs/01-spec-inicial.md`](.specs/01-spec-inicial.md) for the full requirements and for what
the CIIR concepts mean, and [`schemas/ciir.schema.json`](schemas/ciir.schema.json) for the formal
contract of what a valid CIIR document looks like.

## Architecture

Hexagonal (ports & adapters). The core owns the domain logic ("analyze source → produce CIIR")
independent of any delivery mechanism or analysis technology:

```
             ENTRY POINTS
                  |
         +--------+--------+
         |                 |
        CLI             future API
         |                 |
         +--------+--------+
                  v
             Application
                  |
        +---------+----------+
        v                    v
   Analyzer Contract       Writers
        ^
        |
  Java / JavaParser
```

| Module | Role |
|---|---|
| `ciir-core` | The CIIR model itself (records, enums, identity hashing, `embeddingText` generation). No dependency on JavaParser, the CLI, or any serialization technology. |
| `ciir-application` | Ports (`CodeAnalyzer`, `CiirWriter`, `InputResolver`, ...) and the main use case (`AnalyzeInputHandler`), plus Maven module discovery (`ProjectDiscoveryService`, `PomInspector` — plain XML reading, no build/dependency resolution). Depends only on `ciir-core`. |
| `ciir-java` | The only module allowed to depend on `com.github.javaparser.*`. Implements `CodeAnalyzer` using JavaParser + Symbol Solver, driven by each Maven module's own resolved dependency classpath (`mvn dependency:build-classpath`). |
| `ciir-serialization` | JSONL writer, `manifest.json`/`analysis-report.json` writer, and the packaged `ciir.schema.json` resource. No dependency on JavaParser. |
| `ciir-cli` | The composition root and command-line adapter (picocli). Contains no analysis logic — parses arguments, wires the concrete adapters, and calls into `ciir-application`. |

Dependency direction is enforced by convention and by an ArchUnit architecture-boundary test in
`ciir-application` (`ArchitectureBoundaryTest`) that fails the build if `ciir-core`/`ciir-application`
ever import `com.github.javaparser.*`.

## Installation

Requires **Java 25** and **Maven**. Both are installed via [SDKMAN!](https://sdkman.io/):

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 25-tem
sdk install maven
```

## Build

```bash
mvn -q -DskipTests package   # compile + produce ciir-cli/target/ciir-cli.jar
mvn test                     # unit + integration tests (schema conformance, real analyzer pipeline)
mvn verify                   # also runs the CLI black-box subprocess tests (ciir-cli, integration-test phase)
```

The build is warning-free.

## Testing

```bash
mvn test
mvn test -pl ciir-core -Dtest=CiirIdentityTest       # run a single test class
```

Tests mirror `src/main` structurally within each module (idiomatic Maven — unlike the C# repo's
separate test *projects*, everything lives in each module's own `src/test/java`):

- `ciir-core` — identity hashing, modifier canonical ordering, `embeddingText` generation.
- `ciir-application` — input resolution, Maven module discovery/deduplication, orchestration
  (`AnalyzeInputHandler`) with mocked ports, and the ArchUnit architecture-boundary test.
- `ciir-java` — unit tests against hand-built `CompilationUnit`s (conditions, control flow) and a
  real end-to-end integration test (`EndToEndIntegrationTest`) running the actual analyzer +
  Maven classpath resolution against `fixtures/`, with full JSON Schema validation and a
  byte-for-byte determinism check.
- `ciir-serialization` — JSONL wire-format shape (camelCase, lowercase enum tokens, empty-collection
  omission) and schema-conformance tests (one valid sample per emitted `kind`, plus deliberately
  invalid samples).
- `ciir-cli` — `CliExitCodeIT` spawns the real built jar as an OS subprocess and asserts exit codes
  (runs in the `integration-test` phase, since it needs `package` to have already produced the jar).

## Usage

```bash
./bin/ciir <path> [--output <path>] [--verbose] [--no-progress] [--include-source] [--fail-on-error]
```

or directly:

```bash
java -jar ciir-cli/target/ciir-cli.jar <path> [options]
```

`<path>` may be:

- a leaf module `pom.xml` — that module is analyzed;
- an **aggregator** `pom.xml` (one declaring `<modules>`, the role a C# `.sln` plays) — every
  module it transitively references is analyzed;
- a directory — recursively scanned for `pom.xml` files (skipping `target/`, `.git/`, `.idea/`,
  `.settings/`); a leaf module already referenced by a discovered aggregator is never analyzed
  twice just because its `pom.xml` was also found while scanning.

| Option | Meaning |
|---|---|
| `--output <path>` | Output directory (default: `./ciir-output`). |
| `--verbose` | Verbose diagnostic logging. |
| `--no-progress` | Suppress progress reporting. |
| `--include-source` | Embed the literal source text of each entity's declaration. |
| `--fail-on-error` | Exit with a non-zero code if any module fails to load/analyze. |

Exit codes: `0` success, `1` a module failed under `--fail-on-error`, `2` invalid arguments/input,
`3` writing the output failed.

Example:

```bash
./bin/ciir ./fixtures/multiple-projects --output ./artifacts/ciir --verbose
```

## Generated files

Every run produces, under `--output`:

```
ciir-output/
  ciir.jsonl              # one CIIR record per line (JSON Lines — never a JSON array)
  ciir.schema.json         # the CIIR v1 contract, copied verbatim from this repository
  manifest.json            # generator/input/project/file metadata + aggregate statistics
  analysis-report.json     # operational outcome: project/document/relation counts, errors
```

### `ciir.jsonl` example

```json
{"schemaVersion":"1.0","id":"sha256:...","kind":"method","language":"java","project":"payment-service","symbol":{"name":"authorize","qualifiedName":"dev.ftathiago.payments.application.PaymentService.authorize","canonicalName":"dev.ftathiago.payments.application.PaymentService.authorize(dev.ftathiago.payments.domain.Order)","container":"dev.ftathiago.payments.application.PaymentService"},"method":{"accessibility":"public","parameters":[{"name":"order","type":"dev.ftathiago.payments.domain.Order"}],"returnType":"dev.ftathiago.payments.domain.PaymentResult"},"relations":[{"kind":"calls","target":{"symbol":"dev.ftathiago.payments.domain.PaymentGateway.authorize"},"resolution":{"status":"resolved","origin":"project"}}],"embeddingText":"Entity: method\nQualified name: dev.ftathiago.payments.application.PaymentService.authorize\n...","embeddingTextStrategy":"semantic-v1","embeddingTextHash":"sha256:..."}
```

Properties with no content are omitted rather than serialized as empty structures.

### Validating `ciir.jsonl` against the schema

Any JSON Schema (2020-12) validator works, in any language. With Python's `jsonschema`:

```python
import json, jsonschema
schema = json.load(open("ciir-output/ciir.schema.json"))
with open("ciir-output/ciir.jsonl") as f:
    for line in f:
        jsonschema.validate(json.loads(line), schema)
```

The same validation runs as part of this repository's own test suite
(`ciir-serialization`'s `SchemaConformanceTest`, `ciir-java`'s `EndToEndIntegrationTest`).

## Versioning

`schemaVersion` follows semantic-ish rules: backward-compatible additions bump the minor version
(`1.0` → `1.1`); incompatible changes (removing/renaming a property, changing its meaning) require
a major version bump (`2.0`). The semantics of an existing property are never changed silently.

## Building a new language generator

The CIIR contract (`schemas/ciir.schema.json` + `.specs/01-spec-inicial.md`) is designed to be
implemented independently of this Java generator (and of the C# one). A new generator needs to:

1. Produce documents that validate against `schemas/ciir.schema.json`.
2. Compute the deterministic `id` the same way: `sha256(<language>|<project>|<kind>|<canonicalSymbolIdentity>)`
   (see `dev.ftathiago.ciir.core.identity.CiirIdentity` for the reference implementation).
3. Follow the semantic rules in `.specs/01-spec-inicial.md` — in particular, resolve relations
   from real semantic information (never text/regex-based inference), never fabricate a relation
   without static evidence, and keep `embeddingText` a filtered projection rather than a full copy
   of the document.

Within this repository, the pattern to follow is `ciir-application`'s `CodeAnalyzer` port:
implement it in a new adapter module (analogous to `ciir-java`), and register it in
`ciir-cli/src/main/java/dev/ftathiago/ciir/cli/composition/Composition.java` — no other module
needs to change.

## Known limitations

- Control-flow metrics (`controlFlow.cyclomaticComplexity`/`basicBlockCount`) are computed by
  direct AST decision-point counting, not a real control-flow graph — JavaParser has no API
  analogous to Roslyn's `FlowAnalysis.ControlFlowGraph`. See "Control flow" in
  [`.specs/01-spec-inicial.md`](.specs/01-spec-inicial.md).
- Field/property initializer expressions are not analyzed for relations.
- Static initializer blocks (`static { ... }`) and enum constants are not represented as their own
  documents.
- No interprocedural or cross-project data-flow analysis; cross-module relations within the same
  run *are* represented (`resolution.origin: "solution"`), but nothing beyond static,
  single-expression resolution.
- Dependency classpath resolution shells out to `mvn dependency:build-classpath`, so it requires a
  working `mvn` on `PATH` and, for not-yet-resolved dependencies, network access to populate
  `~/.m2` — including for inter-module dependencies in a reactor that hasn't been `mvn install`ed
  yet (see `EndToEndIntegrationTest`'s fixture setup for exactly this case).
- Out of scope for this phase entirely (see `.specs/01-spec-inicial.md`): a database backend, real
  embeddings/LLM calls, a REST API, a graph database, and analyzers for languages other than Java.
