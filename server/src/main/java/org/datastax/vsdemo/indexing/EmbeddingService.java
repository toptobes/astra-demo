package org.datastax.vsdemo.indexing;

import com.datastax.oss.driver.api.core.data.CqlVector;
import org.datastax.vsdemo.indexing.records.EmbeddedPassage;
import org.datastax.vsdemo.indexing.records.EmbeddedQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.datastax.vsdemo.indexing.utils.Prelude.map;

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
            map(EmbeddingService::listToCqlVector, result.dense),
            map(map(EmbeddingService::listToCqlVector), result.multi)
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
            map(EmbeddingService::listToCqlVector, result.multi)
        );
    }

    @SuppressWarnings("unchecked")
    private static CqlVector<Float> listToCqlVector(List<Float> list) {
        return CqlVector.builder().addAll(list).build();
    }

    private record PassageEmbedRequest(List<String> texts) {}
    private record QueryEmbedRequest(String text) {}

    private record PassageEmbedResponse(List<List<Float>> dense, List<List<List<Float>>> multi) {}
    private record QueryEmbedResponse(List<Float> dense, List<List<Float>> multi) {}
}
