package com.cometkaizo.util.function;

import java.util.function.Supplier;

public interface ThrowingSupplier<R, E extends Exception> {
    R get() throws E;
    default R getOr(R defaultValue) {
        try {
            return get();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    static <R> Supplier<R> unchecked(ThrowingSupplier<R, ?> getter) {
        return () -> {
            try {
                return getter.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
    static <R> R getOr(ThrowingSupplier<R, ?> getter, R defaultValue) {
        return getter.getOr(defaultValue);
    }
}
