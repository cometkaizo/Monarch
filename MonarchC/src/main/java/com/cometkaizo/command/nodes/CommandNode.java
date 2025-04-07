package com.cometkaizo.command.nodes;

import com.cometkaizo.command.CommandSyntaxException;
import com.cometkaizo.command.arguments.*;
import com.cometkaizo.util.CollectionUtils;
import com.cometkaizo.util.Diagnostic;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A CommandNode represents a single action in a command (e.g. getting user input, translating user input, executing code, etc).
 * Specific functionality is specified by subclasses.
 *
 * @see LiteralCommandNode
 * @see ArgumentCommandNode
 * @see ActionCommandNode
 * @see ConditionalCommandNode
 */
public abstract class CommandNode {

    protected final List<CommandNode> subNodes;

    protected CommandNode(Builder builder) {
        this.subNodes = buildSubNodes(builder);
    }
    private static List<CommandNode> buildSubNodes(Builder builder) {
        return CollectionUtils.map(builder.subNodes, Builder::build);
    }

    public static CommandNode build(Function<Builder, ? extends Builder> builder) {
        var root = new EmptyCommandNode.Builder();
        builder.apply(root);
        return root.build();
    }


    /**
     * Executes this node's tasks, and potentially one of its sub-nodes depending on arguments provided by {@code context}.
     * If no sub-nodes can be executed or there are insufficient arguments, a CommandSyntaxException is thrown.
     * If multiple sub-nodes can be executed, it will execute the first one that accepts the next argument.
     * @param context the context to run this command in
     * @throws CommandSyntaxException If there are insufficient arguments, or an argument could not be parsed by any sub-nodes.
     */
    public boolean match(CommandContext context) throws CommandSyntaxException {
        int cursor = context.args.cursor();
        boolean matches = matchImpl(context);

        if (matches) {
            context.problems.clear();
            context.matched();

            if (hasSubNodes()) return subNodesMatch(context);
            else if (!context.args.hasNext()) return true;
            else context.problems.add(trailingArgsError(context));
        }
        context.args.jumpTo(cursor);
        if (!matches) context.problems.add(noMatchError(context));
        return false;
    }

    private boolean subNodesMatch(CommandContext context) throws CommandSyntaxException {
        int prevDepth = context.matchedDepth;
        try {
            for (var node : subNodes) {
                if (node.match(context)) return true;
            }
        } catch (NoSuchElementException e) {
            context.problems.add(endOfArgsError(e, context));
        }
        context.matchedDepth = prevDepth;
        return false;
    }


    protected abstract boolean matchImpl(CommandContext context);

    protected Diagnostic.Error trailingArgsError(CommandContext context) {
        var args = context.args;
        String formattedArgs = context.args.stream()
                .map(Objects::toString)
                .collect(Collectors.joining(" "));
        var matchedArgs = args.getListRange(0, args.cursor() + 1);
        int argsLength = String.join(" ", matchedArgs).length();

        return new Diagnostic.Error(
                "Unexpected trailing arguments: \n" + formattedArgs + "\n" +
                        " ".repeat(argsLength + 1) + "^\n"
        );
    }
    protected Diagnostic.Error endOfArgsError(Throwable e, CommandContext context) {
        String formattedArgs = context.args.stream()
                .map(Objects::toString)
                .collect(Collectors.joining(" "));
        String formattedSubNodes = subNodes.stream()
                .map(CommandNode::toPrettyString)
                .collect(Collectors.joining("\n or "));

        return new Diagnostic.Error(e,
                "Missing argument: \n" + formattedArgs + "\n" +
                " ".repeat(formattedArgs.length()) + "^\n" +
                "required: \n    " + formattedSubNodes
        );
    }
    protected Diagnostic.Error noMatchError(CommandContext context) {
        var matchedArgs = context.args.getListRange(0, context.args.cursor() + 1);
        int argsLength = String.join(" ", matchedArgs).length();
        String formattedArgs = context.args.stream()
                .map(Objects::toString)
                .collect(Collectors.joining(" "));

        return new Diagnostic.Error("Invalid argument:\n" +
                formattedArgs + "\n" +
                " ".repeat(argsLength) + "^\n" +
                "required: " + toPrettyString());
    }

    public String toPrettyString() {
        return getClass().getSimpleName().replaceAll("(?<=.)CommandNode$", "").toUpperCase();
    }

    public boolean hasSubNodes() {
        return !subNodes.isEmpty();
    }

    public abstract static class Builder {

        protected List<Builder> subNodes = new ArrayList<>(1);


        public abstract CommandNode build();

        public LiteralCommandNode.Builder literal(String literal) {
            return then(new LiteralCommandNode.Builder(literal));
        }
        public ArgumentCommandNode.Builder argInt(String name) {
            return arg(new IntArgument(name));
        }
        public ArgumentCommandNode.Builder argDouble(String name) {
            return arg(new DoubleArgument(name));
        }
        public ArgumentCommandNode.Builder argBoolean(String name) {
            return arg(new BooleanArgument(name));
        }
        public ArgumentCommandNode.Builder argStr(String name) {
            return arg(new StringArgument(name));
        }
        public ArgumentCommandNode.Builder arg(Argument arg) {
            return then(new ArgumentCommandNode.Builder(arg));
        }


        public ActionCommandNode.Builder executes(Consumer<? super CommandContext> task) {
            return then(new ActionCommandNode.Builder(task));
        }


        /**
         * Adds a sub-node to this node.
         * <strong>Important:</strong>
         * Unless you input the head of a node sequence, only the final node in the sequence will be added.
         * To easily add structures without using temporary variables, see {@link Builder#thenSeq(Function)} and {@link Builder#thenSeq(Function, Function[])}
         * @param node the node to add
         * @return the given node
         */
        public <T extends Builder> T then(T node) {
            this.subNodes.add(node);
            return node;
        }
        public EmptyCommandNode.Builder then(Builder... nodes) {
            var merge = new EmptyCommandNode.Builder();
            var subNodesList = Arrays.asList(nodes);

            subNodesList.forEach(n -> n.then(merge));
            this.subNodes.addAll(subNodesList);
            return merge;
        }

        /**
         * Adds a single sequence of nodes to this node. Example usage:
         * <pre>builder.then(n -> n.word().word());</pre>
         * The above code creates the sequence:
         * <pre>  builder
         * └─ word
         *    └─ word</pre>
         * @param adder the function to add the nodes
         * @return the last node in the sequence of nodes returned by adder
         */
        public <T extends Builder> T thenSeq(Function<Builder, T> adder) {
            return adder.apply(this);
        }

        /**
         * Adds one or multiple sequences of nodes to this node. Example usage:
         * <pre>builder.then(n -> n.word().word(), n -> n.literal().literal());</pre>
         * The above code creates the sequence:
         * <pre>  builder
         * ├─ word
         * |  └─ word
         * └─ literal
         *    └─ literal</pre>
         * @param adder the function to add the nodes
         * @return the last node in the sequence of nodes returned by adder
         */
        @SafeVarargs
        public final EmptyCommandNode.Builder thenSeq(Function<Builder, ? extends Builder> adder,
                                                      Function<Builder, ? extends Builder>... extra) {
            var merge = new EmptyCommandNode.Builder();
            thenSeq(adder).then(merge);
            for (var n : extra) thenSeq(n).then(merge);
            return merge;
        }

        public EmptyCommandNode.Builder empty() {
            return then(new EmptyCommandNode.Builder());
        }
    }
}
