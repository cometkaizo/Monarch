package com.cometkaizo.launcher.driver;

import com.cometkaizo.launcher.app.App;
import com.cometkaizo.util.ExceptionHandler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public abstract class Driver {

    private final App app;


    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final List<ScheduledFuture<?>> tasks = new ArrayList<>(1);
    private boolean isRunning = false;

    protected Driver(App app) {
        this.app = app;
    }

    public static InputStream getConsoleIn() {
        return System.in;
    }

    public void start() {
        if (isRunning) throw new IllegalStateException("Driver has already been started");

        setup();

        isRunning = true;
    }

    public void stop() {
        if (!isRunning) throw new IllegalStateException("Driver is not started");

        cleanup();
        tasks.forEach(loop -> loop.cancel(false));
        tasks.clear();

        isRunning = false;
    }

    protected void setup() {

    }

    protected void cleanup() {

    }


    protected final void addLoop(Runnable task, long period, TimeUnit unit, ExceptionHandler exceptionHandler) {
        addTask(executor.scheduleAtFixedRate(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                exceptionHandler.handle(t);
            }
        }, 0, period, unit));
    }

    protected final void addLoop(Runnable task, long period, TimeUnit unit) {
        addLoop(task, period, unit, ExceptionHandler.PRINT);
    }

    protected final void addTask(ScheduledFuture<?> task) {
        tasks.add(task);
    }

    public App getApp() {
        return app;
    }
}
