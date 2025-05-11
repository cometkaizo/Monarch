package com.cometkaizo.monarch;

import com.cometkaizo.command.CommandGroup;
import com.cometkaizo.launcher.app.App;
import com.cometkaizo.monarch.command.CompileCommand;
import com.cometkaizo.monarch.command.EntrypointCommand;
import com.cometkaizo.monarch.command.ExitCommand;
import com.cometkaizo.monarch.command.TestCommand;
import com.cometkaizo.monarch.structure.CompilationUnit;
import com.cometkaizo.util.Logger;

import java.io.File;

public class MonarchApp extends App {

    protected Compiler compiler = new Compiler();

    private final CommandGroup commandGroup = new CommandGroup(
            CompileCommand.create(this),
            EntrypointCommand.create(this),
            TestCommand.create(this),
            ExitCommand.create(this)
    );

    @Override
    public void setup() {
        super.setup();
        log("Monarch compiler");
        log("Working directory: " + System.getProperty("user.dir"));
    }

    public CompilationUnit.Analysis compile(String location) {
        File sourceFile = new File(location);
        if (!validateFile(sourceFile)) return null;
        log("Compiling '{}'...", sourceFile.getAbsolutePath());

        try {
            var result = compiler.compile(sourceFile);
            if (result.unit == null) {
                logger.red().log("Incorrect syntax");
                logger.red().log(result.syntaxCxt.syntaxProblem().orElseThrow().getString());
                return null;
            }
            // if (result.syntaxCxt.problems.logIfNotEmpty(logger)) return null;
            logger.green().log("No syntax problems found\n" + result.unit);

            if (result.analysisCtx.problems.logIfNotEmpty(logger)) return null;
            logger.green().log("No semantic problems found");

            if (result.chunk != null) logger.log(result.chunk);
            return result.unit;
        } catch (Exception e) {
            err(e);
            return null;
        }
    }

    private boolean validateFile(File sourceFile) {
        if (!sourceFile.exists() || sourceFile.isDirectory()) {
            log("'" + sourceFile + "' is not a file");
            return false;
        } else return true;
    }

    public Logger logger() {
        return logger;
    }

    public void parseInput(String input) {
        try {
            var problems = commandGroup.execute(input);
            problems.forEach(p -> log(p.getString()));
        } catch (Exception e) {
            err(e);
        }
    }


    @Override
    public MonarchSettings getDefaultSettings() {
        return new MonarchSettings();
    }
}
