package org.datastax.vsdemo.indexing.records;

import com.datastax.oss.driver.api.core.data.CqlVector;

import java.util.List;

public record EmbeddedPassage(
    List<CqlVector<Float>> dense,
    List<List<CqlVector<Float>>> multi
) {}
