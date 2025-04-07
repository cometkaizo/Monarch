package com.cometkaizo.command.nodes;

import com.cometkaizo.util.StringUtils;

import java.util.function.Supplier;

public class ConditionalCommandNode extends CommandNode {

    protected final Supplier<Boolean> condition;
    protected final String name;

    public ConditionalCommandNode(Builder builder) {
        super(builder);
        this.condition = builder.condition;
        this.name = builder.name;
    }

    @Override
    protected boolean matchImpl(CommandContext context) {
        return condition.get();
    }

    @Override
    public String toString() {
        return StringUtils.format("""
                ConditionalCommandNode{
                    condition: {}
                }""", condition);
    }

    @Override
    public String toPrettyString() {
        return name;
    }

    public static class Builder extends CommandNode.Builder {

        protected final Supplier<Boolean> condition;
        protected final String name;

        public Builder(Supplier<Boolean> condition) {
            this(condition, "CONDITIONAL");
        }

        public Builder(Supplier<Boolean> condition, String name) {
            this.condition = condition;
            this.name = name;
        }

        @Override
        public ConditionalCommandNode build() {
            return new ConditionalCommandNode(this);
        }
    }
}
