package com.cometkaizo.util.function;

public interface ThrowingConsumer<T, E extends Exception> {
    void accept(T t) throws E;
}
