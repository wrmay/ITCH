# Independent Test of Cluster Health

This tool tests connectivity between a collection of machines.  Each machine simply 
opens a socket to every other machine in the collection and writes "heartbeat" 
messages.  At the same time, each machine records the heartbeats that it receives.

This provides an independent record of connectivity during a period of time.

# Design

For sending the hearbeats, a scheduled thread pool will write the hearbeats 
to a socket channel in async mode.  Each period, if the message is successfully 
written to the socket a message will be recorded in the logs. If the socket is 
not ready to send, a message will be recorded in the logs.  

For receiving heartbeats, a ServerSocketChannels will be created and 
a thread will be periodically scheduled to receive the heartbeats from the 
channels and write a message. 

There will also be a thread that periodically wakes up and checks the last time
a hearbeat was received from every other member.  If the heartbeat is delayed 
more than a certain amount of time a record will be written.

# Heartbeat Content
- identity of the sender
- identity of the recipient
- timestamp when sent
- timestamp when received (added by the recipient upon receipt)
- sequence number (sender plus recipient plus sequence # uniquely identifies a heartbeat message) 

It needs a method to write to a Buffer and another to read from a Buffer

# Configuration
Place a file called itch.yaml in the working directory. The content should be 
similar to the example given below.
```yaml
members:
    - member-1
    - member-2

```
