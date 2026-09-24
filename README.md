# Semantic Extractor

Semantic Extractor is a small Spring Boot proof of concept that demonstrates
semantic search over a simulated catalog of terms.

The application reads words from a Linux dictionary file, creates a vector
embedding for each term, and stores the terms and vectors in Redis. A user can
then describe a concept in natural language and retrieve the words that are
semantically closest to that description.

For example:

```text
Query:   "a place where books are borrowed"
Results: library, archive, bookstore
```

This project intentionally has a small scope. Its purpose is to demonstrate a
complete embedding and vector-search workflow rather than provide a
production-ready search service.

## How it works

The following sequence shows both the catalog import and semantic-search paths:

![Semantic Extractor sequence](docs/diagrams/semantic-extractor-sequence.svg)

The editable Mermaid source is available at
[`docs/diagrams/semantic-extractor-sequence.mmd`](docs/diagrams/semantic-extractor-sequence.mmd).

### Import

The import process:

1. reads terms from a configurable words file;
2. normalizes, filters, and deduplicates them;
3. limits the number of terms to keep the POC fast and inexpensive;
4. generates an embedding for each accepted term; and
5. stores the term, its vector, and minimal metadata in Redis.

The Linux words file is used only as synthetic data to simulate a larger
catalog. It is not intended to be a high-quality semantic knowledge base.

### Search

For each search request, the application:

1. generates an embedding for the natural-language query;
2. performs a nearest-neighbor vector search in Redis;
3. returns the closest terms and their similarity scores; and
4. returns no confident match when results fall below a configured threshold.

A lexical search is also provided as a baseline. This makes it possible to
compare literal matching with concept-based retrieval.

## Planned API

### Import words

```http
POST /api/words/import?limit=1000
```

Example response:

```json
{
  "source": "/usr/share/dict/words",
  "imported": 1000,
  "skipped": 42,
  "durationMs": 12500
}
```

### Lexical search

```http
GET /api/words/lexical?q=protect
```

This endpoint searches for literal matches and serves as the non-AI baseline.

### Semantic search

```http
GET /api/words/semantic?q=something%20that%20protects%20a%20network&limit=5
```

Example response:

```json
{
  "query": "something that protects a network",
  "matches": [
    {
      "term": "firewall",
      "score": 0.84
    },
    {
      "term": "gateway",
      "score": 0.77
    }
  ]
}
```

The API shown above describes the target POC and will be updated if
implementation details change.

## Technology

- Java 17
- Spring Boot
- Maven
- An embedding model exposed through a configurable provider
- Redis Stack with vector-search support
- JUnit for tests and a small retrieval evaluation

The project deliberately excludes authentication, a web interface, agent
orchestration, and production infrastructure. These features would add
development cost without improving the main experiment.

## Data model

Each indexed entry contains approximately:

```json
{
  "id": "word:firewall",
  "term": "firewall",
  "normalizedTerm": "firewall",
  "source": "linux-words",
  "embedding": [0.012, -0.034, 0.056]
}
```

The actual embedding dimension depends on the selected model. The Redis vector
index must use the same dimension and distance metric as the generated vectors.

## Local development

### Prerequisites

- JDK 17
- Maven 3.9+
- Docker or a locally available Redis Stack instance
- Access to the configured embedding model
- A words file such as `/usr/share/dict/words`

The words file may need to be installed separately, depending on the Linux
distribution. A custom path can be configured when the default file is
unavailable.

### Start Redis Stack

```bash
docker run --rm --name semantic-extractor-redis \
  -p 6379:6379 \
  redis/redis-stack-server:latest
```

### Configure the application

The final implementation will expose configuration for:

```properties
semantic-extractor.words-file=/usr/share/dict/words
semantic-extractor.import-limit=1000
semantic-extractor.search-limit=5
semantic-extractor.similarity-threshold=0.60
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

Model-specific endpoint and credential properties will depend on the selected
embedding provider. Secrets must be supplied through environment variables and
must not be committed to the repository.

### Run

```bash
mvn spring-boot:run
```

### Test

```bash
mvn test
```

## Evaluation

The POC will use a small set of natural-language queries with expected terms,
for example:

| Query | Expected term |
|---|---|
| A place where books are borrowed | `library` |
| Protection for a computer network | `firewall` |
| A person who writes software | `developer` |
| A tool used to strike a nail | `hammer` |

The primary metric is whether the expected term appears in the top five
results. Semantic-search results will be compared with lexical-search results
to demonstrate where embeddings add value and where they do not.

## Design decisions

- **Embeddings are the AI capability.** They represent the semantic meaning of
  terms and queries as vectors suitable for similarity search.
- **Redis is both storage and search infrastructure.** Redis Stack provides
  vector indexing and nearest-neighbor retrieval without requiring another
  database.
- **The corpus is intentionally synthetic.** The Linux dictionary simulates a
  catalog while avoiding private or client data.
- **The import is capped.** A small corpus reduces model calls, cost, and
  startup time while preserving the end-to-end workflow.
- **Lexical search is retained as a baseline.** The POC should demonstrate
  measurable value rather than assume vector search is better.
- **No generated answer is required.** The application returns retrieved terms
  directly, avoiding an additional language-model call and reducing
  hallucination risk.

## Limitations and responsible AI considerations

Isolated words contain little context and can have multiple meanings. For
example, `bank` may refer to a financial institution or the side of a river.
Similarity scores indicate vector proximity; they do not prove that a result is
correct.

The application therefore:

- presents results as suggestions rather than facts;
- returns multiple candidates with similarity scores;
- supports a minimum similarity threshold;
- uses only synthetic, non-sensitive input data; and
- records actual evaluation results, including unsuccessful queries.

A production implementation would additionally require access control, governed
source data, privacy controls, monitoring, model-version management, and
broader quality and bias evaluation.

## Project status

This repository is an early proof of concept. The Spring Boot project skeleton
exists; embedding generation, Redis indexing, search endpoints, and evaluation
are the next implementation steps.
