# Independent Test of Cluster Health

This tool tests connectivity between a collection of machines.  Each machine simply 
opens a socket to every other machine in the collection and writes "heartbeat" 
messages.  At the same time, each machine records the heartbeats that it receives.  Output is sent 
to a file named itch_<hostname>.log in the working directory.

```
2021-04-07 19:27:44.002 GMT INFO : listening on /192.168.1.101:5701
2021-04-07 19:27:44.014 GMT INFO : Established connection to /192.168.1.102:5701
2021-04-07 19:27:44.017 GMT INFO : Established connection to /192.168.1.103:5701
2021-04-07 19:27:44.986 GMT INFO : Received a new connection from /192.168.1.103:60990
2021-04-07 19:27:44.993 GMT INFO : Received a new connection from /192.168.1.102:49114
2021-04-07 19:27:45.018 GMT INFO : SENT HeartBeat [ SENDER=/192.168.1.101:56358 at 2021-04-07 19:27:45.017 GMT RECEIVER=/192.168.1.102:5701 at 1970-01-01 00:00:00.000 GMT SEQ=1 ]
2021-04-07 19:27:45.018 GMT INFO : SENT HeartBeat [ SENDER=/192.168.1.101:60486 at 2021-04-07 19:27:45.018 GMT RECEIVER=/192.168.1.103:5701 at 1970-01-01 00:00:00.000 GMT SEQ=1 ]
2021-04-07 19:27:46.017 GMT INFO : SENT HeartBeat [ SENDER=/192.168.1.101:56358 at 2021-04-07 19:27:46.016 GMT RECEIVER=/192.168.1.102:5701 at 1970-01-01 00:00:00.000 GMT SEQ=2 ]
2021-04-07 19:27:46.018 GMT INFO : SENT HeartBeat [ SENDER=/192.168.1.101:60486 at 2021-04-07 19:27:46.018 GMT RECEIVER=/192.168.1.103:5701 at 1970-01-01 00:00:00.000 GMT SEQ=2 ]
2021-04-07 19:27:46.989 GMT INFO : RECEIVED HeartBeat [ SENDER=/192.168.1.103:60990 at 2021-04-07 19:27:45.991 GMT RECEIVER=/192.168.1.101:5701 at 2021-04-07 19:27:46.989 GMT SEQ=1 ]
2021-04-07 19:27:46.995 GMT INFO : RECEIVED HeartBeat [ SENDER=/192.168.1.102:49114 at 2021-04-07 19:27:45.996 GMT RECEIVER=/192.168.1.101:5701 at 2021-04-07 19:27:46.994 GMT SEQ=1 ]
2021-04-07 19:27:47.017 GMT INFO : SENT HeartBeat [ SENDER=/192.168.1.101:56358 at 2021-04-07 19:27:47.016 GMT RECEIVER=/192.168.1.102:5701 at 1970-01-01 00:00:00.000 GMT SEQ=3 ]
2021-04-07 19:27:47.018 GMT INFO : SENT HeartBeat [ SENDER=/192.168.1.101:60486 at 2021-04-07 19:27:47.018 GMT RECEIVER=/192.168.1.103:5701 at 1970-01-01 00:00:00.000 GMT SEQ=3 ]
2021-04-07 19:27:47.989 GMT INFO : RECEIVED HeartBeat [ SENDER=/192.168.1.103:60990 at 2021-04-07 19:27:46.989 GMT RECEIVER=/192.168.1.101:5701 at 2021-04-07 19:27:47.989 GMT SEQ=2 ]
2021-04-07 19:27:47.994 GMT INFO : RECEIVED HeartBeat [ SENDER=/192.168.1.102:49114 at 2021-04-07 19:27:46.995 GMT RECEIVER=/192.168.1.101:5701 at 2021-04-07 19:27:47.994 GMT SEQ=2 ]
2021-04-07 19:27:48.017 GMT INFO : SENT HeartBeat [ SENDER=/192.168.1.101:56358 at 2021-04-07 19:27:48.017 GMT RECEIVER=/192.168.1.102:5701 at 1970-01-01 00:00:00.000 GMT SEQ=4 ]
2021-04-07 19:27:48.019 GMT INFO : SENT HeartBeat [ SENDER=/192.168.1.101:60486 at 2021-04-07 19:27:48.018 GMT RECEIVER=/192.168.1.103:5701 at 1970-01-01 00:00:00.000 GMT SEQ=4 ]
2021-04-07 19:27:48.990 GMT INFO : RECEIVED HeartBeat [ SENDER=/192.168.1.103:60990 at 2021-04-07 19:27:47.989 GMT RECEIVER=/192.168.1.101:5701 at 2021-04-07 19:27:48.990 GMT SEQ=3 ]
2021-04-07 19:27:48.995 GMT INFO : RECEIVED HeartBeat [ SENDER=/192.168.1.102:49114 at 2021-04-07 19:27:47.995 GMT RECEIVER=/192.168.1.101:5701 at 2021-04-07 19:27:48.994 GMT SEQ=3 ]
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
---
members:
  - 192.168.1.101:5701
  - 192.168.1.102:5701
  - 192.168.1.103:5701

maxLogFileMegabytes: 10
```

# Design
Logging is done with java.util.logging.  The logging is configured programmatically, and the default configuration 
is ignored.  Everything logs to Itch.log, which has one FileHandler and one ConsoleHandler.  See Itch.setupLogging 
for details.

All IO is via SocketChannels in async mode.

For each other member in the cluster a socket connection is made, and the connection is wrapped  
by a HeartbeatWriter, which implements Runnable.  A scheduled executor calls the run method on each HeartbeatWriter 
once per second.

There is also a separate, continuously running, non-daemon, socket acceptor thread.  Any time a new connection 
is established, it is wrapped in a HeartbeatReader, which implements Runnable.  Each HeartbeatReader is run 
every second.  The run method reads from the socket and, if sufficient data is present, decodes it into a HeartBeat 
object.  The recipient and receive times are set, and the HeartBeat is logged.

There is also be a scheduled tasks that periodically checks the last time a hearbeat was received from every 
other member.  If any heartbeat is delayed more than 10 seconds a record will be written.

The protocol between sender and receiver is a custom binary protocol. See HeartBeatReader.read for details.

# Enhancements

- Log additional host metrics

