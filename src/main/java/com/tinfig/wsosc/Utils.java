package com.tinfig.wsosc;

public class Utils {
    public static String toString(Throwable t) {
        StringPrintStream sps = new StringPrintStream();
        t.printStackTrace(sps);
        return sps.toString();
    }
}
