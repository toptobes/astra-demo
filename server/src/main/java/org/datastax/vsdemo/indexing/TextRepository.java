package org.datastax.vsdemo.indexing;

import com.datastax.astra.sdk.AstraClient;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.data.CqlVector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.CompletionStage;

@Service
@DependsOn("indexingTableInitializer")
public class TextRepository {
    private final CqlSession session;
    private PreparedStatement insertSentence;
    private PreparedStatement similarSentences;

    @Value("${astra.cql.driver-config.basic.session-keyspace}")
    private String keyspace;

    public TextRepository(AstraClient astra) {
        this.session = astra.cqlSession();
    }

    @PostConstruct
    public void initializeStatements() {
        insertSentence = session.prepare("""
            INSERT INTO %s.indexing (user_id, text_id, embedding_e5_small_v2, text, url) VALUES(?, ?, ?, ?, ?);
        """.formatted(keyspace));

        similarSentences = session.prepare("""
            SELECT * FROM %s.indexing WHERE user_id = ? ORDER BY embedding_e5_small_v2 ANN OF ? LIMIT ?;
        """.formatted(keyspace));
    }

    public void saveAll(List<TextEntity> entities) {
        entities.forEach(entity -> {
            var boundInsertion = insertSentence.bind(entity.userID(), entity.textID(), entity.embeddingE5SmallV2(), entity.text(), entity.url());
            session.executeAsync(boundInsertion);
        });
    }

    public CompletionStage<AsyncResultSet> findByUserIDAndANN(String userID, CqlVector<Float> embedding, int limit) {
        var boundQuery = similarSentences.bind(userID, embedding, limit);
        return session.executeAsync(boundQuery);
    }
}
