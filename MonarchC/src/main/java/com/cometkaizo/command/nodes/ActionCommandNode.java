package com.cometkaizo.command.nodes;

import java.util.function.Consumer;

public class ActionCommandNode extends CommandNode {
    protected final Consumer<? super CommandContext> task;

    protected ActionCommandNode(Builder builder) {
        super(builder);
        this.task = builder.task;
    }

    @Override
    protected boolean matchImpl(CommandContext context) {
        task.accept(context);
        return true;
    }

    public static class Builder extends CommandNode.Builder {
        protected final Consumer<? super CommandContext> task;

        public Builder(Consumer<? super CommandContext> task) {
            this.task = task;
        }

        @Override
        public CommandNode build() {
            return new ActionCommandNode(this);
        }
    }
}
