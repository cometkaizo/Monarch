package com.cometkaizo;

import com.cometkaizo.bytecode.Chunk;
import com.cometkaizo.monarch.structure.Func;

import java.io.IOException;
import java.nio.file.Path;

public class Test {
    // C:/Users/andyw/Documents/Dev/Java/Monarch/src/main/resources/main.mnrc
    public static void main(String[] args) throws IOException {
        Path target = Path.of("C:", "Users", "andyw", "Documents", "Dev", "Monarch", "MonarchC", "src", "main", "resources", "test.mnrc");
        var c = new Chunk();
        writeOps(c);
        c.writeTo(target);
    }
    private static void writeOps(Chunk c) {
        var after = c.createLabel();
        c.opPushPtr(after);

        // no args

        c.opPushPtrArr("main".getBytes());
        c.opPushPtrArr(Func.Interpreter.NAME.getBytes());
        c.opPushPtrArr("main.mnrc".getBytes());
        c.opJumpToUnit();

        c.writeLabel(after);
    }
    private static void writeTestOps(Chunk c) {
        c.opPushAll(0, 0, 0, 0, 2);
        c.opPushAll(0, 0, 0, 3);
        c.opDivide(4, 0, 5, 0);
        c.opPrint(4, 0);
    }
}
