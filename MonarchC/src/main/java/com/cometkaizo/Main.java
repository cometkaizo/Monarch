package com.cometkaizo;

import com.cometkaizo.monarch.MonarchDriver;

public class Main {

    private static final MonarchDriver DRIVER = new MonarchDriver();

    public static void main(String[] args) {
        DRIVER.start();
    }

    public static void stop() {
        DRIVER.stop();
        System.exit(0);
    }
}