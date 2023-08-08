package org.datastax.vsdemo.indexing;

import com.datastax.oss.driver.api.core.data.CqlVector;
import com.datastax.oss.driver.shaded.guava.common.collect.ImmutableList;

import java.util.List;

public class VectorUtils {
    public static double testSimilarity(CqlVector<Float> vecA, CqlVector<Float> vecB) {
        var listA = (ImmutableList<Float>) vecA.getValues();
        var listB = (ImmutableList<Float>) vecB.getValues();

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < listA.size(); i++) {
            dotProduct += listA.get(i) * listB.get(i);
            normA += Math.pow(listA.get(i), 2);
            normB += Math.pow(listB.get(i), 2);
        }

        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        return Math.max(similarity, 0);
    }

    @SuppressWarnings("unchecked")
    public static CqlVector<Float> listToCqlVector(List<Float> list) {
        return CqlVector.builder().addAll(list).build();
    }
}
