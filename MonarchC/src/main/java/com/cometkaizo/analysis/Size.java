package com.cometkaizo.analysis;

public record Size(int byteAmt, int ptrAmt) {
    public static final Size ZERO = new Size(0, 0);
    public static final Size INVALID = new Size(Integer.MIN_VALUE, Integer.MIN_VALUE);
    public static final Size ONE_BYTE = new Size(1, 0);
    public static final Size ONE_PTR = new Size(0, 1);

    public Size plus(Size other) {
        return new Size(this.byteAmt + other.byteAmt, this.ptrAmt + other.ptrAmt);
    }
    public Size plus(int byteAmt, int ptrAmt) {
        return new Size(this.byteAmt + byteAmt, this.ptrAmt + ptrAmt);
    }
    public Size minus(Size other) {
        return new Size(this.byteAmt - other.byteAmt, this.ptrAmt - other.ptrAmt);
    }
    public Size minus(int byteAmt, int ptrAmt) {
        return new Size(this.byteAmt - byteAmt, this.ptrAmt - ptrAmt);
    }

    public boolean valid() {
        return this != INVALID;
    }

    public boolean isZero() {
        return byteAmt == 0 && ptrAmt == 0;
    }

    public static class Mutable {
        public int byteAmt, ptrAmt;
        public void add(Size other) {
            this.byteAmt += other.byteAmt;
            this.ptrAmt += other.ptrAmt;
        }
        public void add(int byteAmt, int ptrAmt) {
            this.byteAmt += byteAmt;
            this.ptrAmt += ptrAmt;
        }
        public void subtract(Size other) {
            this.byteAmt -= other.byteAmt;
            this.ptrAmt -= other.ptrAmt;
        }
        public void subtract(int byteAmt, int ptrAmt) {
            this.byteAmt -= byteAmt;
            this.ptrAmt -= ptrAmt;
        }
        public Size capture() {
            return new Size(byteAmt, ptrAmt);
        }
    }
}
