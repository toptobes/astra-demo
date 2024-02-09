package org.datastax.vsdemo.indexing.utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

public class Prelude {
    public static <A, B> List<B> map(Function<A, B> f, List<A> list) {
        return list.stream().map(f).toList();
    }

    public static <A, B> Function<List<A>, List<B>> map(Function<A, B> f) {
        return list -> list.stream().map(f).toList();
    }

    public static <A, B, C> List<C> zipWith(BiFunction<A, B, C> f, List<A> listA, List<B> listB) {
        return IntStream.range(0, listA.size())
            .mapToObj(i -> f.apply(listA.get(i), listB.get(i)))
            .toList();
    }

    public static <A, B> List<B> mapAsync(Function<A, CompletableFuture<B>> f, List<A> list) {
        return map(CompletableFuture::join, map(f, list));
    }
}
