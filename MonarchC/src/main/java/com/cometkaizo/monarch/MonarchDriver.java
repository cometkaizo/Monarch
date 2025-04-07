package com.cometkaizo.monarch;

import com.cometkaizo.launcher.driver.Driver;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class MonarchDriver extends Driver {

    private static final MonarchApp app = new MonarchApp();

    public MonarchDriver() {
        super(app);
        addLoop(new Runnable() {
            final Scanner scanner = new Scanner(getConsoleIn());
            @Override
            public void run() {
                if (scanner.hasNextLine()) {
                    app.parseInput(scanner.nextLine());
                }
            }
        }, 300, TimeUnit.MILLISECONDS);
    }

}
