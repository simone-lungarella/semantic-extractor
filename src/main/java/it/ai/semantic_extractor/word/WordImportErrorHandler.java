package it.ai.semantic_extractor.word;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WordImportErrorHandler {

    @ExceptionHandler(WordImportException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> importFailed(WordImportException exception) {
        return Map.of("error", exception.getMessage());
    }
}
