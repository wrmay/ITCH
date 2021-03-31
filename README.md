# Independent Test of Cluster Health

This tool tests connectivity between a collection of machines.  Each machine simply 
opens a socket to every other machine in the collection and writes "heartbeat" 
messages.  At the same time, each machine records the heartbeats that it receives.  Output is sent 
to stdout.  An example is below.

```
HeartBeat [ SENDER=/192.168.1.103:48202 at 2021-03-31 15:27:28.122 GMT RECEIVER=/192.168.1.101:5701 at 2021-03-31 15:27:36.739 GMT SEQ=78 ]
HeartBeat [ SENDER=/192.168.1.102:33552 at 2021-03-31 15:27:36.728 GMT RECEIVER=/192.168.1.101:5701 at 2021-03-31 15:27:37.738 GMT SEQ=133 ]
HeartBeat [ SENDER=/192.168.1.103:48202 at 2021-03-31 15:27:28.122 GMT RECEIVER=/192.168.1.101:5701 at 2021-03-31 15:27:37.739 GMT SEQ=79 ]
HeartBeat [ SENDER=/192.168.1.102:33552 at 2021-03-31 15:27:37.730 GMT RECEIVER=/192.168.1.101:5701 at 2021-03-31 15:27:38.738 GMT SEQ=134 ]
HeartBeat [ SENDER=/192.168.1.103:48202 at 2021-03-31 15:27:28.122 GMT RECEIVER=/192.168.1.101:5701 at 2021-03-31 15:27:38.739 GMT SEQ=80 ]
HeartBeat [ SENDER=/192.168.1.102:33552 at 2021-03-31 15:27:38.728 GMT RECEIVER=/192.168.1.101:5701 at 2021-03-31 15:27:39.739 GMT SEQ=135 ]
HeartBeat [ SENDER=/192.168.1.103:48202 at 2021-03-31 15:27:28.122 GMT RECEIVER=/192.168.1.101:5701 at 2021-03-31 15:27:39.739 GMT SEQ=81 ]
HeartBeat [ SENDER=/192.168.1.102:33552 at 2021-03-31 15:27:39.728 GMT RECEIVER=/192.168.1.101:5701 at 2021-03-31 15:27:40.739 GMT SEQ=136 ]
```

This provides an independent record of connectivity between nodes during a period of time.

# Heartbeat Content
- identity of the sender
- identity of the recipient
- timestamp when sent
- timestamp when received (added by the recipient upon receipt)
- sequence number (sender plus recipient plus sequence # uniquely identifies a heartbeat message) 

# Configuration
Place a file called itch.yaml in the working directory. The content should be 
similar to the example given below.  Note that the members must be specified with an IP address.  This 
value is used as the bind address on the local machines and as the connection endpoint for remote machines.
Itch automatically detects which member it is running on.  If Itch is run on a machine where none of the 
listed IPs are local, it will print an error message and exit.

```yaml
members:
    - 192.168.1.101
    - 192.168.1.102
    - 192.168.1.103
```

# Design

For sending the hearbeats, a scheduled thread pool will write the hearbeats  to a socket channel in async mode.  

For receiving heartbeats, a ServerSocketChannel will be created and a thread will be periodically scheduled to receive 
the heartbeats from the channels and write a message.

There will also be a thread that periodically wakes up and checks the last time a hearbeat was received from every 
other member.  If the heartbeat is delayed more than 10 seconds a record will be written.


# Enhancements

1. Use a logging framework ?
