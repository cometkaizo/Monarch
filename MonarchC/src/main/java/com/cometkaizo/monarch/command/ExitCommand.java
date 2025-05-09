package com.cometkaizo.monarch.command;

import com.cometkaizo.Main;
import com.cometkaizo.command.nodes.CommandContext;
import com.cometkaizo.command.nodes.CommandNode;
import com.cometkaizo.monarch.MonarchApp;

public class ExitCommand {

    public static CommandNode create(MonarchApp app) {
            return CommandNode.build(root -> root
                    .literal("exit")
                    .executes(ctx -> compile(ctx, app)));
    }

    private static void compile(CommandContext context, MonarchApp app) {
        Main.stop();
    }
}
