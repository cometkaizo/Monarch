package com.cometkaizo.monarch.structure.diagnostic;

import com.cometkaizo.util.Diagnostic;

public record DuplicateParserErr(String name) implements Diagnostic {
    @Override
    public String getString() {
        return "Duplicate parser '" + name + "'";
    }
}
