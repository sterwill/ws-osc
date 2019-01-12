package com.tinfig.wsosc;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class StringPrintStream extends PrintStream {
    public StringPrintStream() {
        super(new ByteArrayOutputStream(), true);
    }

    @Override
    public String toString() {
        return out.toString();
    }
}
