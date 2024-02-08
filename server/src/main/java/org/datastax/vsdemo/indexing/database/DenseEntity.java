package org.datastax.vsdemo.indexing.database;

import com.datastax.oss.driver.api.core.data.CqlVector;

import java.util.UUID;

public record DenseEntity(String userID, UUID textID, CqlVector<Float> embedding, String text, String url) {}
