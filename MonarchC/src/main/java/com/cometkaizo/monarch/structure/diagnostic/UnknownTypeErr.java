package com.cometkaizo.monarch.structure.diagnostic;

import com.cometkaizo.util.Diagnostic;

public record UnknownTypeErr(String name) implements Diagnostic {
    @Override
    public String getString() {
        return "Unknown type '" + name + "'";
    }
}
