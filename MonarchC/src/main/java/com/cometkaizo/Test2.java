package com.cometkaizo;

public class Test2 {
    public static void main(String[] args) throws Exception {
        int value = Integer.MIN_VALUE;
        for (int i = 31; i >= 0; i--) {
            System.out.print((value & 1<<i) == 0 ? "0" : "1");
        }
        System.out.println();

        System.out.println(Integer.toBinaryString(value));
    }
}
