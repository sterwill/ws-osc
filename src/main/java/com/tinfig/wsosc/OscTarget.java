package com.tinfig.wsosc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OscTarget {
    public String address;
    public int port;

    public OscTarget() {
    }

    public OscTarget(String address, int port) {
        this.address = address;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OscTarget oscTarget = (OscTarget) o;
        return port == oscTarget.port &&
                Objects.equals(address, oscTarget.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }
}
