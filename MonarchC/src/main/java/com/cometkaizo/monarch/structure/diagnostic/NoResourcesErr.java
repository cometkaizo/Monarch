package com.cometkaizo.monarch.structure.diagnostic;

import com.cometkaizo.util.Diagnostic;

public record NoResourcesErr(String name) implements Diagnostic {
    @Override
    public String getString() {
        return name + " must have access to resources";
    }
}
