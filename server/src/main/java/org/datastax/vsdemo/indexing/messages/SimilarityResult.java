package org.datastax.vsdemo.indexing.messages;

import java.util.List;

public record SimilarityResult(List<Single> dense, List<Single> multi) {
    public record Single(String text, String url) {}
}
