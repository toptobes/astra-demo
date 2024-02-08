package org.datastax.vsdemo.indexing.database;

import com.datastax.astra.sdk.AstraClient;
import com.datastax.oss.driver.api.core.CqlSession;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MultiTableInitializer {
    private final CqlSession session;

    @Value("${astra.cql.driver-config.basic.session-keyspace}")
    private String keyspace;

    @Value("${astra-demo.embedding-service.dims.multi}")
    private int dims;

    public MultiTableInitializer(AstraClient astra) {
        this.session = astra.cqlSession();
    }

    @PostConstruct
    public void initialize() {
        this.session.execute("""
            CREATE TABLE IF NOT EXISTS %s.multi (
                user_id text,
                text_id uuid,
                part_id int,
                embedding vector<float, %d>,
                text text,
                url text,
                PRIMARY KEY (user_id, text_id, part_id)
            )
        """.formatted(keyspace, dims));

        this.session.execute("""
            CREATE CUSTOM INDEX IF NOT EXISTS multi_ann_index ON %s.multi (embedding) USING 'StorageAttachedIndex'
                WITH OPTIONS = { 'similarity_function': 'DOT_PRODUCT' }
        """.formatted(keyspace));
    }
}
