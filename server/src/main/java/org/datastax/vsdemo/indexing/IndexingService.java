package org.datastax.vsdemo.indexing;

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.data.CqlVector;
import org.datastax.vsdemo.indexing.messages.IndexRequest;
import org.datastax.vsdemo.indexing.messages.SimilarityResult;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;

import static org.datastax.vsdemo.indexing.VectorUtils.testSimilarity;

@Service
public class IndexingService {
    private final PyEmbeddingService embedder;
    private final TextRepository repository;

    public IndexingService(TextRepository repository, PyEmbeddingService embedder) {
        this.repository = repository;
        this.embedder = embedder;
    }

    public void indexSentences(String userID, List<IndexRequest> request) {
        var denoised = removeNoise(request);

        var texts = denoised.stream()
            .map(IndexRequest::text)
            .toList();

        embedder.embed(texts, PyEmbeddingService.Type.PASSAGE).thenAccept(embeddings -> {
            var entities = IntStream.range(0, denoised.size())
                .mapToObj(i -> (
                    Pair.of(embeddings.get(i), denoised.get(i))
                ))
                .map(p -> (
                    new TextEntity(userID, UUID.randomUUID(), p.getFirst(), p.getSecond().text(), p.getSecond().url())
                ))
                .toList();

            repository.saveAll(entities);
        });
    }

    public CompletionStage<List<SimilarityResult>> getSimilarSentences(String userID, String query, int limit) {
        System.out.println("Querying for " + query);

        var embeddedQueryFuture = embedder.embed(query, PyEmbeddingService.Type.QUERY);

        System.out.println("Embedded query " + query);

        return embeddedQueryFuture.thenCompose(embeddedQuery -> (
            repository
                .findByUserIDAndANN(userID, embeddedQuery, limit)
                .thenApply(resultSet -> createSimilarityResults(resultSet, embeddedQuery))
        ));
    }

    @SuppressWarnings({"DataFlowIssue", "unchecked"})
    private List<SimilarityResult> createSimilarityResults(AsyncResultSet resultSet, CqlVector<Float> embeddedQuery) {
        var rows = resultSet.currentPage();
        var entities = new ArrayList<SimilarityResult>();

        rows.forEach((row) -> {
            var url = row.getString("url");
            var text = row.getString("text");

            var embeddingE5 = row.get("embedding_e5", CqlVector.class);
            var similarity = testSimilarity(embeddingE5, embeddedQuery);

            entities.add(new SimilarityResult(text, url, similarity));
        });

        return entities;
    }

    private List<IndexRequest> removeNoise(List<IndexRequest> request) {
        return request.stream()
            .map(r -> (
                new IndexRequest(r.text().trim(), r.url())
            ))
            .filter(r -> (
                r.text().length() > 10 && !r.text().isBlank()
            ))
            .toList();
    }
}
