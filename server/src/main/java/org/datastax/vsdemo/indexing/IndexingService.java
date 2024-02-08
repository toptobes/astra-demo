package org.datastax.vsdemo.indexing;

import org.datastax.vsdemo.indexing.messages.IndexRequest;
import org.datastax.vsdemo.indexing.messages.SimilarityResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.datastax.vsdemo.indexing.utils.Prelude.map;

@Service
public class IndexingService {
    private final EmbeddingService embedder;
    private final DenseService denseService;
    private final MultiService multiService;

    @Value("${astra-demo.share-texts}")
    private boolean shareTexts;

    public IndexingService(DenseService denseService, MultiService multiService, EmbeddingService embedder) {
        this.denseService = denseService;
        this.multiService = multiService;
        this.embedder = embedder;
    }

    @Async
    public void indexSentences(String userID, List<IndexRequest> request) {
        var denoised = removeNoise(request);

        var texts = map(IndexRequest::text, denoised);
        var embeddingResult = embedder.embedPassages(texts);

        var finalUserID = determineUserID(userID);

        denseService.indexPassages(finalUserID, denoised, embeddingResult);
        multiService.indexPassages(finalUserID, denoised, embeddingResult);
    }

    @Async
    public CompletableFuture<SimilarityResult> getSimilarSentences(String userID, String query, int limit) {
        var embeddedQuery = embedder.embedQuery(query);
        var finalUserID = determineUserID(userID);

        var denseResults = denseService.findSimilarSentences(finalUserID, embeddedQuery, limit);
        var multiResults = multiService.findSimilarSentences(finalUserID, embeddedQuery, limit);

        return CompletableFuture.completedFuture(new SimilarityResult(denseResults.join(), multiResults.join()));
    }

    private List<IndexRequest> removeNoise(List<IndexRequest> request) {
        return request.stream()
            .map(r -> (
                new IndexRequest(r.text().trim(), r.url())
            ))
            .filter(r -> (
                r.text().length() >= 8 && !r.text().isBlank()
            ))
            .toList();
    }

    private String determineUserID(String userID) {
        return shareTexts ? "shared" : userID;
    }
}
