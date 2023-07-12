package org.datastax.vsdemo.indexing.messages;

public record SimilarityResult(String text, String url, double similarity) {}
