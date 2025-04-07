package com.cometkaizo.command.arguments;

import java.util.function.Predicate;

public abstract class ObjectArgument extends Argument {

    public ObjectArgument(String name) {
        super(name);
    }
    public ObjectArgument(String name, Predicate<Object> requirement) {
        super(name, requirement);
    }

}
