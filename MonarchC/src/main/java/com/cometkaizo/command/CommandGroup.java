package com.cometkaizo.command;

import com.cometkaizo.command.nodes.CommandContext;
import com.cometkaizo.command.nodes.CommandNode;
import com.cometkaizo.util.Diagnostic;

import java.util.ArrayList;
import java.util.List;

public class CommandGroup {

    private final List<CommandNode> commands;

    public CommandGroup(CommandNode... commands) {
        this.commands = List.of(commands);
    }

    public List<Diagnostic> execute(String input) {
        List<Diagnostic> problems = new ArrayList<>(1);
        validateInput(input, problems);
        if (!problems.isEmpty()) return problems;
        int maxMatchedDepth = 0;

        // getting inputted command information
        String[] args = getParts(input);

        for (var command : commands) {
            var context = new CommandContext(args);
            boolean success = command.match(context);
            if (success) return context.problems;
            if (context.maxMatchedDepth >= maxMatchedDepth) {
                if (context.maxMatchedDepth > maxMatchedDepth) problems.clear();
                problems.addAll(context.problems);
                maxMatchedDepth = context.maxMatchedDepth;
            }
        }
        return problems;
    }

    private static String[] getParts(String input) {
        return input.trim().split(" ");
    }

    private static void validateInput(String input, List<Diagnostic> problems) {
        if (input == null) problems.add(new Diagnostic.Error("Command cannot be null"));
        else if (input.isBlank()) problems.add(new Diagnostic.Error("Command cannot be blank"));
    }

}
