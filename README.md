# Independent Test of Cluster Health

This tool tests connectivity between a collection of machines.  Each machine simply 
opens a socket to every other machine in the collection and writes "heartbeat" 
messages.  At the same time, each machine records the heartbeats that it receives.  Output is sent 
to a file named itch_hostname.log in the working directory.

```
2021-04-23 20:43:37.259 GMT INFO : listening on /192.168.1.102:5701
2021-04-23 20:43:37.291 GMT INFO : Established connection to /192.168.1.101:5701
2021-04-23 20:43:37.303 GMT INFO : Established connection to /192.168.1.103:5701
2021-04-23 20:43:37.313 GMT INFO : Received a new connection from /192.168.1.101:49646
2021-04-23 20:43:37.322 GMT INFO : Received a new connection from /192.168.1.103:41978
2021-04-23 20:43:38.266 GMT INFO : SENT HeartBeat [ SENDER=/192.168.1.102:53162 at 2021-04-23 20:43:38.266 GMT RECEIVER=/192.168.1.101:5701 at 1970-01-01 00:00:00.000 GMT TIME DELTA=-1619210618266ms SEQ=1 PAYLOAD BYTES=0 ]
2021-04-23 20:43:38.272 GMT INFO : SENT HeartBeat [ SENDER=/192.168.1.102:49216 at 2021-04-23 20:43:38.271 GMT RECEIVER=/192.168.1.103:5701 at 1970-01-01 00:00:00.000 GMT TIME DELTA=-1619210618271ms SEQ=1 PAYLOAD BYTES=0 ]
2021-04-23 20:43:38.286 GMT INFO : RECEIVED HeartBeat [ SENDER=/192.168.1.101:49646 at 2021-04-23 20:43:38.258 GMT RECEIVER=/192.168.1.102:5701 at 2021-04-23 20:43:38.286 GMT TIME DELTA=28ms SEQ=1 PAYLOAD BYTES=0 ]
2021-04-23 20:43:38.293 GMT INFO : RECEIVED HeartBeat [ SENDER=/192.168.1.103:41978 at 2021-04-23 20:43:38.275 GMT RECEIVER=/192.168.1.102:5701 at 2021-04-23 20:43:38.293 GMT TIME DELTA=18ms SEQ=1 PAYLOAD BYTES=0 ]
2021-04-23 20:43:39.266 GMT INFO : SENT HeartBeat [ SENDER=/192.168.1.102:53162 at 2021-04-23 20:43:39.265 GMT RECEIVER=/192.168.1.101:5701 at 1970-01-01 00:00:00.000 GMT TIME DELTA=-1619210619265ms SEQ=2 PAYLOAD BYTES=0 ]
2021-04-23 20:43:39.272 GMT INFO : SENT HeartBeat [ SENDER=/192.168.1.102:49216 at 2021-04-23 20:43:39.271 GMT RECEIVER=/192.168.1.103:5701 at 1970-01-01 00:00:00.000 GMT TIME DELTA=-1619210619271ms SEQ=2 PAYLOAD BYTES=0 ]
2021-04-23 20:43:39.286 GMT INFO : RECEIVED HeartBeat [ SENDER=/192.168.1.101:49646 at 2021-04-23 20:43:39.258 GMT RECEIVER=/192.168.1.102:5701 at 2021-04-23 20:43:39.286 GMT TIME DELTA=28ms SEQ=2 PAYLOAD BYTES=0 ]
2021-04-23 20:43:39.293 GMT INFO : RECEIVED HeartBeat [ SENDER=/192.168.1.103:41978 at 2021-04-23 20:43:39.275 GMT RECEIVER=/192.168.1.102:5701 at 2021-04-23 20:43:39.292 GMT TIME DELTA=17ms SEQ=2 PAYLOAD BYTES=0 ]
```

This provides an independent record of connectivity between nodes during a period of time.

Additionally, the heartbeat information can be exported as a Prometheus metric
for automated collection.

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
payloadBytes: 0
heartbeatPeriodMs: 1000
prometheus:
  enabled: True
  port: 9090
```

You can use the `heartbeatPeriodMs` setting to control the frequency with which heartbeats are sent.

You can also increase the size of the heartbeat arbitrarily by setting `payloadBytes` to a non-zero value.  
This causes a byte array of the given size, filled with `0xff` to be sent in each heartbeat.  This can be useful
for creating stress on the network or causing each heartbeat to be split over multiple TCP packets.

To enable the Prometheus exporter, set `prometheus.enabled` to True and 
specify the port number for the Prometheus HTTP server to listen on.  This 
also determines the endpoint you will need to specify when configuring 
the Prometheus collector.

# Design
Logging is done with java.util.logging.  The logging is configured programmatically, and the default configuration 
is ignored.  Everything logs to Itch.log, which has one FileHandler and one 
ConsoleHandler.  See Itch.setupLogging for details.

All IO is via SocketChannels in async mode.

For each other member in the cluster a socket connection is made, and the 
connection is wrapped by a HeartbeatWriter, which implements Runnable.  
A scheduled executor calls the run method on each HeartbeatWriter 
once per second.

There is also a separate, continuously running, non-daemon, socket acceptor 
thread.  Any time a new connection is established, it is wrapped in a 
HeartbeatReader, which implements Runnable.  Each HeartbeatReader is run 
every second.  The run method reads from the socket and, if sufficient data is present, decodes it into a HeartBeat 
object.  The recipient and receive times are set, and the HeartBeat is logged.

There is also a scheduled tasks that periodically checks the last time a 
hearbeat was received from every other member.  If any heartbeat is delayed 
more than 10 seconds a record will be written.

The protocol between sender and receiver is a custom binary protocol. See 
HeartBeatReader.read for details.

# Enhancements

- Log additional host metrics

