package com.cometkaizo.command.arguments;

import java.util.function.Predicate;

public class BooleanArgument extends Argument {
    public BooleanArgument(String name) {
        super(name);
    }
    public BooleanArgument(String name, Predicate<Boolean> requirement) {
        super(name, o -> requirement.test((Boolean) o));
    }

    @Override
    public boolean accepts(String string) {
        if (!string.equals("true") && !string.equals("false")) return false;
        boolean b = string.equals("true");
        return requirement.test(b);
    }

    @Override
    public Boolean translate(String string) throws IllegalArgumentException {
        if (!accepts(string)) throw new IllegalArgumentException();
        return string.equals("true");
    }
}
