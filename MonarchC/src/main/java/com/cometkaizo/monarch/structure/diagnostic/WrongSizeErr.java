package com.cometkaizo.monarch.structure.diagnostic;

import com.cometkaizo.util.Diagnostic;

public record WrongSizeErr(String msg) implements Diagnostic {
    @Override
    public String getString() {
        return msg;
    }
}
