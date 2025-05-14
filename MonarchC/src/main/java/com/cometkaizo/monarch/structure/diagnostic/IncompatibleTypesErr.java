package com.cometkaizo.monarch.structure.diagnostic;

import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.util.Diagnostic;

public record IncompatibleTypesErr(Type a, Type b) implements Diagnostic {
    @Override
    public String getString() {
        return (a == null ? "void" : a.name()) + " and " + (b == null ? "void" : b.name()) + " are incompatible types";
    }
}
