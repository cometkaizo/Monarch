package com.cometkaizo.util;

import java.io.PrintStream;

public class Logger {
    protected PrintStream out, err;

    public Logger() {
        this(System.out, System.err);
    }

    public Logger(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    /**
     * Returns a logger equal to this one except for the style
     * @param styleCode the style to log all messages in
     * @see ConsoleStyles
     */
    public Logger style(String styleCode) {
        return new Styled(styleCode);
    }
    public Logger black() {
        return style(ConsoleStyles.BLACK);
    }
    public Logger red() {
        return style(ConsoleStyles.RED);
    }
    public Logger green() {
        return style(ConsoleStyles.GREEN);
    }
    public Logger yellow() {
        return style(ConsoleStyles.YELLOW);
    }
    public Logger blue() {
        return style(ConsoleStyles.BLUE);
    }
    public Logger purple() {
        return style(ConsoleStyles.PURPLE);
    }
    public Logger cyan() {
        return style(ConsoleStyles.CYAN);
    }
    public Logger white() {
        return style(ConsoleStyles.WHITE);
    }

    public void log(String message) {
        out.println(message);
    }
    public void log(String message, Object... args) {
        if (message == null) log(args);
        else log(StringUtils.format(message, args));
    }
    public void log(Object... args) {
        log(StringUtils.join(args));
    }
    public void logP(String message) {
        out.print(message);
    }
    public void logP(String message, Object... args) {
        if (message == null) logP(args);
        else logP(StringUtils.format(message, args));
    }
    public void logP(Object... args) {
        logP(StringUtils.join(args));
    }
    public void err(String message) {
        err.println(message);
    }
    public void err(Throwable t, String message) {
        err(message);
        if (t != null) t.printStackTrace(err);
    }
    public void err(String message, Object... args) {
        if (message == null) err(args);
        else err(StringUtils.format(message, args));
    }
    public void err(Throwable t, Object... args) {
        err(t, StringUtils.join(args));
    }
    public void err(Throwable t, String message, Object... args) {
        if (message == null) err(t, args);
        else err(t, StringUtils.format(message, args));
    }
    public void err(Object... args) {
        err(StringUtils.join(args));
    }

    protected class Styled extends Logger {
        protected final String style;
        protected Styled(String style) {
            this.style = style;
        }

        @Override
        public void log(String message) {
            Logger.this.log(style + message + ConsoleStyles.RESET);
        }
        @Override
        public void logP(String message) {
            Logger.this.logP(style + message + ConsoleStyles.RESET);
        }
        @Override
        public void err(String message) {
            Logger.this.err(style + message + ConsoleStyles.RESET);
        }
    }

    public PrintStream outStream() {
        return out;
    }
    public PrintStream errStream() {
        return err;
    }
    public void setOutStream(PrintStream out) {
        this.out = out;
    }
    public void setErrStream(PrintStream err) {
        this.err = err;
    }
}
