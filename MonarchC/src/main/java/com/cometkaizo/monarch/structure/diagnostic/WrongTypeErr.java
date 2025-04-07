package com.cometkaizo.monarch.structure.diagnostic;

import com.cometkaizo.util.Diagnostic;

import static com.cometkaizo.util.StringUtils.aOrAn;

public record WrongTypeErr(String name, String requiredType) implements Diagnostic {
    @Override
    public String getString() {
        return name + " must be " + aOrAn(requiredType);
    }
}
