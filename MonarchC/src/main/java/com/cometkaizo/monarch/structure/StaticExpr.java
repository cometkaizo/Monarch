package com.cometkaizo.monarch.structure;

import java.util.Optional;

public interface StaticExpr<T> {
    T staticEvaluate();
    default <U> Optional<U> staticEvaluate(Class<U> type) {
        T t = staticEvaluate();
        if (type.isAssignableFrom(t.getClass())) return Optional.of(type.cast(t));
        else return Optional.empty();
    }
}
