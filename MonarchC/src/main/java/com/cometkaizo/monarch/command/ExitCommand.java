package com.cometkaizo.monarch.command;

import com.cometkaizo.Main;
import com.cometkaizo.command.nodes.CommandContext;
import com.cometkaizo.command.nodes.CommandNode;
import com.cometkaizo.monarch.MonarchApp;

public class ExitCommand {

    public static CommandNode create(MonarchApp app) {
            return CommandNode.build(root -> root
                    .literal("exit")
                    .executes(ctx -> exit(ctx, app)));
    }

    private static void exit(CommandContext context, MonarchApp app) {
        Main.stop();
    }
}
