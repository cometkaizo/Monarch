package com.cometkaizo.monarch.structure.diagnostic;

import com.cometkaizo.util.Diagnostic;

public record DuplicateVarErr(String name) implements Diagnostic {
    @Override
    public String getString() {
        return "Duplicate variable '" + name + "'";
    }
}
