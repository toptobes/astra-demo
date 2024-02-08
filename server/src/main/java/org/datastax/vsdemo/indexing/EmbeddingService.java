package org.datastax.vsdemo.indexing;

import org.datastax.vsdemo.indexing.records.EmbeddedPassage;
import org.datastax.vsdemo.indexing.records.EmbeddedQuery;
import org.datastax.vsdemo.indexing.utils.VectorUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.datastax.vsdemo.indexing.utils.Prelude.map;
import static org.datastax.vsdemo.indexing.utils.VectorUtils.listToCqlVector;

@Service
public class EmbeddingService {
    private final RestClient restClient;

    public EmbeddingService(@Value("${astra-demo.embedding-service.url}") String embedUrl, RestClient.Builder restBuilder) {
        this.restClient = restBuilder.baseUrl(embedUrl).build();
    }

    @SuppressWarnings({"DataFlowIssue"})
    public EmbeddedPassage embedPassages(List<String> texts) {
        var result = restClient.post()
            .uri("/embed/passages")
            .body(new PassageEmbedRequest(texts))
            .retrieve()
            .body(PassageEmbedResponse.class);

        return new EmbeddedPassage(
            map(VectorUtils::listToCqlVector, result.dense),
            map(map(VectorUtils::listToCqlVector), result.multi)
        );
    }

    @SuppressWarnings("DataFlowIssue")
    public EmbeddedQuery embedQuery(String text) {
        var result = restClient.post()
            .uri("/embed/query")
            .body(new QueryEmbedRequest(text))
            .retrieve()
            .body(QueryEmbedResponse.class);

        return new EmbeddedQuery(
            listToCqlVector(result.dense),
            map(VectorUtils::listToCqlVector, result.multi)
        );
    }

    private record PassageEmbedRequest(List<String> texts) {}
    private record QueryEmbedRequest(String text) {}

    private record PassageEmbedResponse(List<List<Double>> dense, List<List<List<Double>>> multi) {}
    private record QueryEmbedResponse(List<Double> dense, List<List<Double>> multi) {}
}
