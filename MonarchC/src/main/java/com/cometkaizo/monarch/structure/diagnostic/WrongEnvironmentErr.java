package com.cometkaizo.monarch.structure.diagnostic;

import com.cometkaizo.util.Diagnostic;

public record WrongEnvironmentErr(String name, String environment) implements Diagnostic {
    @Override
    public String getString() {
        return name + " must be in a " + environment + " environment";
    }
}
