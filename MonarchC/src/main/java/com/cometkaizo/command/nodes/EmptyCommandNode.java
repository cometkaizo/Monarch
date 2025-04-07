package com.cometkaizo.command.nodes;

import com.cometkaizo.util.StringUtils;

public class EmptyCommandNode extends CommandNode {

    protected EmptyCommandNode(Builder builder) {
        super(builder);
    }

    @Override
    protected boolean matchImpl(CommandContext context) {
        return true;
    }

    @Override
    public String toString() {
        return StringUtils.format("EmptyCommandNode{}");
    }

    public static class Builder extends CommandNode.Builder {

        @Override
        public EmptyCommandNode build() {
            return new EmptyCommandNode(this);
        }
    }
}
