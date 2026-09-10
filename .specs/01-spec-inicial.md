# Java Code Intelligence IR Generator

> **Sobre este documento**: este arquivo consolida, num único lugar, tanto os requisitos de
> implementação do gerador Java (o "como construir") quanto a explicação semântica do contrato
> CIIR (o "o que significa cada conceito") — esta segunda parte era originalmente mantida em
> `docs/ciir-specification.md`, um documento separado voltado a quem for implementar um gerador
> para outra linguagem, mas foi incorporada aqui para manter toda a especificação do projeto
> apenas em `.specs/`. O JSON Schema (`schemas/ciir.schema.json`) continua sendo a fonte de
> verdade sobre "o que é uma estrutura JSON válida"; este documento explica "o que essa estrutura
> significa".

## 1. Objetivo

Criar uma aplicação Java capaz de analisar estaticamente código-fonte Java e gerar uma representação intermediária padronizada denominada **Code Intelligence Intermediate Representation — CIIR**.

O CIIR deverá ser independente da linguagem de programação e servirá como contrato intermediário para futuros processos, incluindo:

* geração de embeddings;
* indexação vetorial;
* construção de grafos de dependência;
* construção de call graphs;
* graph expansion;
* descoberta de fluxos de execução;
* análise arquitetural;
* análise de impacto;
* investigação de código;
* extração futura de regras de negócio;
* geração de documentação;
* construção de ferramentas de Code Intelligence.

