# Lamport Clock Implementation

For this homework, I implemented Lamport clock and the resource allocated algorithm.

## How to run:

To compile the Lamport Clock manager:

```bash
$ javac Main.java
```

To run the Lamport Clock manager with 5 clocks:

```bash
$ java Main 5
```

Each clock is identified with a unique ID. However, to control their actions, we 
use the order number specified when the program starts:

```bash
Process 0 Unique ID 10 is initialized with local clock 0
Process 1 Unique ID 11 is initialized with local clock 0
Process 2 Unique ID 12 is initialized with local clock 0
```

To execute local event on clock 2

```bash
$ local 2
```

To send a message from clock 1 to clock 0

```bash
$ send 1 0
```

## Part 1: Lamport Clock

In order to simulate a distributed environment, I used Java's `MulticastSocket` 
and `DatagramPacket` to send data between different threads. Once a message has 
been sent, it will be relayed to all participating threads. However, only the one 
with the recipient ID updates its local time and is considered a receiver.

To instantiate a Lamport Clock instance, let's define its properties:

```java
public LamportClock(InetAddress group, int port) throws Exception {
    this.group = group;
    this.port = port;

    // if we don't assign an order to a process
    this.order = -1;

    this.time = 0;

    sock = new MulticastSocket(port);
    sock.setTimeToLive(2);
    sock.joinGroup(group);
}
```

With the following code, when a new Lamport Clock object is created, it joins the 
pool along with the rest and begins receiving messages.

Along with the Lamport Clock class, I also defined a class for events:

```java
public class Event {

    public int type;
    public long senderId;
    public long receiverId;
    public int localTime;
    public String content;

    public Event(int type, long senderId, long receiverId, String content) {
        this.type = type;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.localTime = 0;
    }

    public Event(int type, long senderId,
    long receiverId, int localTime, String content) {
        this(type, senderId, receiverId, content);
        this.localTime = localTime;
    }

}
```

`type` of `Event` is:

- 0 if an event is a `LOCAL EVENT`
- 1 if an event is a `SEND EVENT`
- 2 if an event is a `RECEIVE EVENT`

Inside `Event`, we also include a sender's and receiver's ID, the time of that event 
and its content.

Now inside the main loop, besides participating threads that act as Lamport Clock instances, 
we also have a master thread to keep track of the messages being sent among those 
participants. The master's job is to call the method `updateTime` whenever there is 
a new `Event` happening. The reason we need to do that is because as a Thread is in 
the loop to receive messages, it pretty much can't do anything else.

A command to enter for an event to happen is of the following syntax: <event_type> 
<sender order> <receiver order>. Note that in this case a sender's `order` is different 
from a sender's `ID` mainly in the fact that an ID is a unique thread ID, while an
`order` is something we assign to a parciparting thread to distinguish them from another.

```java
switch(splits[0].toUpperCase()) {
    case "SEND":
        int clockArrayId = Integer.parseInt(splits[1]);
        long firstProcessId = clocks[clockArrayId].getId();
        long secondProcessId = clocks[Integer.parseInt(splits[2])].getId();
        String messageContent = "";
        if (splits.length >= 3) {
            List<String> wordsList = Arrays.asList(
                Arrays.copyOfRange(splits, 3, splits.length));
            messageContent = String.join(" ", wordsList);
        }

        Event e = new Event(1, firstProcessId, secondProcessId, messageContent);
        clocks[clockArrayId].updateTime(e);
        break;

    case "LOCAL":
        clockArrayId = Integer.parseInt(splits[1]);
        firstProcessId = clocks[clockArrayId].getId();
        secondProcessId = 0;
        messageContent = "";

        e = new Event(0, firstProcessId, secondProcessId, messageContent);
        clocks[clockArrayId].updateTime(e);
        break;

    default:
        throw new RuntimeException("Invalid event name");

}
```

After we call `updateTime` method, there is a switch statement to update time 
based on the type of events.

