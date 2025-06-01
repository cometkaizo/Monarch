package com.cometkaizo;

import java.util.List;

public class Test2 {
    public static void main(String[] args) throws Exception {

    }
    static List<?> a() {
        return null;
    }
    static List<?> b() {
        return a();
    }
}
