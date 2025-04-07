package com.cometkaizo.parser;

import com.cometkaizo.util.Diagnostic;
import com.cometkaizo.util.DiagnosticList;

public class Context {
    public final DiagnosticList problems;

    public Context() {
        this(new DiagnosticList(1));
    }
    public Context(DiagnosticList problems) {
        this.problems = problems;
    }

    public void report(Diagnostic diagnostic) {
        problems.add(diagnostic);
    }
}
