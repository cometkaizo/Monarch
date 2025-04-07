package com.cometkaizo.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ExceptionHandler {
    ExceptionHandler PRINT = new ExceptionHandler() {
        @Override
        public Throwable handleException(Exception e) {
            System.err.println("Encountered exception");
            e.printStackTrace(System.err);
            return null;
        }

        @Override
        public Error handleError(Error e) {
            System.err.println("Encountered fatal exception");
            e.printStackTrace(System.err);
            return e;
        }
    };

    Throwable handleException(Exception e);
    Throwable handleError(Error e);

    default void handle(Throwable t) {
        if (t instanceof Exception e) {
            Throwable newEx = handleException(e);
            if (newEx != null) throw newEx instanceof RuntimeException r ? r : new RuntimeException(newEx);
        } else if (t instanceof Error e) {
            Throwable newEx = handleError(e);
            if (newEx != null) throw newEx instanceof RuntimeException r ? r : new RuntimeException(newEx);
            throw e;
        }
    }

    static <T> Supplier<T> orNull(Callable<T> task) {
        return () -> {
            try {
                return task.call();
            } catch (Exception e) {
                PRINT.handle(e);
                return null;
            }
        };
    }

    static <T> T call(Callable<T> task, Consumer<? super Throwable> exceptionHandler) {
        try {
            return task.call();
        } catch (Exception e) {
            exceptionHandler.accept(e);
            return null;
        } catch (Throwable e) {
            exceptionHandler.accept(e);
            throw e;
        }
    }

    static void run(CheckedRunnable<? extends Throwable> task, Consumer<? super Throwable> exceptionHandler) {
        try {
            task.run();
        } catch (Throwable e) {
            exceptionHandler.accept(e);
        }
    }
}
