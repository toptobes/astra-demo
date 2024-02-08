package org.datastax.vsdemo.indexing.database;

import com.datastax.astra.sdk.AstraClient;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.data.CqlVector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@DependsOn("denseTableInitializer")
public class DenseRepository {
    private final CqlSession session;
    private PreparedStatement insertSentence;
    private PreparedStatement similarSentences;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Value("${astra.cql.driver-config.basic.session-keyspace}")
    private String keyspace;

    @Value("${astra-demo.entry-ttl}")
    private int ttl;

    public DenseRepository(AstraClient astra) {
        this.session = astra.cqlSession();
    }

    @PostConstruct
    public void initializeStatements() {
        var ttl = (this.ttl >= 0)
            ? "USING TTL " + this.ttl
            : "";

        insertSentence = session.prepare("""
            INSERT INTO %s.dense (user_id, text_id, embedding, text, url) VALUES(?, ?, ?, ?, ?) %s;
        """.formatted(keyspace, ttl));

        similarSentences = session.prepare("""
            SELECT url, text FROM %s.dense WHERE user_id = ? ORDER BY embedding ANN OF ? LIMIT ?;
        """.formatted(keyspace));
    }

    public void saveAll(List<DenseEntity> entity) {
        entity.forEach(this::save);
    }

    private void save(DenseEntity entity) {
        var boundInsertion = insertSentence.bind(entity.userID(), entity.textID(), entity.embedding(), entity.text(), entity.url());
        executor.submit(() -> session.execute(boundInsertion));
    }

    public ResultSet findByUserIDAndANN(String userID, CqlVector<Float> embedding, int limit) {
        var boundQuery = similarSentences.bind(userID, embedding, limit);
        return session.execute(boundQuery);
    }
}
