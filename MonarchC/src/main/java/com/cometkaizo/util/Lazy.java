package com.cometkaizo.util;

import com.cometkaizo.util.function.ThrowingSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class Lazy<T, E extends Exception> {
    protected volatile T value;
    protected final ThrowingSupplier<T, E> getter;
    protected final List<BiConsumer<? super T, ? super Exception>> listeners = new ArrayList<>(1);

    protected Lazy(ThrowingSupplier<T, E> getter) {
        this.getter = getter;
    }

    /**
     * Returns the value of this lazy object, or blocks the thread until it is computed. The value is computed at most once.
     * @return the lazily initialized value
     * @throws E If the computation fails.
     */
    public T get() throws E {
        if (value == null) {
            synchronized (this) {
                if (value == null) {
                    try {
                        value = getter.get();
                        listeners.forEach(l -> l.accept(value, null));
                    } catch (Exception e) {
                        listeners.forEach(l -> l.accept(null, e));
                        throw e;
                    }
                }
            }
        }
        return value;
    }
    /**
     * Returns the value of this lazy object, or blocks the thread until it is computed. The value is computed at most once.
     * @return the value
     * @throws RuntimeException If the computation fails. Contains the original exception as its cause.
     */
    public T getUnchecked() {
        try {
            return get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isInitialized() {
        return value != null;
    }

    public void addListener(BiConsumer<? super T, ? super Exception> listener) {
        listeners.add(listener);
    }
    public void removeListener(BiConsumer<? super T, ? super Exception> listener) {
        listeners.remove(listener);
    }

    public static <T, E extends Exception> Lazy<T, E> ofThrowing(ThrowingSupplier<T, E> getter) {
        return new Lazy<>(getter);
    }
    public static <T> Lazy<T, RuntimeException> of(Supplier<T> getter) {
        return ofThrowing(getter::get);
    }
    public static <T> Lazy<T, RuntimeException> direct(T value) {
        return ofThrowing(() -> value);
    }
}
