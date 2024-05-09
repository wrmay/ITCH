package com.sorintlab.itch;

import io.prometheus.metrics.core.metrics.Gauge;

import java.io.IOException;

public class MachineStatsCollector implements Runnable{

    private final  Gauge loadAvg1m;
    private final String hostname;

    public MachineStatsCollector(String hostname){
        this.hostname = hostname;
        this.loadAvg1m = Gauge.builder()
                .name("load_average_1m")
                .help("load average of the past 1 minute")
                .labelNames("host")
                .register();

    }

    @Override
    public void run() {
        ProcessBuilder pb = new ProcessBuilder("cat","/proc/loadavg");
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        try {
            Process p = pb.start();
            int rc = p.waitFor();
            if (rc != 0){
                Itch.log.warning("cat /proc/loadavg returned a non-zero exit code: " + rc);
                return;
            }
            byte [] result = p.getInputStream().readAllBytes();
            String output = new String(result);
            String []words = output.split("\\s+");
            if(words.length > 0){
                double loadavg = Double.parseDouble(words[0]);
                Itch.log.info("1 minute load average: " + loadavg);
                loadAvg1m.labelValues(hostname).set(loadavg);
            } else {
                Itch.log.warning("An error occurred while parsing the output from \"cat /proc/loadavg\": " + output);
            }
        } catch (IOException iox){
            Itch.log.warning("An exception occurred while spawning a process: " + iox);
        } catch (InterruptedException e) {
            Itch.log.warning("MachineStatsCollector was interrupted");
        }
    }
}