Esta implementação é a segunda deste contrato (a primeira analisa C# utilizando Roslyn, em `code-csharp-ciir`) e deverá analisar **Java utilizando JavaParser + Symbol Solver**, mas a arquitetura não poderá depender de Java fora do projeto responsável especificamente pela análise Java.

O CIIR é um contrato independente do analisador — este repositório mantém sua própria cópia de `schemas/ciir.schema.json`, adaptada ao vocabulário Java (accessibility/modifiers/typeKind específicos), e não compartilha esse arquivo com `code-csharp-ciir`.

```text
C# / Roslyn ─────────────┐
Java / JavaParser ───────┤
TypeScript ──────────────┤
Python ──────────────────┤
SQL ─────────────────────┤
                         ▼
                       CIIR
                         │
              ┌──────────┼──────────┐
              ▼          ▼          ▼
          Embeddings    Graph      Analysis
```

### Definição formal

> Code Intelligence Intermediate Representation (CIIR) is a language-independent intermediate representation designed to describe statically observable software entities, their semantic properties, source evidence, relationships, documentation and selected control-flow characteristics.
>
> Language-specific analyzers translate native syntax and semantic models into CIIR.
>
> CIIR serves as a stable interchange format for downstream code-intelligence systems including search, embedding generation, dependency graphs, execution-flow analysis and software comprehension.
>
> CIIR represents observable facts and SHALL avoid presenting probabilistic or AI-generated interpretations as deterministic program facts.

---

# 2. Princípios arquiteturais

A implementação deverá seguir:

* SOLID;
* DRY;
* KISS;
* YAGNI;
* Separation of Concerns;
* Dependency Inversion;
* Composition over inheritance;
* baixo acoplamento;
* alta coesão;
* programação orientada a contratos;
* testabilidade;
* processamento incremental/streaming sempre que possível.

Evitar abstrações sem necessidade concreta. Não criar interfaces apenas para satisfazer artificialmente padrões de arquitetura. Interfaces deverão existir principalmente quando:

* houver uma fronteira arquitetural;
* houver necessidade real de substituição;
* houver mais de uma implementação plausível;
* forem necessárias para Dependency Inversion;
* facilitarem testes de componentes externos.

O código deverá seguir recomendações de qualidade equivalentes ao Sonar, incluindo: nenhum código morto, nenhum warning relevante, métodos pequenos e coesos, complexidade cognitiva controlada, tratamento correto de recursos (`try-with-resources`/`AutoCloseable`), tratamento apropriado de exceções, ausência de duplicação significativa, ausência de secrets hardcoded, uso apropriado de mecanismos de cancelamento cooperativo, argumentos públicos validados, e Javadoc para APIs públicas relevantes.

---

# 3. Requisito fundamental de desacoplamento

O CLI é **apenas uma forma de iniciar uma análise**. Nenhuma regra de análise deverá existir no módulo CLI. A aplicação deverá ser organizada para permitir no futuro:

```text
CLI ───────────┐
REST API ──────┤
Worker ────────┤
Queue Consumer ┤
Kubernetes Job ┤
Git Webhook ───┤
               ▼
       Analysis Application
               │
               ▼
           CIIR Engine
```

O componente principal deverá poder ser chamado programaticamente sem qualquer dependência do CLI.

---

# 4. Estrutura inicial do reactor Maven

Criar um reactor Maven multi-módulo semelhante a:

```text
ciir-core/
ciir-application/
ciir-java/
ciir-serialization/
ciir-cli/

fixtures/
schemas/
```

Diferente do repositório C# (que usa projetos de teste separados por convenção .NET), cada módulo mantém seus testes em `src/test/java`, dentro do próprio módulo — separar testes em módulos Maven adicionais não traria benefício arquitetural e apenas aumentaria o boilerplate do reactor. A separação arquitetural entre os cinco módulos principais é preservada integralmente.

---

# 5. Responsabilidade dos módulos

## 5.1 `ciir-core`

Não deverá possuir dependência de JavaParser, CLI, filesystem concreto ou infraestrutura. Responsável por definir o modelo CIIR: entidades, value objects, enums, contratos principais, regras de identidade, tipos para source locations, relações, documentação, control flow metadata, condições, metadados, constantes da especificação.

Exemplos (como `record`s Java): `CiirDocument`, `CiirSymbol`, `CiirSourceLocation`, `CiirRelation`, `CiirRelationTarget`, `CiirDocumentation`, `CiirParameter`, `CiirCondition`, `CiirControlFlow`, `CiirComment`.

## 5.2 `ciir-application`

Responsável por orchestration e casos de uso. Exemplos: `AnalyzeInputCommand`, `AnalyzeInputHandler`, `InputResolver`, `CodeAnalyzer`, `CiirWriter`, `AnalysisReporter`.

Esse módulo deverá decidir: o que deve ser analisado; em que ordem; como múltiplos módulos Maven são processados; quando iniciar e finalizar writers; geração do relatório final; propagação de cancelamento cooperativo. Não deverá conhecer detalhes de JavaParser.

## 5.3 `ciir-java`

Responsável exclusivamente pela implementação de análise Java. Deverá utilizar JavaParser + Symbol Solver (`com.github.javaparser:javaparser-symbol-solver-core`).

Responsabilidades:

* parsear compilation units (`CompilationUnit`);
* construir e configurar o symbol solver combinado (`ReflectionTypeSolver` + `JavaParserTypeSolver` por módulo + `JarTypeSolver` por dependência resolvida);
* resolver declarações e símbolos;
* extrair Javadoc;
* extrair relações;
* extrair condições;
* calcular métricas básicas de control flow (aproximação por contagem de nós de decisão — ver §35);
* produzir objetos CIIR.

Somente esse módulo deverá conhecer tipos de `com.github.javaparser.*` (`CompilationUnit`, `ResolvedType`, `SymbolReference`, `TypeSolver`, etc.). Nenhum desses tipos poderá aparecer nas APIs públicas do Core ou Application. Esse limite é verificado por um teste de arquitetura (ArchUnit) em `ciir-application`.

## 5.4 `ciir-serialization`

Responsável por: serialização JSON (Jackson); escrita JSONL; manifest; cópia do JSON Schema; hashing relacionado ao artifact; compatibilidade do formato. Não deverá depender de JavaParser.

## 5.5 `ciir-cli`

Responsável exclusivamente pela interface de linha de comando (picocli). Deverá: interpretar argumentos; validar argumentos básicos; construir manualmente as dependências (composition root, sem container de DI); chamar `ciir-application`; apresentar progresso; apresentar erros; retornar exit codes adequados.

Não deverá: abrir `pom.xml` diretamente para fins de análise semântica; utilizar JavaParser; percorrer AST; gerar diretamente CIIR; implementar regras semânticas; montar `embeddingText`.

---

# 6. Uso do CLI

A interface principal deverá ser:

```bash
ciir <path>
```

Exemplos:

```bash
ciir ./pom.xml
ciir ./services/billing/pom.xml
ciir ./src
```

Opções mínimas:

```text
--output <path>
--verbose
--no-progress
--include-source
--fail-on-error
```

Exemplo:

```bash
ciir ./src \
  --output ./artifacts/ciir \
  --verbose
```

O nome final do executável é `ciir`: um script wrapper em `bin/ciir` invoca o uber-jar produzido por `ciir-cli` (`java -jar ciir-cli/target/ciir-cli.jar "$@"`).

---

# 7. Tipos de entrada

O argumento `<path>` poderá apontar para:

### POM agregador (papel da Solution)

Um `pom.xml` com `<modules>` — todos os módulos Maven referenciados (direta ou transitivamente) deverão ser analisados.

### POM de módulo (papel do Project)

Um `pom.xml` de um módulo folha — deverá ser analisado apenas esse módulo.

### Diretório

Exemplo:

```bash
ciir /repositories/legacy-system
```

O sistema deverá percorrer recursivamente o diretório procurando arquivos `pom.xml`. Arquivos encontrados em `target/`, `.git/`, `.idea/`, `.settings/` não deverão ser usados para descoberta — comparação **case-sensitive** pelo nome exato do diretório (diferente da escolha case-insensitive do gerador C#, que reflete convenções do NTFS/Windows; em Linux o sistema de arquivos é case-sensitive e a comparação deve refletir isso).

A descoberta deverá evitar análise duplicada: se um POM agregador encontrado já referencia determinado módulo, esse módulo não poderá ser analisado novamente apenas porque seu `pom.xml` também foi encontrado durante a varredura. Deverá ser construído internamente um conjunto único de módulos a analisar. A identidade do módulo deverá ser baseada no caminho físico absoluto e normalizado do `pom.xml`.

---

# 8. Tratamento de múltiplos POMs agregadores

Uma pasta poderá conter:

```text
Root/
  pom.xml                      (agregador)

  services/
    billing/
      pom.xml                  (agregador)
      billing-domain/
        pom.xml

    payment/
      pom.xml                  (módulo folha)
```

Todos os módulos Java únicos encontrados dentro da árvore deverão ser analisados. Módulos repetidos entre POMs agregadores deverão ser analisados apenas uma vez.

---

# 9. Módulos que não puderem ser carregados

Uma falha ao carregar/resolver o classpath de um módulo não deverá necessariamente abortar toda a execução. O comportamento padrão deverá ser: registrar erro, continuar próximos módulos, registrar o problema no relatório.

Com `--fail-on-error`, a aplicação deverá terminar com exit code diferente de zero quando qualquer módulo não puder ser analisado corretamente. Falhas inesperadas não deverão ser silenciosamente ignoradas.

---

# 10. Output

O diretório de output deverá conter no mínimo:

```text
ciir-output/
  ciir.jsonl
  ciir.schema.json
  manifest.json
  analysis-report.json
```

---

# 11. `ciir.jsonl`

JSON Lines: cada linha representa exatamente uma entidade CIIR. Nunca produzir um array JSON para o arquivo principal. O objetivo é permitir streaming de arquivos com milhões de registros.

---

# 12. Requisitos de memória

Não carregar toda a CIIR em memória. Fluxo preferencial:

```text
Módulo Maven
  ↓
Compilation Unit
  ↓
Análise semântica (Symbol Solver)
  ↓
Registro CIIR
  ↓
JSONL writer
```

Os registros deverão ser escritos progressivamente, um documento por vez, sem buffer do arquivo inteiro.

---

# 13. CIIR v1 — envelope conceitual

```json
{
  "schemaVersion": "1.0",
  "id": "...",
  "kind": "...",
  "language": "java",
  "project": "...",
  "symbol": {},
  "source": {},
  "documentation": {},
  "comments": [],
  "relations": [],
  "conditions": [],
  "controlFlow": {},
  "embeddingText": "...",
  "embeddingTextStrategy": "semantic-v1",
  "embeddingTextHash": "...",
  "extensions": {}
}
```

Propriedades sem conteúdo deverão ser omitidas em vez de serializadas com estruturas vazias.

---

# 14. `schemaVersion`

Obrigatório. Versão inicial deste contrato Java: `"1.0"`. Mudanças incompatíveis deverão alterar major version.

---

# 15. `id`

Identidade determinística — nunca GUID/UUID aleatório. Formato: `sha256:<hash>`, construído a partir de uma chave canônica:

```text
language + project identity + kind + canonical symbol identity
```

Exemplo de chave anterior ao hash:

```text
java|
payments-application|
method|
dev.ftathiago.payments.application.PaymentService.authorize(dev.ftathiago.payments.domain.Order)
```

Overloads deverão possuir IDs diferentes. O algoritmo (`dev.ftathiago.ciir.core.identity.CiirIdentity`) está documentado e testado: o id é o SHA-256 (hexadecimal, minúsculo, prefixado com `sha256:`) da chave canônica acima, unindo os quatro componentes com `|` literal. Duas análises do mesmo símbolo semanticamente idêntico DEVEM produzir o mesmo id.

---

# 16. `kind`

O schema CIIR prevê o vocabulário universal (ver `schemas/ciir.schema.json`). O gerador Java v1 implementa apenas:

```text
project
namespace   (populado por packages Java)
type
method
constructor
field
```

**Sem `property`/`event`**: Java não possui sintaxe de primeira classe para propriedades ou eventos — getters/setters são métodos comuns e já geram seus próprios documentos `method`, então nada de informação é perdido ao não introduzir esses kinds. O schema reserva `property`/`event`/`database`/`endpoint`/etc. para outros geradores de linguagem/domínio; o gerador Java não implementa funcionalidades artificiais apenas para preencher todos os kinds do vocabulário universal — YAGNI. O termo universal `namespace` é mantido (em vez de introduzir um kind `package` específico de Java) para preservar um único vocabulário entre linguagens.

---

# 17. `language`

```json
"language": "java"
```

---

# 18. `project`

Identifica o módulo Maven lógico de origem, por exemplo:

```json
"project": "payments-application"
```

O manifest deverá conter informação suficiente para correlacionar esse nome ao `pom.xml`.

---

# 19. `symbol`

Estrutura universal do contrato:

* **`name`** — nome simples.
* **`qualifiedName`** — nome completo, legível por humanos.
* **`canonicalName`** — a identidade inequívoca do símbolo; para métodos/construtores inclui os
  tipos dos parâmetros, de forma que overloads sejam distinguíveis. É esse valor (não o
  `qualifiedName`) que alimenta o hash do `id` (ver §15).
* **`container`** — o nome qualificado da entidade semanticamente proprietária (o tipo ou package
  que contém o símbolo), quando aplicável.

---

# 20. Types

```json
{
  "type": {
    "typeKind": "class",
    "accessibility": "public",
    "modifiers": ["abstract"],
    "genericParameters": []
  }
}
```

`typeKind` para Java v1: `class`, `interface`, `enum`, `record`, `annotation` (para `@interface`), `unknown`. Java não possui `struct`/`delegate`.

---

# 21. Methods

```json
{
  "method": {
    "accessibility": "public",
    "modifiers": [],
    "parameters": [
      { "name": "order", "type": "dev.ftathiago.payments.domain.Order" }
    ],
    "returnType": "dev.ftathiago.payments.domain.PaymentResult"
  }
}
```

Modifiers Java relevantes a preservar: `static`, `final`, `abstract`, `synchronized`, `native`, `strictfp`, `default` (métodos default de interface), `transient`/`volatile` (aplicáveis a fields), `sealed`/`non-sealed` (classes Java 17+).

---

# 22. Source location

Todo símbolo declarado no source deverá possuir path relativo à raiz de análise (**nunca** um caminho absoluto da máquina), linhas/colunas 1-based, e hash SHA-256 do span exato de source. `additionalSourceLocations` é reservado para entidades com mais de uma declaração física; o gerador Java v1 não produz atualmente mais de uma localização por entidade (Java não tem `partial` types/methods como o C#).

---

# 23. Source text

Por padrão, o código-fonte completo do elemento não é incluído no JSONL. Com `--include-source`, `source.text` é emitido. O `embeddingText` continua sendo produzido mesmo sem `source.text`.

---

# 24. Documentation

Para Java, extrair **Javadoc**. Exemplo de source:

```java
/**
 * Authorizes a payment for the given order.
 *
 * @param order order being authorized.
 * @return authorization result.
 */
PaymentResult authorize(Order order);
```

CIIR:

```json
{
  "documentation": {
    "format": "javadoc",
    "source": "declared",
    "summary": "Authorizes a payment for the given order.",
    "parameters": [
      { "name": "order", "description": "order being authorized." }
    ],
    "returns": "authorization result.",
    "exceptions": []
  }
}
```

JavaParser expõe Javadoc estruturado (`BodyDeclaration.getJavadoc()` → `Javadoc`), sem necessidade de parsing manual de XML como no gerador C#.

---

# 25. Comentários comuns

Comentários comuns deverão ser preservados separadamente da documentação formal (Javadoc). Kinds suportados: `line`, `block`, `todo`, `fixme`, `warning`, `note` — classificados por marcador textual (`TODO`, `FIXME`, `WARNING`, `NOTE`, case-insensitive) no início do comentário. Comentários devem ser associados ao elemento semântico apropriado usando a associação nativa de comentários do JavaParser (`Node.getComment()`/`getOrphanComments()`).

---

# 26. Relações

```json
{
  "relations": [
    {
      "kind": "calls",
      "target": { "symbol": "dev.ftathiago.payments.domain.PaymentGateway.authorize" },
      "resolution": { "status": "resolved", "origin": "project" },
      "location": { "startLine": 31, "startColumn": 20, "endLine": 31, "endColumn": 55 }
    }
  ]
}
```

Cada relação registra um único fato estaticamente observável, da entidade de origem até um símbolo alvo. Apenas a relação direta é registrada (`A CALLS B`); a relação inversa (`B CALLED_BY A`) é responsabilidade de uma futura construção de grafo, não deste gerador.

---

# 27. Relações obrigatórias para Java v1

Quando detectáveis estaticamente: `inherits`, `implements`, `overrides`, `calls`, `constructs`, `reads`, `writes`, `throws`, `catches`. Não é necessário gerar a relação inversa (`A CALLS B` é suficiente; não gerar `B CALLED_BY A`).

Definição precisa de cada uma:

* **`contains`** não é produzida pelo gerador Java v1 — a contenção já é recuperável a partir do
  `symbol.container` de cada documento filho, então emitir uma relação `contains` paralela para
  cada membro seria redundante (YAGNI).
* **`inherits` / `implements`** — a superclasse direta / as interfaces diretamente declaradas de um
  tipo. Interfaces implementadas transitivamente por uma superclasse não são repetidas; um grafo
  posterior pode computar isso transitivamente a partir de `inherits` + o `implements` do próprio
  supertipo.
* **`overrides`** — o método virtual/abstrato imediatamente sobrescrito (encontrado casando a
  assinatura apagada — erased signature — subindo pela cadeia de supertipos resolvida; o JavaParser
  não sinaliza "é um override" da forma como a API de símbolos do Roslyn faz).
* **`calls`** — uma invocação estaticamente resolvida, obtida do Symbol Solver do JavaParser
  (`MethodCallExpr.resolve()`), nunca inferida a partir do texto do source (ver §30).
* **`constructs`** — uma expressão de criação de objeto; o alvo é o **tipo construído**, não o
  nome canônico do construtor (ex.: `CONSTRUCTS Money`, não `CONSTRUCTS Money.<init>(BigDecimal,String)`).
* **`reads` / `writes`** — um acesso a campo (ver §28). Um acesso usado apenas como receptor de
  uma chamada (ex.: `gateway` em `gateway.authorize(order)`) ainda produz uma relação `reads` para
  `gateway`: o CIIR mantém todo fato estaticamente observável, e é o `embeddingText` — não a lista
  bruta de relações — que aplica um filtro semântico de ruído (ver §39).
* **`throws`** — o tipo estático do operando de uma instrução `throw`.
* **`catches`** — o(s) tipo(s) de exceção declarado(s) de uma cláusula `catch` (multi-catch do
  Java, `catch (A | B e)`, produz uma relação `catches` por tipo).

---

# 28. Relações com campos

```java
order.getTotal()
```

pode produzir `READS Order.total` (quando `getTotal()` é resolvido como acesso conceitual ao campo, ou diretamente `order.total` em acesso de campo). Uma atribuição:

```java
order.status = OrderStatus.PAID;
```

produz `WRITES Order.status`.

---

# 29. Criação de objetos

```java
new Payment(...)
```

produz `CONSTRUCTS Payment`.

---

# 30. Calls

A análise deverá utilizar o Symbol Solver do JavaParser — nunca inferir chamadas apenas pelo texto da AST. `gateway.authorize(...)` deverá tentar resolver o método via `MethodCallExpr.resolve()`, obtendo o `ResolvedMethodDeclaration` semanticamente correto.

---

# 31. Polimorfismo

Não fingir que a análise estática conhece a implementação concreta chamada em runtime. Dado:

```java
PaymentGateway gateway;
gateway.authorize(order);
```

o fato observável é `CALLS PaymentGateway.authorize`. A análise nunca adivinha qual implementação concreta é invocada em runtime — isso exigiria informação de runtime que a análise estática não possui. Implementações possíveis são representadas pelo grafo de tipos (`StripeGateway IMPLEMENTS PaymentGateway`), recuperáveis a partir das próprias relações `implements`, nunca inferidas na relação de `calls`.

---

# 32. Resolution

```json
{ "resolution": { "status": "resolved", "origin": "project" } }
```

**`status`** — valores possíveis:

* `resolved` — encontrado no módulo analisado ou em outro módulo analisado nesta mesma execução.
* `external` — encontrado, porém fora desta execução de análise (JDK ou uma dependência Maven).
* `unresolved` — nenhum símbolo correspondente encontrado (`UnsolvedSymbolException`).
* `ambiguous` — mais de um candidato, nenhum selecionável estaticamente.
* `dynamic` — reservado; Java não possui uma palavra-chave de despacho dinâmico como o `dynamic`
  do C#, então este gerador não produz esse status atualmente.

**`origin`** — valores possíveis:

* `project` — o próprio módulo Maven analisado.
* `solution` — um módulo Maven **diferente** do módulo atualmente analisado, mas que faz parte
  desta mesma execução (POM agregador com múltiplos módulos analisados na mesma run — resolvido
  via a raiz de source daquele módulo, registrada no symbol solver compartilhado). Nesse caso,
  `status` é `resolved`, e `target.id` é preenchido com o id do documento CIIR do alvo — calculado
  a partir do próprio símbolo alvo, sem exigir que a emissão de documentos daquele outro módulo já
  tenha ocorrido.
* `framework` — o JDK, resolvido via `ReflectionTypeSolver`.
* `dependency` — um artefato Maven de terceiros resolvido, via `JarTypeSolver`.
* `runtime`, `external_service`, `unknown` — reservados para uso futuro.

### `target.id`

`target.id` é preenchido sempre que `resolution.status` for `resolved` (`origin: project` ou
`origin: solution`) **e** o kind do símbolo alvo for um dos que este gerador emite documento
próprio (`type`, `method`, `constructor`, `field`). É sempre omitido para relações
`external`/`unresolved`/`ambiguous`/`dynamic` — nunca inventado.

---

# 33. External symbols

Chamadas ao JDK ou a dependências Maven externas ao run de análise não exigem geração de uma entidade CIIR completa para o alvo — apenas a relação com `status: "external"`.

```json
{
  "kind": "calls",
  "target": { "symbol": "java.util.Objects.requireNonNull(java.lang.Object)" },
  "resolution": { "status": "external", "origin": "framework" }
}
```

---

# 34. Conditions

```json
{
  "conditions": [
    {
      "kind": "if",
      "expression": "order.getTotal() <= 0",
      "location": { "startLine": 17, "endLine": 18 },
      "reads": ["dev.ftathiago.payments.domain.Order.total"]
    }
  ]
}
```

Condições preservam construções de ramificação/repetição como fatos verbatim (`expression` é o texto de source literal) — o CIIR nunca interpreta uma condição como regra de negócio.

Kinds iniciais: `if`, `else_if`, `switch`, `switch_expression`, `while`, `do_while`, `for`, `foreach`, `conditional_expression`, `guard`. `foreach` cobre o for-each aprimorado do Java; `switch_expression` cobre `switch` em modo expressão (Java 14+, sintaxe `->`), enquanto um `switch` clássico é `switch`.

O kind `guard` é usado especificamente para um `if` sem `else` cujo corpo é uma única instrução de saída antecipada (`return`/`throw`/`continue`/`break`); todo outro `if` é `if` (ou `else_if` quando é o ramo `else` de outro `if`).

---

# 35. Control Flow

A v1 não serializa um CFG completo. **Diferença em relação ao gerador C#**: JavaParser não expõe uma API de control-flow-graph equivalente a `Microsoft.CodeAnalysis.FlowAnalysis.ControlFlowGraph`. As métricas são aproximadas por contagem direta de nós de decisão na AST:

* `cyclomaticComplexity` = 1 + nº de (`if`, `for`, for-each, `while`, `do-while`, rótulos `case`, cláusulas `catch`, operadores `&&`/`||`, expressões ternárias `?:`);
* `basicBlockCount` = a mesma contagem de pontos de decisão + 1.

Essa aproximação é uma escolha deliberada e documentada — não é uma omissão silenciosa, nem um CFG completo é serializado em nenhum dos dois geradores (C# ou Java) na v1. O formato é deixado deliberadamente extensível para uma representação futura mais detalhada, em qualquer um dos dois geradores.

```json
{
  "controlFlow": {
    "basicBlockCount": 7,
    "cyclomaticComplexity": 3,
    "hasBranches": true,
    "hasLoops": false
  }
}
```

---

# 36. `embeddingText`

Produzido pelo analisador e armazenado diretamente no JSONL. O vetor NÃO deverá ser produzido — nunca incluir `"embedding": [...]`. O `embeddingText` é a entrada para uma futura etapa separada de geração de embeddings, não a sua saída.

---

# 37. Estratégia de geração do `embeddingText`

Estratégia inicial: `semantic-v1`, determinística, implementada por `dev.ftathiago.ciir.core.embeddingtext.EmbeddingTextBuilder`. Ordem fixa de seções (cada uma — cabeçalho incluído — omitida por completo quando vazia):

```text
Entity
Qualified name
Container
Documentation
Parameters
Returns
Comments
Reads
Writes
Calls
Constructs
Throws
Conditions
```

---

# 38. Exemplo de embeddingText

```text
Entity: method
Qualified name: dev.ftathiago.payments.application.PaymentService.authorize
Container: dev.ftathiago.payments.application.PaymentService
Documentation: Authorizes a payment for the given order.
Parameters:
- order: dev.ftathiago.payments.domain.Order
Returns: dev.ftathiago.payments.domain.PaymentResult
Reads:
- dev.ftathiago.payments.domain.Order.total
Calls:
- dev.ftathiago.payments.domain.PaymentGateway.authorize
Throws:
- dev.ftathiago.payments.domain.InvalidOrderException
```

---

# 39. Informação irrelevante no embeddingText

O CIIR mantém todas as relações extraídas relevantes para o grafo. Porém o `embeddingText` é uma **projeção semântica**. Quais relações `calls`, comentários e condições são relevantes o bastante para entrar é decidido por um único componente coeso e testável — `EmbeddingTextPolicy` (em `ciir-core`) — nunca por `if`s espalhados pelo analisador. A implementação Java (`JavaNoiseEmbeddingTextPolicy`, em `ciir-java`) exclui chamadas extremamente genéricas do JDK (`String.isEmpty`, `Objects.requireNonNull`, operadores comuns de `java.util.stream.*`, `Logger.*`, `System.out.println`, ...) e só inclui comentários que carregam um marcador explícito (TODO/FIXME/WARNING/NOTE). Essa lista não é configurável externamente na v1 de propósito — nada na especificação pede configurabilidade aqui, apenas que a decisão de filtragem viva num único lugar coerente.

---

# 40. Hash do embedding text

```json
"embeddingTextHash": "sha256:..."
```

calculado sobre o texto exato que seria enviado a um modelo de embedding futuramente, permitindo detectar quando reprocessamento é desnecessário.

---

# 41. Separação entre fatos e inferência

O CIIR armazena prioritariamente fatos observáveis (`CALLS`, `READS`, `WRITES`, `THROWS`). Não gerar automaticamente interpretações de negócio como fatos semânticos. Interpretação por LLM é responsabilidade de um componente futuro, fora de escopo aqui.

---

# 42. Uso do JavaParser + Symbol Solver

A análise deverá combinar adequadamente:

```text
AST (CompilationUnit)
+
Symbol Solver (resolução semântica)
+
Classpath do módulo Maven
```

A AST identifica estruturas sintáticas. O Symbol Solver deverá ser utilizado para resolver significado — nunca registrar apenas o nome textual de uma chamada quando a resolução semântica permite determinar o alvo totalmente qualificado.

---

# 43. Símbolos

Considerar as resoluções do Symbol Solver: `ResolvedReferenceTypeDeclaration`, `ResolvedMethodDeclaration`, `ResolvedConstructorDeclaration`, `ResolvedFieldDeclaration`, `ResolvedParameterDeclaration`. Não guardar instâncias JavaParser dentro dos objetos Core — converter imediatamente para tipos CIIR.

---

# 44. Tipos aninhados

Classes/interfaces/enums/records aninhados deverão representar cada um sua própria entidade `type`, com `qualifiedName`/`container` refletindo o aninhamento (nome pontuado, não o formato `Outer$Inner` do bytecode).

---

# 45. Declarações de múltiplas variáveis em um field

```java
private int a, b;
```

deverá gerar **dois** documentos `field` distintos (um por variável declarada), não um documento artificial combinando ambos.

---

# 46. Generated code

Diretórios `target/` (incluindo `target/generated-sources/**`) deverão ser ignorados por padrão na descoberta. Essa decisão deverá ser contabilizada no relatório quando relevante. Adicionar opção futura de exclusão deverá ser possível sem modificar o núcleo.

---

# 47. Records

Java records deverão gerar `kind = type`, `type.typeKind = record`. Diferente do C# (onde um `record struct` exigia uma extensão), o record Java mapeia diretamente sem necessidade de `extensions` para o caso básico.

---

# 48. Extensions

Qualquer informação específica de Java que não pertença ao modelo universal deverá ficar sob:

```json
{ "extensions": { "java": {} } }
```

O Core não deverá acumular propriedades como `hasCompactConstructor` ou `isSealedPermitsList` como campos de primeira classe, a menos que se tornem conceitos realmente universais.

---

# 49. Exemplo completo de method

```json
{
  "schemaVersion": "1.0",
  "id": "sha256:...",
  "kind": "method",
  "language": "java",
  "project": "payments-application",

  "symbol": {
    "name": "authorize",
    "qualifiedName": "dev.ftathiago.payments.application.PaymentService.authorize",
    "canonicalName": "dev.ftathiago.payments.application.PaymentService.authorize(dev.ftathiago.payments.domain.Order)",
    "container": "dev.ftathiago.payments.application.PaymentService"
  },

  "method": {
    "accessibility": "public",
    "modifiers": [],
    "parameters": [
      { "name": "order", "type": "dev.ftathiago.payments.domain.Order" }
    ],
    "returnType": "dev.ftathiago.payments.domain.PaymentResult"
  },

  "source": {
    "path": "src/main/java/dev/ftathiago/payments/application/PaymentService.java",
    "startLine": 9,
    "startColumn": 5,
    "endLine": 24,
    "endColumn": 6,
    "hash": "sha256:..."
  },

  "documentation": {
    "format": "javadoc",
    "source": "declared",
    "summary": "Authorizes a payment for the given order.",
    "parameters": [
      { "name": "order", "description": "Order to authorize." }
    ],
    "returns": "Payment authorization result."
  },

  "relations": [
    {
      "kind": "calls",
      "target": { "symbol": "dev.ftathiago.payments.domain.PaymentGateway.authorize" },
      "resolution": { "status": "resolved", "origin": "project" }
    },
    {
      "kind": "reads",
      "target": { "symbol": "dev.ftathiago.payments.domain.Order.total" },
      "resolution": { "status": "resolved", "origin": "project" }
    },
    {
      "kind": "throws",
      "target": { "symbol": "dev.ftathiago.payments.domain.InvalidOrderException" },
      "resolution": { "status": "resolved", "origin": "project" }
    }
  ],

  "conditions": [
    {
      "kind": "if",
      "expression": "order.getTotal() <= 0",
      "reads": ["dev.ftathiago.payments.domain.Order.total"],
      "location": { "startLine": 13, "endLine": 14 }
    }
  ],

  "controlFlow": {
    "basicBlockCount": 3,
    "cyclomaticComplexity": 2,
    "hasBranches": true,
    "hasLoops": false
  },

  "embeddingText": "Entity: method\nQualified name: dev.ftathiago.payments.application.PaymentService.authorize\nContainer: dev.ftathiago.payments.application.PaymentService\nDocumentation: Authorizes a payment for the given order.\nParameters:\n- order: dev.ftathiago.payments.domain.Order\nReturns: dev.ftathiago.payments.domain.PaymentResult\nReads:\n- dev.ftathiago.payments.domain.Order.total\nCalls:\n- dev.ftathiago.payments.domain.PaymentGateway.authorize\nThrows:\n- dev.ftathiago.payments.domain.InvalidOrderException",

  "embeddingTextStrategy": "semantic-v1",
  "embeddingTextHash": "sha256:..."
}
```

---

# 50. JSON Schema

O projeto mantém `schemas/ciir.schema.json` — um **artifact público do projeto**, própria cópia deste repositório (não compartilhada com `code-csharp-ciir`). Sua finalidade é permitir que terceiros criem geradores para outras linguagens sem depender desta implementação Java. O JSON Schema representa integralmente o CIIR v1 conforme produzido por este gerador.

---

# 51. Requisitos do JSON Schema

JSON Schema 2020-12, com `$schema`, `$id`, `title`, `description`, `$defs`, campos obrigatórios declarados, enums declarados, formatos declarados, `additionalProperties: false` onde apropriado, tipos opcionais representados corretamente, legível por humanos, e validável por qualquer implementador independente de Java (`com.networknt:json-schema-validator` é usado nos testes deste repositório, mas o schema em si não depende dessa biblioteca).

---

# 52. O JSON Schema é o contrato

Testes deverão validar que todo registro CIIR produzido pelo gerador Java é válido contra `ciir.schema.json`, como parte da suíte automática (`ciir-serialization`'s `SchemaConformanceTests` e os testes de integração de `ciir-java`).

---

# 53. Evolução do Schema

Mudanças backward-compatible evoluem a minor version (`1.0 → 1.1`). Mudanças incompatíveis exigem major version (`2.0`). Nunca alterar silenciosamente a semântica de propriedades existentes.

---

# 54. Manifest

`manifest.json` segue a mesma forma conceitual do gerador C# (formato, schemaVersion, generator, input, generatedAt, projects[], files[] com sha256, statistics), com `generator.name = "ciir-java"`.

---

# 55. Analysis report

`analysis-report.json` contém informação operacional (não CIIR): `success`, `projects{discovered,analyzed,failed}`, `documents{analyzed,ignored}`, `relations{resolved,unresolved}`, `errors[]`.

---

# 56. Relações não resolvidas

O relatório deverá contabilizar relações `unresolved`/`ambiguous`/`dynamic`, registrando a razão sempre que possível, sem jamais inventar uma resolução.

---

# 57. Logging

Logging estruturado via SLF4J. `ciir-application`, `ciir-java` e `ciir-serialization` não deverão escrever diretamente em `System.out`/`System.err` — apenas `ciir-cli` controla apresentação de console.

---

# 58. Progresso

Para bases grandes, o CLI deverá informar progresso (`Discovering projects...`, `Found N project(s).`, `[i/total] nome` com contagem de entidades). A implementação de progresso não pode ser dependência da lógica de análise — usar a abstração `AnalysisProgressReporter`.

---

# 59. Cancelamento

Operações longas deverão respeitar um sinal de cancelamento cooperativo (verificado entre módulos no loop de `AnalyzeInputHandler`). `Ctrl+C` no CLI deverá solicitar cancelamento gracioso. O writer deverá finalizar/fechar corretamente arquivos quando possível.

---

# 60. Exit codes

```text
0 = success
1 = analysis completed with fatal failure
2 = invalid arguments/input
3 = output/write failure
```

---

# 61. Determinismo

Executar `ciir ./pom.xml` duas vezes sobre o mesmo conteúdo deve produzir um `ciir.jsonl` byte-a-byte idêntico (com exceção do `generatedAt` do `manifest.json`, que nunca aparece dentro de um documento CIIR). Campos naturalmente variáveis ficam restritos ao manifest.

---

# 62. Ordenação

Isso é obtido por: ordenar os módulos Maven descobertos e os tipos analisados pelo nome totalmente qualificado; ordenar os membros dentro de um tipo por (constructor/method/field, depois nome, depois nome canônico para overloads); ordenar `modifiers[]` numa ordem canônica única (`dev.ftathiago.ciir.core.symbols.CiirModifierOrder`), independentemente da ordem de declaração no source; e nunca incluir horário de parede (wall-clock time) ou outro dado não reproduzível dentro de um `CiirDocument`. De forma geral: módulos, documentos e relations possuem ordem determinística; parâmetros mantêm ordem de declaração; arrays sem significado posicional são ordenados deterministicamente.

---

# 63. Thread safety

Paralelismo é permitido, desde que não comprometa determinismo, não compartilhe o symbol solver de maneira insegura entre threads sem sincronização, não crie consumo ilimitado de memória, e limite concorrência. Preferir uma implementação correta e extensível a um paralelismo prematuro — YAGNI.

---

# 64. Performance

Prioridade: `correctness > semantic accuracy > memory efficiency > performance`. Evitar micro-otimizações prematuras, mas não exigir carregar um reactor Maven inteiro transformado em objetos CIIR antes de gravar o arquivo.

---

# 65. Testes unitários

No mínimo: geração de IDs determinísticos; canonical names; tipos; métodos; overloads; constructors; fields; accessibility; modifiers; generics; inheritance; interfaces; overrides; calls; reads; writes; constructs; throws; catches; conditions; Javadoc; comentários; embeddingText; embeddingTextHash; serialização; validação contra o JSON Schema.

---

# 66. Testes de integração

Fixtures Java pequenas em `fixtures/`: `basic-project` (módulo único, cobre a maioria dos conceitos v1) e `multiple-projects` (reactor de 2 módulos, cobre relações cross-module com `origin: "solution"`). Pipeline real: fixture → resolução de classpath Maven → JavaParser + Symbol Solver → CIIR → JSONL → validação contra o JSON Schema.

---

# 67. Teste de diretório

Deverá existir um teste com estrutura semelhante a:

```text
repo/
  pom.xml                       (agregador)

  src/
    domain/pom.xml
    application/pom.xml

  tools/
    tool/pom.xml
```

Validando que cada `pom.xml` é processado apenas uma vez.

---

# 68. Testes snapshot/golden file

Para casos pequenos, comparar o `embeddingText`/estrutura CIIR produzida contra um texto esperado fixo (como em `EmbeddingTextBuilderTest`), pequeno, legível e revisável.

---

# 69. Code coverage

Cobertura é uma métrica auxiliar, não um objetivo isolado. Priorizar cobertura das regras semanticamente importantes.

---

# 70. Build

O reactor deverá compilar sem warnings relevantes.

```bash
mvn -q -DskipTests package
mvn test
```

---

# 71. Formatting

Manter convenções de formatação consistentes: indentação de 2 espaços, limite de 100 colunas — estilo Google Java Style Guide. Um `.editorconfig` na raiz documenta essas regras, e o plugin Maven do Spotless (`googleJavaFormat`) as aplica/verifica em build (`validate`), com um hook de pre-commit em `.githooks/` que roda `spotless:apply` automaticamente.

---

# 72. Dependency injection

Composição manual (`new` chains) na composition root do CLI — nenhum container de DI é necessário dado o número pequeno de serviços (YAGNI). Evitar Service Locator.

---

# 73. Fluxo principal

```text
ciir <path>
      │
      ▼
Input Resolver
      │
      ├── POM agregador?
      ├── POM de módulo?
      └── diretório?
      │
      ▼
Project Discovery
      │
      ▼
Unique Project Set
      │
      ▼
Java Analyzer
      │
      ▼
Classpath Resolution (Maven)
      │
      ▼
AST + Symbol Solver
      │
      ▼
CIIR Documents
      │
      ├─────────────► EmbeddingTextBuilder
      │
      ▼
Streaming CIIR Writer
      │
      ▼
ciir.jsonl
      │
      ├─────────────► manifest.json
      ├─────────────► analysis-report.json
      └─────────────► ciir.schema.json
```

---

# 74. Fronteira essencial

Permitido:

```text
ciir-java  →  ciir-core
ciir-application  →  ciir-core
```

Proibido:

```text
ciir-core  →  com.github.javaparser
ciir-application  →  ciir-cli
```

---

# 75. Preparação para futuros geradores

A arquitetura deverá permitir futuramente algo semelhante a:

```java
public interface CodeAnalyzer {
    boolean canAnalyze(Path projectPath);
    void analyze(Path projectPath, Path rootDirectory, AnalysisOptions options, Consumer<CiirDocument> sink);
}
```

Essa assinatura é apenas ilustrativa. O requisito arquitetural é que `ciir-application` dependa de uma abstração, não diretamente de JavaParser.

---

# 76. Preparação para outros iniciadores

O principal caso de uso deverá poder ser chamado aproximadamente assim:

```java
handler.execute(command);
```

sem qualquer necessidade de `System.out`/`args[]`/parsing de linha de comando, permitindo futuramente `ciir-api`, `ciir-worker`, `ciir-kubernetes-job`, `ciir-git-webhook` reutilizando integralmente o mesmo Application/Core/Java engine.

---

# 77. O que NÃO implementar agora

Não implementar: banco PostgreSQL; pgvector; embeddings reais; chamada a modelos de IA; LLM; reranking; REST API; Kubernetes; queue; graph database; Neo4j; graph traversal; business-rule extraction; execução dinâmica do código; instrumentação runtime; data-flow interprocedural sofisticado; CFG completo serializado; nenhum backend de análise Java alternativo (JDT ou outro) nesta fase; análise de C#/TypeScript/Python/SQL.

---

# 78. Limitações conhecidas do gerador (v1)

* Métricas de control flow são uma aproximação por contagem de AST, não um control-flow graph
  real (ver §35) — um desvio deliberado em relação à computação baseada em CFG do gerador C#
  apoiado em Roslyn, não uma omissão.
* Expressões de inicialização de fields não são analisadas para relações.
* Blocos de inicialização estática (`static { ... }`) não são representados como seu próprio kind
  de documento.
* Nenhuma análise de *data-flow* entre projetos é realizada (uma relação nunca afirma conhecer um
  valor de runtime ou sua proveniência) — mas relações cross-module para tipos/métodos/campos em
  outro módulo Maven carregado como parte desta mesma execução **são** representadas, como
  `resolved`/`solution`, distintas de chamadas para o JDK ou dependências Maven externas
  (`external`).
* A resolução do classpath de dependências invoca `mvn dependency:build-classpath`, portanto exige
  um `mvn` funcional no `PATH` e, para dependências ainda não resolvidas, acesso à rede para
  popular `~/.m2` — o análogo direto do gerador C# precisar de pacotes NuGet já restaurados via
  `MSBuildWorkspace`.

---

# 79. README

Criar README contendo: propósito do projeto; arquitetura; definição de CIIR; estrutura dos módulos; build; testes; instalação (SDKMAN + Java 25 + Maven); exemplos CLI; arquivos gerados; exemplos JSONL; como validar CIIR contra JSON Schema; regras de versionamento; como criar futuramente um novo generator; limitações conhecidas da análise estática (ver §78).

---

# 80. Definição formal

Ver a definição formal citada na abertura deste documento (§1).

---

# 81. Critérios de aceite

* `ciir modulo/pom.xml` gera CIIR válido para o módulo.
* `ciir aggregator/pom.xml` gera CIIR para todos os módulos Java referenciados.
* `ciir ./repository` descobre e processa todos os módulos Java únicos existentes abaixo da pasta.
* Produz `ciir.jsonl` válido e processável linha a linha.
* Produz `ciir.schema.json` e todos os registros gerados são válidos contra ele.
* Produz `manifest.json` com informações da execução.
* Produz `analysis-report.json` com erros, warnings e relações não resolvidas.
* A implementação utiliza o Symbol Solver do JavaParser, não apenas parsing textual.
* Extrai no mínimo `inherits`, `implements`, `overrides`, `calls`, `constructs`, `reads`, `writes`, `throws`, `catches` quando estaticamente observáveis.
* Extrai Javadoc.
* Preserva comentários normais relevantes separadamente.
* Preserva condições encontradas nos métodos.
* Todo elemento indexável possui `embeddingText`, `embeddingTextStrategy`, `embeddingTextHash`.
* Mesmo source produz os mesmos IDs e conteúdo semântico.
* CLI não contém lógica de análise.
* Nenhum módulo genérico (`ciir-core`, `ciir-application`) depende de tipos JavaParser.
* Todos os testes passam com `mvn test`.
* Reactor compila sem warnings relevantes.

---

# 82. Diretriz final para implementação

Antes de escrever código:

1. criar o reactor Maven;
2. estabelecer dependências entre módulos;
3. implementar o modelo CIIR;
4. criar `ciir.schema.json`;
5. implementar serialização JSONL;
6. implementar resolução de input;
7. implementar descoberta de módulos Maven;
8. implementar análise JavaParser + Symbol Solver;
9. implementar geração do `embeddingText`;
10. implementar orchestration;
11. implementar CLI;
12. implementar manifest e relatório;
13. criar testes de integração (fixtures);
14. documentar o CIIR (README, CLAUDE.md, esta especificação).

Não inverter essa ordem criando primeiro uma grande `Main.java`. A arquitetura deverá existir antes da integração CLI.

---

# 83. Regra arquitetural central

```text
             ENTRY POINTS
                  │
         ┌────────┴────────┐
         │                 │
        CLI             future API
         │                 │
         └────────┬────────┘
                  ▼
             Application
                  │
        ┌─────────┴──────────┐
        ▼                    ▼
   Analyzer Contract       Writers
        ▲
        │
  Java / JavaParser
        │
        ▼
       CIIR
```

O **CIIR é o contrato central da plataforma**, e não JavaParser e não o CLI. JavaParser é apenas o segundo produtor desse contrato (o primeiro sendo Roslyn, em `code-csharp-ciir`). O CLI é apenas o primeiro consumidor deste módulo do caso de uso de geração desse contrato.
