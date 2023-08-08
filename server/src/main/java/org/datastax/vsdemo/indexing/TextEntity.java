package org.datastax.vsdemo.indexing;

import com.datastax.oss.driver.api.core.data.CqlVector;

import java.util.UUID;

public record TextEntity(String userID, UUID textID, CqlVector<Float> embeddingE5SmallV2, String text, String url) {}
