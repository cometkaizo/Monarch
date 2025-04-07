package com.cometkaizo.monarch.command;

import com.cometkaizo.command.nodes.CommandContext;
import com.cometkaizo.command.nodes.CommandNode;
import com.cometkaizo.monarch.MonarchApp;

public class TestCommand {

    public static CommandNode create(MonarchApp app) {
            return CommandNode.build(root -> root
                    .literal("test")
                    .thenSeq(
                            n -> n.argStr("flags"),
                            n -> n.empty()
                    )
                    .executes(ctx -> compile(ctx, app)));
    }

    private static void compile(CommandContext context, MonarchApp app) {
        configure(context);
        app.compile("src/main/resources/main.txt");
    }


    private static void configure(CommandContext context) {
        if (context.hasArg("flags")) context.argStr("flags");
    }
}
