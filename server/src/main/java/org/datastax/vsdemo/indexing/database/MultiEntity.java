package org.datastax.vsdemo.indexing.database;

import com.datastax.oss.driver.api.core.data.CqlVector;

import java.util.List;
import java.util.UUID;

public record MultiEntity(String userID, UUID textID, List<CqlVector<Float>> embeddings, String text, String url) {}
