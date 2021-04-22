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

    private HeartBeatFactory heartBeatFactory;


    public HeartBeatWriter(SocketChannel channel, HeartBeatFactory heartBeatFactory) throws IOException {
        this.heartBeatFactory = heartBeatFactory;
        this.channel = channel;
        this.channel.configureBlocking(false);
        this.state = State.READY;
        this.localAddress = channel.getLocalAddress().toString();
        this.remoteAddress = channel.getRemoteAddress().toString();
        this.sequence = 1;

        // this one is only for sizing purposes
        HeartBeat testhb = heartBeatFactory.newHeartBeat();
        testhb.setSender(this.localAddress);
        long bufSize = testhb.serializedSize();

        Itch.log.fine("HearBeatWriter allocated " + bufSize + " byte buffer");
        this.byteBuffer = ByteBuffer.allocate((int) bufSize + 2 + 2 + 4);  // +2 (H) +2 (B) +4 (int bytes)
    }

    /*
       Creates a new Heartbeat, serializes it to the byte buffer and attempts to write the buffer to
       the socket channel.  If all of the serialized data cannot be written to the socket channel then
       then the unsent portion will remain in the byte buffer and the next time this is called, it will
       attempt to send the remaining bytes.  No new heartbeat will be generated until the previous one
       has been fully written to the socket channel.
     */
    @Override
    public void run(){
        Itch.log.fine("Running HeartBeatWriter for " + this.remoteAddress);
        try {
            if (state == State.READY){
                HeartBeat hb = heartBeatFactory.newHeartBeat();
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

        } catch(Exception x){
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
