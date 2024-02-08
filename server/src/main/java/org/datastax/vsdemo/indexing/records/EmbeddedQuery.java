package org.datastax.vsdemo.indexing.records;

import com.datastax.oss.driver.api.core.data.CqlVector;

import java.util.List;

public record EmbeddedQuery(
    CqlVector<Float> dense,
    List<CqlVector<Float>> multi
) {}
