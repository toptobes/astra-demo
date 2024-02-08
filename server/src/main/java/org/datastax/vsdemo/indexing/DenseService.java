package org.datastax.vsdemo.indexing;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.datastax.vsdemo.indexing.database.DenseEntity;
import org.datastax.vsdemo.indexing.database.DenseRepository;
import org.datastax.vsdemo.indexing.messages.IndexRequest;
import org.datastax.vsdemo.indexing.messages.SimilarityResult;
import org.datastax.vsdemo.indexing.records.EmbeddedPassage;
import org.datastax.vsdemo.indexing.records.EmbeddedQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.datastax.vsdemo.indexing.utils.Prelude.zipWith;

@Service
public class DenseService {
    private final DenseRepository denseRepository;

    public DenseService(DenseRepository denseRepository) {
        this.denseRepository = denseRepository;
    }

    public void indexPassages(String userID, List<IndexRequest> request, EmbeddedPassage embeddingResult) {
        denseRepository.saveAll(zipWith(
            (req, embedding) -> new DenseEntity(userID, UUID.randomUUID(), embedding, req.text(), req.url()),
            request,
            embeddingResult.dense()
        ));
    }

    @Async
    public CompletableFuture<List<SimilarityResult.Single>> findSimilarSentences(String userID, EmbeddedQuery embeddedQuery, int limit) {
        var rawResults = denseRepository.findByUserIDAndANN(userID, embeddedQuery.dense(), limit);
        var results = createSimilarityResults(rawResults);
        return CompletableFuture.completedFuture(results);
    }

    private List<SimilarityResult.Single> createSimilarityResults(ResultSet rows) {
        var entities = new ArrayList<SimilarityResult.Single>();

        rows.forEach((row) -> {
            var url = row.getString("url");
            var text = row.getString("text");
            entities.add(new SimilarityResult.Single(text, url));
        });

        return entities;
    }
}
