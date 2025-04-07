package com.cometkaizo.command.nodes;

import com.cometkaizo.util.Diagnostic;
import com.cometkaizo.util.Triterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandContext {
    public int matchedDepth = -1, maxMatchedDepth = -1;
    public final Triterator<String> args;
    public final Map<String, Object> parsedArgs = new HashMap<>(1);
    public final List<Diagnostic> problems = new ArrayList<>(0);

    public CommandContext(String[] args) {
        this.args = Triterator.of(args);
    }

    public Object arg(String key) {
        return parsedArgs.get(key);
    }
    public String argStr(String key) {
        return (String) arg(key);
    }
    public Integer argInt(String key) {
        return (Integer) arg(key);
    }
    public Double argDouble(String key) {
        return (Double) arg(key);
    }
    public Boolean argBoolean(String key) {
        return (Boolean) arg(key);
    }
    public boolean hasArg(String key) {
        return parsedArgs.containsKey(key);
    }

    public void matched() {
        matchedDepth ++;
        if (matchedDepth > maxMatchedDepth) maxMatchedDepth = matchedDepth;
    }
}
