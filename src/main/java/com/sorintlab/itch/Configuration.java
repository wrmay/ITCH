package com.sorintlab.itch;

import java.util.Arrays;

public class Configuration {
    private String []members;

    public String[] getMembers() {
        return members;
    }

    public void setMembers(String[] members) {
        this.members = members;
    }

    @Override
    public String toString() {
        return "Configuration [members=" + Arrays.toString(members) + "]";
    }

    
}
