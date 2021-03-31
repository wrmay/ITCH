package com.sorintlab.itch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class HeartBeatReader implements Runnable {

    private enum State { AWAITING_HB, AWAITING_SIZE, AWAITING_DATA, HEARTBEAT_READY};

    private ByteBuffer byteBuffer;
    private SocketChannel channel;
    private HeartBeat heartbeat;
    private State state;
    private int expectedBytes;
    private String localAddress;
    private String remoteAddress;

    public HeartBeatReader(SocketChannel channel) throws IOException {
        this.channel = channel;
        this.channel.configureBlocking(false);
        this.byteBuffer = ByteBuffer.allocate(2048);
        this.heartbeat = null;
        this.state = State.AWAITING_HB;
        this.localAddress = channel.getLocalAddress().toString();
        this.remoteAddress = channel.getRemoteAddress().toString();
    }

    @Override
    public void run(){
        try {
            if (this.read()){
                HeartBeat hb = this.getHeartBeat();
                hb.setReceiver(localAddress);
                hb.setReceiveTime(System.currentTimeMillis());
                System.out.println(hb.toString());
            }
        } catch(IOException iox){
            System.out.println("An error occurred while reading a HearBeat from " + remoteAddress + ": "  + iox.getMessage());
        }
    }

    public void close(){
        try { 
            System.out.println("Closing socket from " + channel.getRemoteAddress());
            channel.close();
        } catch(IOException x){
            System.out.println("Warning: an error occurred while closing a socket. " + x.getMessage());
        }
    }

    /*
        If a HeartBeat is ready and hasn't been taken yet, don't read the socket, just return true.

       Read whatever is available in the socket (may be nothing).
       
       If less than 4 bytes are availbe, return false.   
       
       Otherwise read 2 chars which should be 'H' and 'B'.  If they are not then throw those 4 bytes away by reading 
       them from the buffer and compacting it, then return false. 
       
       If the expected chars are found, if less than 4 more bytes are available, return false.  Otherwise, read an 
       integer indicating the size in bytes of the heartbeat payload.  
       
       If less than those number of additional bytes is available, return false.  Otherwise, read all fields 
       of the heartbeat and return true;

    */
    public boolean read() throws IOException {
        if (state == State.HEARTBEAT_READY) {
            return true;
        } else {
            if (channel.read(byteBuffer) > 0){
                byteBuffer.flip();
                while(true){
                    if (state == State.AWAITING_HB){
                        if (byteBuffer.remaining() < 4) break;

                        char H = byteBuffer.getChar();
                        char B = byteBuffer.getChar();
                        if (H != 'H' || B != 'B'){
                            System.out.println("Warning: Unexpected data found in place of HB marker");
                        } else {
                            state = State.AWAITING_SIZE;
                        }
                    } else if ( state == State.AWAITING_SIZE ){
                        if (byteBuffer.remaining() < 4) break;

                        expectedBytes = byteBuffer.getInt();
                        if (expectedBytes <= 0){
                            System.out.println("Warning: negative value found in heartbeat size header. Ignoring this heartbeat");
                            state = State.AWAITING_HB;
                        } else {
                            state = State.AWAITING_DATA;
                        }
                    } else if (state == State.AWAITING_DATA){
                        if (byteBuffer.remaining() < expectedBytes ) break;
                    
                        heartbeat = HeartBeat.readFrom(byteBuffer);
                        if (heartbeat == null){
                            System.out.println("An error occurred while reading a HeartBeat from the socket.  Ignoring this heartbeat.");
                            state = State.AWAITING_HB;
                        } else {
                            state = State.HEARTBEAT_READY;
                            break;
                        }
                    }
                }
                byteBuffer.compact();
            }
            return state == State.HEARTBEAT_READY;
        }
    }

    /**
     * Should only be called when there is a heartbeat ready
     */
    public HeartBeat getHeartBeat(){
        if (state == State.HEARTBEAT_READY){
            HeartBeat result = heartbeat;
            this.heartbeat = null;
            this.state = State.AWAITING_HB;
            return result;
        } else {
            return null;
        }
    }
}
