package org.datastax.vsdemo.indexing;

import com.datastax.oss.driver.api.core.data.CqlVector;

import java.util.UUID;

public record TextEntity(String userID, UUID textID, CqlVector<Float> embedding, String text, String url) {}
