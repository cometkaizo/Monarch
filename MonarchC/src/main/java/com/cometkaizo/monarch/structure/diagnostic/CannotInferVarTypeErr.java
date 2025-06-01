package com.cometkaizo.monarch.structure.diagnostic;

import com.cometkaizo.util.Diagnostic;

public record CannotInferVarTypeErr() implements Diagnostic {
    @Override
    public String getString() {
        return "variable type cannot be inferred; either a type or an initializer must be specified";
    }
}
