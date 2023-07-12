package org.datastax.vsdemo.indexing.messages;

public record SimilarityRequest(String query, int limit) {}
