package org.datastax.vsdemo.indexing;

import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.CqlVector;
import com.datastax.oss.driver.shaded.guava.common.collect.ImmutableList;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.datastax.vsdemo.indexing.database.MultiEntity;
import org.datastax.vsdemo.indexing.database.MultiRepository;
import org.datastax.vsdemo.indexing.messages.IndexRequest;
import org.datastax.vsdemo.indexing.messages.SimilarityResult;
import org.datastax.vsdemo.indexing.records.EmbeddedPassage;
import org.datastax.vsdemo.indexing.records.EmbeddedQuery;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.datastax.vsdemo.indexing.utils.Prelude.*;

@Service
public class MultiService {
    private final MultiRepository multiRepository;

    public MultiService(MultiRepository multiRepository) {
        this.multiRepository = multiRepository;
    }

    public void indexPassages(String userID, List<IndexRequest> request, EmbeddedPassage embeddingResult) {
        multiRepository.saveAll(zipWith(
            (eq, req) -> new MultiEntity(userID, UUID.randomUUID(), eq, req.text(), req.url()),
            embeddingResult.multi(),
            request
        ));
    }

    private record Score(UUID id, float score) {}

    @Async
    public CompletableFuture<List<SimilarityResult.Single>> findSimilarSentences(String userID, EmbeddedQuery embeddedQuery, int limit) {
        var eqs = embeddedQuery.multi();

        // Fetches the top k/2 documents for each v in Eq using ANN
        var uniqueIDs = mapAsync(
            v -> multiRepository.findByUserIDAndANN(userID, v, limit / 2),
            eqs
        ).stream()
            .flatMap(r -> r.map(row -> row.getUuid("text_id")).all().stream())
            .distinct()
            .toList();

        // Fetches the full document for each of the unique IDs
        var similar = mapAsync(id -> multiRepository.findByPartialID(userID, id), uniqueIDs);

        // TreeMap to keep the results sorted by score (in an ID record to avoid collisions)
        var scored = new TreeMap<Score, SimilarityResult.Single>(Comparator.comparing(Score::score).thenComparing(Score::id).reversed());

        for (int i = 0; i < similar.size(); i++) {
            var rows = similar.get(i).all();

            // sum(maxsim(qv, embeddings_for_part) for qv in query_encodings)
            var maxSims = new float[eqs.size()];

            for (int j = 0; j < eqs.size(); j++) {
                maxSims[j] = maxSim(eqs.get(j), rows);
            }

            var sum = sumReduce(maxSims);

            // Scores each documents
            var score = new Score(uniqueIDs.get(i), sum);
            var single = new SimilarityResult.Single(rows.getFirst().getString("text"), rows.getFirst().getString("url"));

            scored.put(score, single);
        }

        // Returns the top k documents
        return CompletableFuture.completedFuture(
            scored.values().stream().limit(limit).toList()
        );
    }

    @SuppressWarnings("unchecked")
    private float maxSim(CqlVector<Float> query, List<Row> rows) {
        var passage = map(row -> (CqlVector<Float>) row.getCqlVector("embedding"), rows);

        var sims = new float[passage.size()];

        for (int i = 0; i < passage.size(); i++) {
            sims[i] = dot(listToArr(query), listToArr(passage.get(i)));
        }

        return max(sims);
    }

    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

    private static float dot(float[] a, float[] b) {
        var upperBound = SPECIES.loopBound(a.length);
        var vc = FloatVector.zero(SPECIES);
        var i = 0;

        for (; i < upperBound; i += SPECIES.length()) {
            var va = FloatVector.fromArray(SPECIES, a, i);
            var vb = FloatVector.fromArray(SPECIES, b, i);
            vc = va.fma(vb, vc);
        }

        var c = vc.reduceLanes(VectorOperators.ADD);

        for (; i < a.length; i++) {
            c += a[i] * b[i];
        }

        return c;
    }

    private static float max(float[] arr) {
        var upperBound = SPECIES.loopBound(arr.length);
        var maxes = FloatVector.zero(SPECIES);
        var i = 0;

        for (; i < upperBound; i += SPECIES.length()) {
            var v = FloatVector.fromArray(SPECIES, arr, i);
            maxes = maxes.max(v);
        }

        var max = maxes.reduceLanes(VectorOperators.MAX);

        for (; i < arr.length; i++) {
            max = Math.max(max, arr[i]);
        }

        return max;
    }

    private static float sumReduce(float[] arr) {
        var upperBound = SPECIES.loopBound(arr.length);
        var sum = 0f;
        var i = 0;

        for (; i < upperBound; i += SPECIES.length()) {
            var v = FloatVector.fromArray(SPECIES, arr, i);
            sum += v.reduceLanes(VectorOperators.ADD);
        }

        for (; i < arr.length; i++) {
            sum += arr[i];
        }

        return sum;
    }

    private static float[] listToArr(CqlVector<Float> floats) {
        var asList = (ImmutableList<Float>) floats.getValues();
        var array = new float[asList.size()];

        for (int i = 0; i < array.length; i++) {
            array[i] = asList.get(i);
        }

        return array;
    }
}
