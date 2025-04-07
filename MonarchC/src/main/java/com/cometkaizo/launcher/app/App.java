package com.cometkaizo.launcher.app;

import com.cometkaizo.util.Logger;

public abstract class App {

    private final AppSettings settings;
    protected Logger logger = new Logger();

    protected App() {
        this.settings = getDefaultSettings();
    }


    public abstract AppSettings getDefaultSettings();

    public void setup() {

    }

    public void cleanup() {

    }


    public AppSettings settings() {
        return settings;
    }


    public void log(String message) {
        logger.log(message);
    }
    public void log(String message, Object... args) {
        logger.log(message, args);
    }
    public void log(Object... args) {
        logger.log(args);
    }
    public void err(String message) {
        logger.err(message);
    }
    public void err(Throwable t, String message) {
        logger.err(t, message);
    }
    public void err(String message, Object... args) {
        logger.err(message, args);
    }
    public void err(Throwable t, Object... args) {
        logger.err(t, args);
    }
    public void err(Throwable t, String message, Object... args) {
        logger.err(t, message, args);
    }
    public void err(Object... args) {
        logger.err(args);
    }

}
