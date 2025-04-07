package com.cometkaizo.util;

public interface CheckedRunnable<T extends Throwable> {
    void run() throws T;
}
