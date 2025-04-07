package com.cometkaizo.bytecode;

import com.cometkaizo.analysis.Size;
import com.cometkaizo.util.CollectionUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Chunk {
    private static final int INSN_INDEX_SIZE = 4;
    protected final List<Info> info = new ArrayList<>();

    public void opReturn() {
        writeData(0x00);
    }
    public void opDebug() {
        writeData(0x01);
    }
    public void opPrint(Size len) {
        opPrint(len.byteAmt(), len.ptrAmt());
    }
    public void opPrint(int byteLen, int ptrLen) {
        writeData(0x02, byteLen, ptrLen);
    }
    public void opScan() {
        writeData(0x03);
    }
    public void opTime() {
        writeData(0x04);
    }

    public void opPush(int b) {
        writeData(0x05, b);
    }
    public void opPushAll(Info.Label dest) {
        writeData(0x06, INSN_INDEX_SIZE);
        writeLabelRef(dest);
    }
    public void opPushAll(int... bytes) {
        writeData(0x06, bytes.length);
        writeData(bytes);
    }
    public void opPushAll(byte... bytes) {
        writeData(0x06, bytes.length);
        writeData(bytes);
    }
    public void opPop() {
        writeData(0x07);
    }
    public void opPopAll(Size amt) {
        writeData(0x08, amt.byteAmt(), amt.ptrAmt());
    }
    public void opPushThis() {
        writeData(0x09);
    }
    public void opPushUnit() {
        writeData(0x0A);
    }
    public void opPushUnitOf() {
        writeData(0x0B);
    }
    public void opPushPtrNext() {
        writeData(0x0C);
    }
    public void opPushPtrArr(byte... bytes) {
        writeData(0x0D, bytes.length);
        writeData(bytes);
    }
    public void opPushPtr(Info.Label dest) {
        writeData(0x0E);
        writeLabelRef(dest);
    }
    public void opCopy(Size off, Size size) {
        opCopy(off.byteAmt(), off.ptrAmt(), size.byteAmt(), size.ptrAmt());
    }
    public void opCopy(int byteOff, int ptrOff, int byteSize, int ptrSize) {
        writeData(0x0F, byteOff, ptrOff, byteSize, ptrSize);
    }
    public void opMove(Size off, Size size) {
        opMove(off.byteAmt(), off.ptrAmt(), size.byteAmt(), size.ptrAmt());
    }
    public void opMove(int byteOff, int ptrOff, int byteSize, int ptrSize) {
        writeData(0x10, byteOff, ptrOff, byteSize, ptrSize);
    }
    public void opSet(Size size, Size off) {
        opSet(size.byteAmt(), size.ptrAmt(), off.byteAmt(), off.ptrAmt());
    }
    public void opSet(int byteSize, int ptrSize, int byteOff, int ptrOff) {
        writeData(0x11, byteSize, ptrSize, byteOff, ptrOff);
    }

    public void opAdd(Size a, Size b) {
        opAdd(a.byteAmt(), a.ptrAmt(), b.byteAmt(), b.ptrAmt());
    }
    public void opAdd(int aByteSize, int aPtrSize, int bByteSize, int bPtrSize) {
        writeData(0x12, aByteSize, aPtrSize, bByteSize, bPtrSize);
    }
    public void opMultiply(Size a, Size b) {
        opMultiply(a.byteAmt(), a.ptrAmt(), b.byteAmt(), b.ptrAmt());
    }
    public void opMultiply(int aByteSize, int aPtrSize, int bByteSize, int bPtrSize) {
        writeData(0x13, aByteSize, aPtrSize, bByteSize, bPtrSize);
    }
    public void opOr(Size a, Size b) {
        opOr(a.byteAmt(), a.ptrAmt(), b.byteAmt(), b.ptrAmt());
    }
    public void opOr(int aByteSize, int aPtrSize, int bByteSize, int bPtrSize) {
        writeData(0x14, aByteSize, aPtrSize, bByteSize, bPtrSize);
    }
    public void opAnd(Size a, Size b) {
        opAnd(a.byteAmt(), a.ptrAmt(), b.byteAmt(), b.ptrAmt());
    }
    public void opAnd(int aByteSize, int aPtrSize, int bByteSize, int bPtrSize) {
        writeData(0x15, aByteSize, aPtrSize, bByteSize, bPtrSize);
    }
    public void opXor(Size a, Size b) {
        opXor(a.byteAmt(), a.ptrAmt(), b.byteAmt(), b.ptrAmt());
    }
    public void opXor(int aByteSize, int aPtrSize, int bByteSize, int bPtrSize) {
        writeData(0x16, aByteSize, aPtrSize, bByteSize, bPtrSize);
    }
    public void opLShift(Size a) {
        opLShift(a.byteAmt(), a.ptrAmt());
    }
    public void opLShift(int aByteSize, int aPtrSize) {
        writeData(0x17, aByteSize, aPtrSize);
    }
    public void opRShift(Size a) {
        opRShift(a.byteAmt(), a.ptrAmt());
    }
    public void opRShift(int aByteSize, int aPtrSize) {
        writeData(0x18, aByteSize, aPtrSize);
    }

    public void opEquals(Size a, Size b) {
        opEquals(a.byteAmt(), a.ptrAmt(), b.byteAmt(), b.ptrAmt());
    }
    public void opEquals(int aByteSize, int aPtrSize, int bByteSize, int bPtrSize) {
        writeData(0x19, aByteSize, aPtrSize, bByteSize, bPtrSize);
    }
    public void opGreater(Size a, Size b) {
        opGreater(a.byteAmt(), a.ptrAmt(), b.byteAmt(), b.ptrAmt());
    }
    public void opGreater(int aByteSize, int aPtrSize, int bByteSize, int bPtrSize) {
        writeData(0x1A, aByteSize, aPtrSize, bByteSize, bPtrSize);
    }

    public void opMalloc() {
        writeData(0x1B);
    }
    public void opMSet() {
        writeData(0x1C);
    }
    public void opMGet() {
        writeData(0x1D);
    }
    public void opFree() {
        writeData(0x1E);
    }
    public void opJumpToIndex(Info.Label dest) {
        opPushAll(dest);
        writeData(0x1F);
    }
    public void opJumpForward() {
        writeData(0x20);
    }
    public void opJumpBackward() {
        writeData(0x21);
    }
    public void opJumpIf(Info.Label dest) {
        opPushAll(dest);
        writeData(0x22);
    }
    public void opJumpArrSwitch(Collection<Info.Label> branches, Collection<byte[]> keys) {
        opJumpArrSwitch(branches.toArray(Info.Label[]::new), keys.toArray(byte[][]::new));
    }
    public void opJumpArrSwitch(Info.Label[] branches, byte[][] keys) {
        if (branches.length != keys.length) throw new IllegalArgumentException();
        writeData(0x23, branches.length);
        for (var branch : branches) writeLabelRef(branch);
        for (var key : keys) {
            writeData(key.length);
            writeData(key);
        }
    }
    public void opJumpSwitch(Collection<Info.Label> branches) {
        opJumpSwitch(branches.toArray(Info.Label[]::new));
    }
    public void opJumpSwitch(Info.Label... branches) {
        writeData(0x24, branches.length);
        for (var branch : branches) writeLabelRef(branch);
    }
    public void opJumpToUnit() {
        writeData(0x25);
    }
    public void opJumpToPtr() {
        writeData(0x26);
    }

    public void opStructCreate() {
        writeData(0x27);
    }
    public void opStructEntry(int entryIndex) {
        writeData(0x28, entryIndex);
    }
    public void opStructEntrySize(int entryIndex) {
        writeData(0x29, entryIndex);
    }
    public void opStructSet(int entryIndex) {
        writeData(0x2A, entryIndex);
    }
    public void opStructGet(int entryIndex) {
        writeData(0x2B, entryIndex);
    }
    public void opMapCreate() {
        writeData(0x2C);
    }
    public void opMapEntry() {
        writeData(0x2D);
    }
    public void opMapSet() {
        writeData(0x2E);
    }
    public void opMapGet() {
        writeData(0x2F);
    }

    public Info.Label createLabel() {
        return new Info.Label();
    }

    public void writeLabel(Info.Label label) {
        writeInfo(label);
    }

    public void writeLabelRef(Info.Label label) {
        writeInfo(new Info.LabelRef(label));
    }

    public void writeData(int... bytes) {
        for (int b : bytes) info.add(new Info.Data((byte) b));
    }
    public void writeData(byte... bytes) {
        for (byte b : bytes) info.add(new Info.Data(b));
    }

    private void writeInfo(Info... info) {
        this.info.addAll(Arrays.asList(info));
    }

    public void writeTo(Path target) throws IOException {
        try (var out = new BufferedOutputStream(Files.newOutputStream(target))) {
            writeTo(out);
            out.flush();
        }
    }
    public void writeTo(OutputStream out) throws IOException {
        resolveLabelAddresses();
        for (Info i : info) writeTo(i, out);
    }
    public byte[] toByteArray() {
        try {
            var out = new ByteArrayOutputStream(info.size());
            writeTo(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void resolveLabelAddresses() {
        int dataCount = 0;
        for (Info i : info) {
            switch (i) {
                case Info.Data d -> dataCount ++;
                case Info.LabelRef r -> dataCount += INSN_INDEX_SIZE;
                case Info.Label l -> l.address = dataCount;
            }
        }
    }

    private void writeTo(Info i, OutputStream out) throws IOException {
        switch (i) {
            case Info.Data d -> out.write(d.value);
            case Info.LabelRef r -> {
                var a = r.label.address;
                out.write(a >>> 8 * 3);
                out.write(a >>> 8 * 2);
                out.write(a >>> 8);
                out.write(a);
            }
            case Info.Label l -> {}
        }
    }

    public Chunk plus(Chunk other) {
        var chunk = new Chunk();
        chunk.info.addAll(this.info);
        chunk.info.addAll(other.info);
        return chunk;
    }

    @Override
    public String toString() {
        return "Chunk:\n" + CollectionUtils.stream(toByteArray()).map(Integer::toHexString).collect(Collectors.joining(" "));
    }

    public sealed interface Info {
        final class Data implements Info {
            private final byte value;
            public Data(byte b) {
                this.value = b;
            }
        }
        final class Label implements Info {
            private int address;
            public Label() {}
        }
        final class LabelRef implements Info {
            private final Label label;
            public LabelRef(Label label) {
                this.label = label;
            }
        }
    }


    public static class JumpArrSwitchBuilder {
        private final List<byte[]> keys = new ArrayList<>();
        private final List<Chunk.Info.Label> locations = new ArrayList<>();
        public void addBranch(String key, Chunk.Info.Label location) {
            addBranch(key.getBytes(), location);
        }
        public void addBranch(byte[] key, Chunk.Info.Label location) {
            keys.add(key);
            locations.add(location);
        }
        public void apply(Chunk c) {
            c.opJumpArrSwitch(locations, keys);
        }
    }
}
