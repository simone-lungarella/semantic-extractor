.DEFAULT_GOAL := help

.PHONY: help run run-ollama test redis-up redis-down redis-status redis-index ollama-up ollama-pull ollama-status import demo

# Show the available development commands.
help:
	@echo "Semantic Extractor commands:"
	@echo "  make run           Start the Spring Boot API"
	@echo "  make run-ollama    Start the API with local Granite embeddings"
	@echo "  make test          Run the test suite"
	@echo "  make redis-up      Start Redis Stack"
	@echo "  make redis-down    Stop Redis Stack (keep data)"
	@echo "  make redis-status  Show the Redis container status"
	@echo "  make redis-index   Show a summary of the vector index"
	@echo "  make ollama-up     Start local Ollama"
	@echo "  make ollama-pull   Download the Granite embedding model"
	@echo "  make ollama-status Show the installed Ollama models"
	@echo "  make import        Import a distributed dictionary sample"
	@echo "  make demo          Run the demo search queries"

# Start the Spring Boot API in the foreground.
run:
	mvn spring-boot:run

# Start the API with the local Granite embedding model served by Ollama.
run-ollama:
	EMBEDDING_PROVIDER=ollama mvn spring-boot:run

# Run the application test suite.
test:
	mvn test

# Start the Redis Stack container and persistent volume.
redis-up:
	podman compose up -d

# Stop Redis without deleting its persistent volume.
redis-down:
	podman compose down

# Show the current Redis container state and health.
redis-status:
	podman compose ps

# Show only the Redis vector-index details useful during a demonstration.
redis-index:
	@command -v jq >/dev/null || (echo "Missing required command: jq" >&2; exit 1)
	@podman exec semantic-extractor-redis redis-cli --json FT.INFO word-index | jq '{index: .index_name, documents: .num_docs, vector: (.attributes[] | select(.type == "VECTOR") | {field: .identifier, algorithm, dataType: .data_type, dimensions: .dim, distanceMetric: .distance_metric}), memoryMb: .vector_index_sz_mb, indexingFailures: .hash_indexing_failures}'

# Ollama is an optional Compose profile so Redis can still run independently.
ollama-up:
	podman compose --profile ollama up -d ollama

ollama-pull: ollama-up
	podman exec semantic-extractor-ollama ollama pull granite-embedding:30m

ollama-status:
	podman exec semantic-extractor-ollama ollama list

# Import a deterministic sample spread across the dictionary. The named terms
# make the semantic-search demonstration repeatable while remaining real entries
# from /usr/share/dict/words.
import:
	@test -r /usr/share/dict/words || (echo "Missing readable dictionary: /usr/share/dict/words" >&2; exit 1)
	@command -v jq >/dev/null || (echo "Missing required command: jq" >&2; exit 1)
	@curl --fail --silent --show-error http://localhost:8080/actuator/health >/dev/null || (echo "Semantic Extractor is not healthy at http://localhost:8080" >&2; exit 1)
	@{ awk 'NR % 484 == 1' /usr/share/dict/words | head -n 990; grep -x -E 'airplane|automobile|developer|doctor|firewall|hammer|hospital|library|school|teacher' /usr/share/dict/words; } | sort -u | curl --fail --silent --show-error --request POST --header 'Content-Type: text/plain' --data-binary @- 'http://localhost:8080/api/words/import?reset=true' | jq

# Compare literal prefix matching with natural-language semantic retrieval.
demo:
	@command -v jq >/dev/null || (echo "Missing required command: jq" >&2; exit 1)
	@curl --fail --silent --show-error 'http://localhost:8080/api/words/lexical?q=lib&limit=5' | jq
	@curl --fail --silent --show-error --get --data-urlencode 'q=a place where books are borrowed' --data-urlencode 'limit=5' http://localhost:8080/api/words/semantic | jq
	@curl --fail --silent --show-error --get --data-urlencode 'q=a person who creates computer software' --data-urlencode 'limit=5' http://localhost:8080/api/words/semantic | jq
	@curl --fail --silent --show-error --get --data-urlencode 'q=a tool used to hit a nail' --data-urlencode 'limit=5' http://localhost:8080/api/words/semantic | jq
	@curl --fail --silent --show-error --get --data-urlencode 'q=a place where sick people receive treatment' --data-urlencode 'limit=5' http://localhost:8080/api/words/semantic | jq