```java
public void updateTime(Event e) throws Exception {
    int type = e.type;

    switch (type) {
        // LOCAL EVENT
        case 0:
            this.localEvent();
            break;

        // SEND EVENT
        case 1: // extract information from the event
            long senderId = e.senderId;
            long receiverId = e.receiverId;
            // increase the time first before sending the message
            e.localTime = ++this.time;
            String content = e.content;

                /** send a message of the following format
             * SENDER_ID|RECEIVER_ID|LOCAL_TIME
             */
            String msg = Long.toString(senderId) + "-" + Long.toString(receiverId)
                + "-" + e.localTime + "-" + content;
            sendEvent(msg);
            break;

        // RECEIVE EVENT
        case 2:
            // update its logical clock
            this.time = Math.max(e.localTime, this.time) + 1;
            break;
        default:
            break;
    }

    printTime(e);
}
```

Now, let's do some testing.

```
local 1
-------------------------
Process 11
Process' local time 1
	Event type: LOCAL EVENT
	Event sender's ID: 11
	Event receiver's ID: 0
	Event local time: 0
	Event content:
-------------------------
local 2
-------------------------
Process 12
Process' local time 1
	Event type: LOCAL EVENT
	Event sender's ID: 12
	Event receiver's ID: 0
	Event local time: 0
	Event content:
-------------------------
local 0
-------------------------
Process 10
Process' local time 1
	Event type: LOCAL EVENT
	Event sender's ID: 10
	Event receiver's ID: 0
	Event local time: 0
	Event content:
-------------------------
send 1 2
-------------------------
Process 11
Process' local time 2
	Event type: SEND EVENT
	Event sender's ID: 11
	Event receiver's ID: 12
	Event local time: 2
	Event content:
-------------------------
-------------------------
Process 12
Process' local time 3
	Event type: RECEIVE EVENT
	Event sender's ID: 11
	Event receiver's ID: 12
	Event local time: 2
	Event content:
-------------------------
send 0 1
-------------------------
Process 10
Process' local time 2
	Event type: SEND EVENT
	Event sender's ID: 10
	Event receiver's ID: 11
	Event local time: 2
	Event content:
-------------------------
-------------------------
Process 11
Process' local time 3
	Event type: RECEIVE EVENT
	Event sender's ID: 10
	Event receiver's ID: 11
	Event local time: 2
	Event content:
-------------------------
```

Looks like the program is behaving correctly.

## Part 2: Resouce allocation

To expand the case handling for different events related to request, reply and 
acknowledgement of requests for distributed mutual exclusion, we add the following 
to `updateTime()`:

```java
// REQUEST EVENT
case 3:
    // update its local clock
    e.localTime = ++this.time;
    // add new request to the priority queue
    clockPQ.add(new Request(this.time, this.getId()));
    String requestContent = "REQUEST-" + this.time + "-" + this.getId();
    sendEvent(requestContent);
    break;

// REPLY REQUEST EVENT
case 4:
    // update its local clock
    ++this.time;
    // add new request to the priority queue
    clockPQ.add(new Request(e.localTime, e.senderId));
    break;

// REPLY EVENT
case 5:
    e.localTime = ++this.time;
    senderId = e.senderId;
    break;

// ACK EVENT
case 6:
    // update its local clock
    ++this.time;
    break;

default:
    break;
```

To add a class for wrapping around `Request`:

```java
/**
 * This class represents a request to be added to the priority queue
 */
public class Request implements Comparable<Request> {

    // ...

    public Request(int time, long processId) {
        this.time = time;
        this.processId = processId;
    }

    // ...

    @Override
    public int compareTo(Request other) {
        if (this.getTime() == other.getTime())
            return 0;
        else if (this.getTime() > other.getTime())
            return 1;
        else
            return -1;
    }

}
```

We override `compareTo` so that each `Request` can be put into a process' `PriorityQueue`:

```java
public LamportClock(InetAddress group, int port) throws Exception {
    // ...

    // initialize the priority queue
    this.clockPQ = new PriorityQueue<>();

    // ...
}
```

For a process to request to enter the critical section, we enter the following command:

```bash
$ request <process order number>
```
