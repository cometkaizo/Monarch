package com.cometkaizo.command.nodes;

import com.cometkaizo.command.arguments.Argument;
import com.cometkaizo.util.StringUtils;

public class ArgumentCommandNode extends CommandNode {
    private final Argument argument;

    public ArgumentCommandNode(Builder builder) {
        super(builder);
        this.argument = builder.argument;
    }

    @Override
    protected boolean matchImpl(CommandContext context) {
        var arg = context.args.safeNext();
        if (arg.isEmpty() || !argument.accepts(arg.get())) return false;
        context.parsedArgs.put(argument.getName(), argument.translate(arg.get()));
        return true;
    }

    @Override
    public String toString() {
        return StringUtils.format("""
                ArgumentCommandNode{
                    argument: {}
                }""",
                argument);
    }

    @Override
    public String toPrettyString() {
        return argument.toPrettyString() + " ARGUMENT";
    }

    public static class Builder extends CommandNode.Builder {

        protected final Argument argument;

        public Builder(Argument argument) {
            this.argument = argument;
        }

        @Override
        public ArgumentCommandNode build() {
            return new ArgumentCommandNode(this);
        }

        @Override
        public String toString() {
            return StringUtils.format("""
                    ArgumentCommandNodeBuilder{
                        argument: {}
                    }""", argument);
        }
    }
}
