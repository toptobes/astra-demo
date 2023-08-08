package org.datastax.vsdemo.indexing;

import com.datastax.oss.driver.api.core.data.CqlVector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.datastax.vsdemo.indexing.VectorUtils.listToCqlVector;

@Service
public class PyEmbeddingService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${embedding-service.url}")
    private String embedUrl;

    @Async
    @SuppressWarnings({"rawtypes", "DataFlowIssue", "unchecked"})
    public CompletableFuture<List<CqlVector<Float>>> embed(List<String> texts, Type type) {
        ResponseEntity<List> responseEntity = restTemplate.postForEntity(
            embedUrl,
            new EmbedRequest(texts.stream().map(text -> type.name().toLowerCase() + ": " + text).toList()),
            List.class
        );

        List<CqlVector<Float>> embeddings = responseEntity.getBody()
            .stream().map(embedding -> {
                var floatEmbedding = ((List<Double>) embedding).stream()
                    .map(Double::floatValue)
                    .collect(Collectors.toList());

                return listToCqlVector(floatEmbedding);
            }).toList();

        return CompletableFuture.completedFuture(embeddings);
    }

    @Async
    public CompletableFuture<CqlVector<Float>> embed(String text, Type type) {
        return embed(List.of(text), type).thenApply(list -> list.get(0));
    }

    private record EmbedRequest(List<String> texts) {}

    public enum Type {
        QUERY, PASSAGE
    }
}
