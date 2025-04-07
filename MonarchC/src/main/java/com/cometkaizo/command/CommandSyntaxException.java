package com.cometkaizo.command;

public class CommandSyntaxException extends RuntimeException {
    public CommandSyntaxException(String message) {
        super(message);
    }

}
