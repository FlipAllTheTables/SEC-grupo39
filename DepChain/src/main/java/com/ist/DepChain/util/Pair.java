package com.ist.DepChain.util;

public class Pair {
    public int value1;
    public String value2;

    public Pair(int value1, String value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    @Override
    public String toString() {
        return "{" + value1 + ", " + value2 + "}";
    }

}