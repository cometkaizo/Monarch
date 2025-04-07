package com.cometkaizo.monarch.structure.diagnostic;

import com.cometkaizo.util.Diagnostic;

public record UnknownVarErr(String name) implements Diagnostic {
    @Override
    public String getString() {
        return "Unknown variable '" + name + "'";
    }
}
