package com.cometkaizo.monarch.structure.diagnostic;

import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.util.Diagnostic;

public record IncompatibleTypesErr(Type a, Type b) implements Diagnostic {
    @Override
    public String getString() {
        boolean sameNames = a != null && b != null && a.name().equals(b.name());
        String aFootprint = sameNames ? a.footprint().toPrettyString() : "";
        String bFootprint = sameNames ? b.footprint().toPrettyString() : "";
        return (a == null ? "void" : a.name() + aFootprint) + " and " +
                (b == null ? "void" : b.name() + bFootprint) + " are incompatible types";
    }
}
