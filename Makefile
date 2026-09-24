.DEFAULT_GOAL := help

.PHONY: help run test redis-up redis-down redis-status import

# Show the available development commands.
help:
	@echo "Semantic Extractor commands:"
	@echo "  make run           Start the Spring Boot API"
	@echo "  make test          Run the test suite"
	@echo "  make redis-up      Start Redis Stack"
	@echo "  make redis-down    Stop Redis Stack (keep data)"
	@echo "  make redis-status  Show the Redis container status"
	@echo "  make import        Import 1,000 Linux dictionary words"

# Start the Spring Boot API in the foreground.
run:
	mvn spring-boot:run

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

# Import a fixed sample from the local Linux dictionary through the running API.
import:
	@test -r /usr/share/dict/words || (echo "Missing readable dictionary: /usr/share/dict/words" >&2; exit 1)
	@curl --fail --silent --show-error http://localhost:8080/actuator/health >/dev/null || (echo "Semantic Extractor is not healthy at http://localhost:8080" >&2; exit 1)
	@head -n 1000 /usr/share/dict/words | curl --fail --silent --show-error --request POST --header 'Content-Type: text/plain' --data-binary @- 'http://localhost:8080/api/words/import?reset=true'
	@echo
