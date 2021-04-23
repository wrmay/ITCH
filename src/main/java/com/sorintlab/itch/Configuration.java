package com.sorintlab.itch;

import java.util.Arrays;

public class Configuration {
    private String []members;
    private int maxLogFileMegabytes;
    private int payloadBytes;
    private int heartbeatPeriodMs;

    public String[] getMembers() {
        return members;
    }

    public void setMembers(String[] members) {
        this.members = members;
    }

    public int getMaxLogFileMegabytes() {
        return maxLogFileMegabytes;
    }

    public void setMaxLogFileMegabytes(int maxLogFileMegabytes) {
        this.maxLogFileMegabytes = maxLogFileMegabytes;
    }

    public int getPayloadBytes() {
        return payloadBytes;
    }

    public void setPayloadBytes(int payloadBytes) {
        this.payloadBytes = payloadBytes;
    }

    public int getHeartbeatPeriodMs() {
        return heartbeatPeriodMs;
    }

    public void setHeartbeatPeriodMs(int heartbeatPeriodMs) {
        this.heartbeatPeriodMs = heartbeatPeriodMs;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "members=" + Arrays.toString(members) +
                ", maxLogFileMegabytes=" + maxLogFileMegabytes +
                ", payloadBytes=" + payloadBytes +
                ", heartbeatPeriodMs=" + heartbeatPeriodMs +
                '}';
    }
}
