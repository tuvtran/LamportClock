import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.lang.Thread;
import java.util.Random;

public class LamportClock extends Thread {

    private MulticastSocket sock;
    private InetAddress group;
    private int port;

    // local time of a process
    private int time;

    public LamportClock(InetAddress group, int port) throws Exception {
        this.group = group;
        this.port = port;

        // set local time to random
        Random rand = new Random();
        this.time = rand.nextInt(10);

        sock = new MulticastSocket(port);
        sock.setTimeToLive(2);
        sock.joinGroup(group);
    }

    public int getTime() {
        return this.time;
    }

    public int localEvent() {
        ++this.time;
        System.out.println(this.getId() + " performing local event. local time is " + this.time);
        return this.time;
    }

    public int receivedEvent(long senderId, int receivedTime) {
        int cur = getTime();

        if (receivedTime >= cur) {
            this.time = Math.max(receivedTime, this.time) + 1;
        }

        System.out.println(this.getId() + " received message from "
            + senderId + ". local time is " + this.time);

        return this.time;
    }

    public int sendEvent(String msg) throws Exception {
        byte[] data = msg.getBytes();

        // System.out.println(this.getId() + " sending message to " + id);
        // System.out.println("Local time is " + this.time);

        ++this.time;

        DatagramPacket d = new DatagramPacket(data, data.length, group, port);
        sock.send(d);

        return this.time;
    }

    public void updateTime(Event e) throws Exception {
        int type = e.type;

        switch (type) {
            case 0:
                this.localEvent();
                break;
            case 1:
                // extract information from the event
                long senderId = e.senderId;
                long receiverId = e.receiverId;
                int localTime = e.localTime;
                String content = e.content;

                 /** send a message of the following format
                 * SENDER_ID|RECEIVER_ID|LOCAL_TIME
                 */
                String msg = Long.toString(senderId) + "-" + Long.toString(receiverId)
                    + "-" + localTime + "-" + content;
                sendEvent(msg);
                break;
            case 2:
                senderId = e.senderId;
                localTime = e.localTime;
                receivedEvent(senderId, localTime);
                break;
            default:
                break;
        }

        printTime(e);
    }

    public void printTime(Event e) {
        System.out.println();
        System.out.println("Process " + this.getId());
        System.out.println("Process' local time " + this.getTime());
        System.out.println("\tEvent type: " + e.type);
        System.out.println("\tEvent sender's ID: " + e.senderId);
        System.out.println("\tEvent receiver's ID: " + e.receiverId);
        System.out.println("\tEvent local time: " + e.localTime);
        System.out.println("\tEvent content: " + e.content);
    }

    public void run() {
        System.out.println("Process " + this.getId() + " is initialized with local clock " + this.time);
        try {
            while (true) {
                DatagramPacket d = new DatagramPacket(new byte[256], 256);
                sock.receive(d);
                String s = new String(d.getData());
                // System.out.println(this.getId() + " received " + s);

                String[] meta = s.trim().split("-");
                long senderId = Long.parseLong(meta[0]);
                long receiverId = Long.parseLong(meta[1]);
                int localTime = Integer.parseInt(meta[2]);
                String content = "";
                // if there is a message
                if (meta.length >= 4)
                    content = meta[3];

                if (this.getId() == receiverId) {
                    Event e = new Event(2, senderId, receiverId, localTime, content);
                    updateTime(e);
                }
            }
        } catch (Exception e) {
            System.err.println("LC Failed: " + e);
        }
    }

}