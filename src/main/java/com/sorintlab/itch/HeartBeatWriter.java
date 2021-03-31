package com.sorintlab.itch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class HeartBeatWriter implements Runnable {

    private enum State { READY, SENDING};

    private ByteBuffer byteBuffer;
    private SocketChannel channel;
    private State state;
    private String localAddress;
    private String remoteAddress;
    private int sequence;


    public HeartBeatWriter(SocketChannel channel) throws IOException {
        this.channel = channel;
        this.channel.configureBlocking(false);
        this.byteBuffer = ByteBuffer.allocate(2048);
        this.state = State.READY;
        this.localAddress = channel.getLocalAddress().toString();
        this.remoteAddress = channel.getRemoteAddress().toString();
        this.sequence = 1;
    }

    @Override
    public void run(){
        try {
            if (state == State.READY){
                HeartBeat hb = new HeartBeat();
                hb.setSender(this.localAddress);
                hb.setSendTime(System.currentTimeMillis());
                hb.setSequence(sequence++);
                byteBuffer.putChar('H');
                byteBuffer.putChar('B');
                hb.writeTo(byteBuffer);
                byteBuffer.flip();
                state = State.SENDING;
            }

            channel.write(byteBuffer);
            if (!byteBuffer.hasRemaining()){
                byteBuffer.clear();
                state = State.READY;
            } else {
                System.out.println("Warning: could not send all heartbeat bytes to " + remoteAddress + " no more heartbeats will be sent until this heartbeat can be written to the socket");
            }

        } catch(IOException x){
            System.out.println("An error occurred while attempting to send a heartbeat to " + remoteAddress + ": " + x.getMessage());
            state = State.READY;
        }
    }

    public void close(){
        try { 
            System.out.println("Closing socket to " + remoteAddress);
            channel.close();
        } catch(IOException x){
            System.out.println("Warning: an error occurred while closing a socket. " + x.getMessage());
        }
    }

}
