# Demonstration Guide

This guide runs the complete local semantic-search workflow with IBM Granite
Embedding 30M English through Ollama. No cloud credentials are required.

## Before starting

Run this once to download the local model:

```bash
make ollama-pull
```

Close any application already using port 8080. The walkthrough uses two
terminal windows: one for the application and one for commands.

## Start the demonstration

In the first terminal, start Redis and Ollama:

```bash
make redis-up
make ollama-up
make ollama-status
```

The model list should contain `granite-embedding:30m`.

Start the application with Ollama selected as its embedding provider:

```bash
make run-ollama
```

Keep this terminal visible long enough to show that Spring Boot started on port
8080.

## Import the catalog

In the second terminal:

```bash
make import
```

The import takes a deterministic sample spread throughout
`/usr/share/dict/words`, adds a small set of known demonstration terms, creates
their embeddings in batches, and stores the terms and binary vectors in Redis.
It also recreates the 384-dimensional cosine vector index.

The response reports imported, skipped, and total terms plus import duration.

## Show the comparison

Run all prepared queries:

```bash
make demo
```

Explain the results as follows:

1. Lexical search receives the prefix `lib` and finds words beginning with
   those letters. It does not understand a description.
2. Semantic search embeds `a place where books are borrowed` and retrieves
   `library` even though that word is absent from the query.
3. Further queries should retrieve `developer`, `hammer`, and `hospital` near
   the top of the ranked results.
4. The scores represent cosine-vector similarity, not confidence or factual
   certainty.

Results can vary when the model, dictionary, or Ollama version changes. Do not
hide irrelevant results: they demonstrate the limitations of a small model and
a catalog made of isolated dictionary words.

## Optional technical evidence

Show application and Redis health:

```bash
curl --silent http://localhost:8080/actuator/health | jq
```

Show that Redis indexed binary 384-dimensional vectors:

```bash
make redis-index
```

This prints only the index name, document count, vector configuration, memory
usage, and indexing-failure count. The relevant vector fields are `FLOAT32`,
dimension `384`, and distance metric `COSINE`.

Show that the application can generate an embedding directly:

```bash
curl --silent \
  --request POST \
  --header 'Content-Type: text/plain' \
  --data 'library' \
  http://localhost:8080/api/embeddings/test | jq
```

The endpoint exposes only the vector dimensions and a five-value sample, not
the complete embedding.
