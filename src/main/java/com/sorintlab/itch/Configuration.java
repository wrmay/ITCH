package com.sorintlab.itch;

import java.util.Arrays;

public class Configuration {
    private String []members;
    private int maxLogFileMegabytes;

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

    @Override
    public String toString() {
        return "Configuration{" +
                "members=" + Arrays.toString(members) +
                ", maxLogFileMegabytes=" + maxLogFileMegabytes +
                '}';
    }
}
