package org.datastax.vsdemo.indexing;

import com.datastax.oss.driver.api.core.data.CqlVector;
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

    @Async
    @SuppressWarnings({"rawtypes", "DataFlowIssue", "unchecked"})
    public CompletableFuture<List<CqlVector<Float>>> embed(List<String> texts) {
        ResponseEntity<List> responseEntity = restTemplate.postForEntity(
            "http://localhost:5000/embed",
            new EmbedRequest(texts),
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
    public CompletableFuture<CqlVector<Float>> embed(String text) {
        return embed(List.of(text)).thenApply(list -> list.get(0));
    }

    private record EmbedRequest(List<String> texts) {}
}
