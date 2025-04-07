package com.cometkaizo.util.function;

public interface ThrowingFunction<T, R, E extends Exception> {
    R apply(T t) throws E;

    static <T, E extends RuntimeException> ThrowingFunction<T, T, E> identity() {
        return t -> t;
    }
}
