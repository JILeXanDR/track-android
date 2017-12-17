package com.jilexandr.trackyourbus;

import java.util.logging.Level;

class MyLevel extends Level {
    public static final Level DISASTER = new MyLevel("DISASTER", Level.SEVERE.intValue() + 1);

    public MyLevel(String name, int value) {
        super(name, value);
    }
}
