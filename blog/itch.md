

Some problems only happen in prodction ...

Troubleshooting problems with clustered solutions like Hazelcast or GemFire is complicated and, with the steady trend toward virtualization, it has become even more challenging.  Virtualized environments may be subject to relatively short-lived "brown outs" that might not affect other workloads but can trigger failure recovery mechanisms in clustered solutions.  Often the on-board diagnostics cannot detect problems with host because it is running within the guest and facts about the host are intentionally hidden (this is a prerequisite of virtualization).  Also, many times historical metrics are aggregated at a very course grained level making them not useful for troubleshooting purposes.  For example, if a machine is at at 100% CPU for 1 minute during a 30 minute interval, the average CPU utilization for the 30 minute interval might be quite low.  As another example, if a VM is vMotioned to another host, there can be a perceptible pause which would affect the health of the cluster but would not be detectable from within the guest (after all, that is the goal of virtualization).

In any case, when troubleshooting, its not always clear whether the resources necessary for stable operation were in fact available when an incident occurred and sometimes the information is not available after the fact.  If the trouble manifests itself as a failure of the Hazelcast cluster then Hazelcast becomes suspect.  

 I have often wished for a mechanism to verify resource availability and detect virtualization related paused that is independent of the afflicted process.  This is the inspiration for itch.

This post describes the how short "brown-outs", delays in network connectivity and virtualization related pauses can be detected. 

Every member of the cluster  established a connection to every other member of the cluster. This of course means that every member exposes a server socket and expects a connection from every other member. With connections established, we simply start sending heartbeat packages along each connection.  

[Picture Here]

As it sends the heartbeat, the sender includes a timestamp taken from its own system clock.  As the recipient receives a heartbeat, it records the time of receipt into the system clock.  Every member logs both heartbeats sent and received.  This provides a second by second record of reachability and CPU availability within the cluster of machines.

Consider a virtualization pause.  This is not something that can be noticed by a process running within the paused machine (by design).  However, it is easily observable by any machines in the cluster that were not paused because those machines will temporarily stop sending hearbeats.  

This is exactly what itch does.  



