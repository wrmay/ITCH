package com.sorintlab.itch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

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

                // only setting the receiver for logging purposes
                // the heartbeat is written to the send buffer without a receiver because it is
                // filled in by the receiver upon receipt
                hb.setReceiver(remoteAddress);
                Itch.log.info("SENT " + hb);
            }

            channel.write(byteBuffer);
            if (!byteBuffer.hasRemaining()){
                byteBuffer.clear();
                state = State.READY;
            } else {
                Itch.log.warning("Could not send all heartbeat bytes to " + remoteAddress + " no more heartbeats will be sent until this heartbeat can be written to the socket");
            }

        } catch(IOException x){
            Itch.log.log(Level.WARNING, "An error occurred while attempting to send a heartbeat to " + remoteAddress ,x);
            state = State.READY;
        }
    }

    public void close(){
        try { 
            Itch.log.info("Closing socket to " + remoteAddress);
            channel.close();
        } catch(IOException x){
            Itch.log.warning("Warning: an error occurred while closing a socket. " + x.getMessage());
        }
    }

}
