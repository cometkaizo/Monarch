package com.cometkaizo.runtime.diagnostic;

import com.cometkaizo.util.Diagnostic;

public record NoEntryPointErr() implements Diagnostic {
    @Override
    public String getString() {
        return "Could not find entry point";
    }
}
