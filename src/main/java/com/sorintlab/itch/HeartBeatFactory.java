package com.sorintlab.itch;

import java.util.Arrays;

public class HeartBeatFactory {
    private int payloadSize;
    private byte []payload;

    public HeartBeatFactory(int payloadSize){
        this.payloadSize = payloadSize;
        this.payload = new byte[payloadSize];
        Arrays.fill(payload, (byte) 0xff);
    }

    public HeartBeat newHeartBeat(){
        HeartBeat result = new HeartBeat();
        result.setPayload(payload);
        return result;
    }
}
