package com.sorintlab.itch;

import java.util.Arrays;

public class Configuration {
    private String []members;
    private int maxLogFileMegabytes;
    private int payloadBytes;
    private int heartbeatPeriodMs;

    private PrometheusConfig prometheus;

    private LoggingConfig logging;

    public PrometheusConfig getPrometheus() {
        return prometheus;
    }

    public void setPrometheus(PrometheusConfig prometheus) {
        this.prometheus = prometheus;
    }

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

    public LoggingConfig getLogging() {
        return logging;
    }

    public void setLogging(LoggingConfig logging) {
        this.logging = logging;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "members=" + Arrays.toString(members) +
                ", maxLogFileMegabytes=" + maxLogFileMegabytes +
                ", payloadBytes=" + payloadBytes +
                ", heartbeatPeriodMs=" + heartbeatPeriodMs +
                ", prometheus=" + prometheus +
                ", logging=" + logging +
                '}';
    }

    public static class PrometheusConfig {
        private boolean enabled;
        private int port;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "PrometheusConfig{" +
                    "enabled=" + enabled +
                    ", port=" + port +
                    '}';
        }
    }

    public static class LoggingConfig {
        private boolean fileEnabled;
        private boolean consoleEnabled;

        public boolean isFileEnabled() {
            return fileEnabled;
        }

        public void setFileEnabled(boolean fileEnabled) {
            this.fileEnabled = fileEnabled;
        }

        public boolean isConsoleEnabled() {
            return consoleEnabled;
        }

        public void setConsoleEnabled(boolean consoleEnabled) {
            this.consoleEnabled = consoleEnabled;
        }

        @Override
        public String toString() {
            return "LoggingConfig{" +
                    "fileEnabled=" + fileEnabled +
                    ", consoleEnabled=" + consoleEnabled +
                    '}';
        }
    }
}
