package com.sorintlab.itch;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HeartBeat {
    private String sender;
    private String receiver;
    private long sendTime;
    private long receiveTime;
    private int sequence;
    private byte []payload;


    public  HeartBeat(){
        // to reduce serialization complexity, null values are not allowed in sender and receiver
        sender = "";
        receiver = "";
    }

    public String getSender() {
        return sender;
    }
    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public long getSendTime() {
        return sendTime;
    }
    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }

    public long getReceiveTime() {
        return receiveTime;
    }
    public void setReceiveTime(long receiveTime) {
        this.receiveTime = receiveTime;
    }

    public int getSequence() {
        return sequence;
    }
    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public byte[] getPayload() {
        return payload;
    }
    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    /*
     Note: it is the caller's responsibility to make sure that there is sufficient data
     in the buffer to read the entire heartbeat
    */
    public static HeartBeat readFrom(ByteBuffer buffer){
        // its ok not to use a factory here because we will learn the payload size and content
        // from the serialzed bytes
        HeartBeat result = new HeartBeat();

        int senderBytes = buffer.getInt();
        if (senderBytes > 0){
            result.sender = new String(buffer.array(), buffer.arrayOffset() + buffer.position(), senderBytes);
            buffer.position(buffer.position() + senderBytes);
        } else {
            // it should already have this value, but just in case ...
            result.sender = "";
        }

        int receiverBytes = buffer.getInt();
        if (receiverBytes > 0){
            result.receiver = new String(buffer.array(), buffer.arrayOffset() + buffer.position(), receiverBytes);
            buffer.position(buffer.position() + receiverBytes);
        } else {
            result.receiver = "";
        }

        result.sendTime = buffer.getLong();
        result.receiveTime = buffer.getLong();
        result.sequence = buffer.getInt();

        int payloadSize = buffer.getInt();
        byte []payload = new byte[payloadSize];

        buffer.get(payload);
        result.setPayload(payload);

        return result;
    }

    public int serializedSize(){
        return serializedSize(sender.getBytes().length, receiver.getBytes().length);
    }

    private int serializedSize(int senderBytes, int receiverBytes){
        return 4 + senderBytes + 4 + receiverBytes  + 8 + 8 + 4 + 4 + payload.length;
    }

   /*
    Writes the size in bytes, then the data - the buffer must have sufficient capacity!
   */
   public  void writeTo(ByteBuffer buffer){
       int senderBytes = 0;
       int receiverBytes = 0;

       byte []senderBuf = null;
       byte []receiverBuf = null;

        if (sender.length() > 0){
            senderBuf = sender.getBytes();
            senderBytes = senderBuf.length;
        }

        if (receiver.length() > 0){
            receiverBuf = receiver.getBytes();
            receiverBytes = receiverBuf.length;
        }

        int size = serializedSize(senderBytes, receiverBytes);
        buffer.putInt(size);

        buffer.putInt(senderBytes);
        if (senderBytes > 0) buffer.put(senderBuf);

        buffer.putInt(receiverBytes);
        if (receiverBytes > 0) buffer.put(receiverBuf);

        buffer.putLong(sendTime);
        buffer.putLong(receiveTime);
        buffer.putInt(sequence);

        buffer.putInt(payload.length);
        buffer.put(payload);
   }

   @Override
   public String toString(){
       DateFormat fmt = new SimpleDateFormat(Itch.TIMESTAMP_FORMAT);
       return "HeartBeat [ SENDER=" + sender + " at " + fmt.format(new Date(sendTime)) +
               " RECEIVER=" + receiver + " at " + fmt.format(new Date(receiveTime)) +
               " TIME DELTA=" + (receiveTime - sendTime) + "ms SEQ=" + sequence +
               " PAYLOAD BYTES=" + payload.length + " ]";
   }


}
