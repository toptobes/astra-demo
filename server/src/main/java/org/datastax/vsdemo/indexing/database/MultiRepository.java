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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@DependsOn("multiTableInitializer")
public class MultiRepository {
    private final CqlSession session;
    private PreparedStatement insertSentence;
    private PreparedStatement similarParts;
    private PreparedStatement byID;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Value("${astra.cql.driver-config.basic.session-keyspace}")
    private String keyspace;

    @Value("${astra-demo.entry-ttl}")
    private int ttl;

    public MultiRepository(AstraClient astra) {
        this.session = astra.cqlSession();
    }

    @PostConstruct
    public void initializeStatements() {
        var ttl = (this.ttl >= 0)
            ? "USING TTL " + this.ttl
            : "";

        insertSentence = session.prepare("""
            INSERT INTO %s.multi (user_id, text_id, part_id, embedding, text, url) VALUES(?, ?, ?, ?, ?, ?) %s;
        """.formatted(keyspace, ttl));

        similarParts = session.prepare("""
            SELECT text_id FROM %s.multi WHERE user_id = ? ORDER BY embedding ANN OF ? LIMIT ?;
        """.formatted(keyspace));

        byID = session.prepare("""
            SELECT text, url, embedding FROM %s.multi WHERE user_id = ? AND text_id = ?;
        """.formatted(keyspace));
    }

    public void saveAll(List<MultiEntity> entities) {
        entities.forEach(entity -> {
            var partID = new AtomicInteger();

            entity.embeddings().forEach(embedding -> {
                var boundInsertion = insertSentence.bind(entity.userID(), entity.textID(), partID.getAndIncrement(), embedding, entity.text(), entity.url());
                executor.submit(() -> session.execute(boundInsertion));
            });
        });
    }

    @Async
    public CompletableFuture<ResultSet> findByUserIDAndANN(String userID, CqlVector<Float> embedding, int limit) {
        var boundQuery = similarParts.bind(userID, embedding, limit);
        return CompletableFuture.completedFuture(session.execute(boundQuery));
    }

    @Async
    public CompletableFuture<ResultSet> findByPartialID(String userID, UUID textID) {
        return CompletableFuture.completedFuture(session.execute(byID.bind(userID, textID)));
    }
}
