package com.cometkaizo.monarch.command;

import com.cometkaizo.command.nodes.CommandContext;
import com.cometkaizo.command.nodes.CommandNode;
import com.cometkaizo.monarch.MonarchApp;

public class CompileCommand {

    public static CommandNode create(MonarchApp app) {
            return CommandNode.build(root -> root
                    .literal("compile")
                    .argStr("location")
                    .thenSeq(
                            n -> n.argStr("flags"),
                            n -> n.empty()
                    )
                    .executes(ctx -> compile(ctx, app)));
    }

    private static void compile(CommandContext context, MonarchApp app) {
        configure(context);
        app.compile(context.argStr("location"));
    }

    private static void configure(CommandContext context) {
        context.arg("flags");
    }
}
