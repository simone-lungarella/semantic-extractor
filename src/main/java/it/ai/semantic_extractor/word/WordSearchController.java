package it.ai.semantic_extractor.word;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/words")
public class WordSearchController {

    private final WordSearchService wordSearchService;

    public WordSearchController(WordSearchService wordSearchService) {
        this.wordSearchService = wordSearchService;
    }

    @GetMapping("/lexical")
    public WordSearchResponse lexicalSearch(
            @RequestParam("q") String query,
            @RequestParam(required = false) Integer limit) {

        return wordSearchService.lexicalSearch(query, limit);
    }

    @GetMapping("/semantic")
    public SemanticSearchResponse semanticSearch(
            @RequestParam("q") String query,
            @RequestParam(required = false) Integer limit) {

        return wordSearchService.semanticSearch(query, limit);
    }
}
