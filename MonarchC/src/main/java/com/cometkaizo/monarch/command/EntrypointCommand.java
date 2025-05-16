package com.cometkaizo.monarch.command;

import com.cometkaizo.bytecode.Chunk;
import com.cometkaizo.command.nodes.CommandContext;
import com.cometkaizo.command.nodes.CommandNode;
import com.cometkaizo.monarch.MonarchApp;
import com.cometkaizo.monarch.structure.Func;
import com.cometkaizo.util.StringUtils;

import java.io.File;
import java.io.IOException;

public class EntrypointCommand {

    public static CommandNode create(MonarchApp app) {
            return CommandNode.build(root -> root
                    .literal("entrypoint")
                    .argStr("location")
                    .argStr("function_name")
                    .executes(ctx -> compile(ctx, app)));
    }

    private static void compile(CommandContext context, MonarchApp app) {
        try {
            var entryFile = new File(context.argStr("location"));
            if (validateFile(entryFile, app)) return;
            var entryFunctionName = context.argStr("function_name");

            writeOps(entryFile, entryFunctionName);
            app.log("Created entrypoint file to function '" + entryFunctionName + "' in '" + entryFile + "'");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean validateFile(File entryFile, MonarchApp app) {
        if (!entryFile.exists() || entryFile.isDirectory()) {
            app.log("'" + entryFile + "' is not a file");
            return true;
        }
        return false;
    }

    private static void writeOps(File entryFile, String entryFunctionName) throws IOException {
        var entryFileName = StringUtils.nameNoExt(entryFile);
        var entrypointName = entryFileName + "entry.mnrc";
        Chunk c = new Chunk();

        var after = c.createLabel();
        c.opPushPtr(after);

        // no args

        c.opPushPtrArr(entryFunctionName.getBytes());
        c.opPushPtrArr(Func.Interpreter.NAME.getBytes());
        c.opPushPtrArr(entryFile.getName().getBytes());
        c.opJumpToUnit();

        c.writeLabel(after);

        c.writeTo(entryFile.toPath().resolveSibling(entrypointName));
    }
}
