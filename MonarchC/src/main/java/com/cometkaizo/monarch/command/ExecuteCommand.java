package com.cometkaizo.monarch.command;

import com.cometkaizo.command.nodes.CommandContext;
import com.cometkaizo.command.nodes.CommandNode;
import com.cometkaizo.monarch.MonarchApp;

public class ExecuteCommand {
    public static CommandNode create(MonarchApp app) {
        return CommandNode.build(root -> root
                .literal("execute")
                .argStr("location")
                .executes(ctx -> run(ctx))
        );

    }

    private static void run(CommandContext context) {/*
        WaveSourceCode sourceCode = app.getCompiledSourceFiles().get((String) parsedArgs.get("location"));

        if (sourceCode == null) {
            LogUtils.error("Source code location '{}' cannot be found", parsedArgs.get("location"));
            return;
        }

        sourceCode.execute();*/
    }

}
