package com.cometkaizo.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class DiagnosticList extends ArrayList<Diagnostic> {
    public DiagnosticList(int initialCapacity) {
        super(initialCapacity);
    }
    public DiagnosticList() {
    }
    public DiagnosticList(Collection<? extends Diagnostic> c) {
        super(c);
    }

    public void err(Throwable e, String msg) {
        add(new Diagnostic.Error(e, msg));
    }
    public void err(String msg) {
        add(new Diagnostic.Error(msg));
    }
    public void warn(Throwable e, String msg) {
        add(new Diagnostic.Warning(e, msg));
    }
    public void warn(String msg) {
        add(new Diagnostic.Warning(msg));
    }

    @Override
    public String toString() {
        return stream().map(Diagnostic::getString).collect(Collectors.joining("\n\n"));
    }

    public boolean logIfNotEmpty(Logger logger) {
        boolean notEmpty = !isEmpty();
        if (notEmpty) logger.red().log(toString());
        return notEmpty;
    }
}
