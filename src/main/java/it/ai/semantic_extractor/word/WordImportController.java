package it.ai.semantic_extractor.word;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/words")
public class WordImportController {

    private final WordImportService wordImportService;

    public WordImportController(WordImportService wordImportService) {
        this.wordImportService = wordImportService;
    }

    @PostMapping(value = "/import", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public WordImportResponse importWords(
            @RequestParam(defaultValue = "false") boolean reset,
            @RequestBody String words) {
        return wordImportService.importWords(words.lines().toList(), reset);
    }
}
