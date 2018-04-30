import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Thread;

public class LamportClock extends Thread {

    private MulticastSocket sock;
    private InetAddress group;
    private int port;

    // local time of a process
    private int time;

    public LamportClock(InetAddress group, int port) throws Exception {
        this.group = group;
        this.port = port;

        // set local time to 0
        this.time = 0;

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

    public int sendEvent(long id) throws Exception {
        /** send a message of the following format
         * SENDER_ID|RECEIVER_ID|LOCAL_TIME
         */
        String msg = Long.toString(this.getId()) + "-"
            + Long.toString(id) + "-" + Integer.toString(this.getTime());
        byte[] data = msg.getBytes();

        System.out.println(this.getId() + " sending message to " + id);
        System.out.println("Local time is " + this.time);

        ++this.time;

        DatagramPacket d = new DatagramPacket(data, data.length, group, port);
        sock.send(d);

        return this.time;
    }

    public void run() {
        System.out.println("Process " + this.getId() + " is waiting for event!");
        try {
            while (true) {
                DatagramPacket d = new DatagramPacket(new byte[256], 256);
                sock.receive(d);
                String s = new String(d.getData());
                // System.out.println(this.getId() + " received " + s);

                String[] meta = s.trim().split("-");
                long senderId = Long.parseLong(meta[0]);
                long receiverId = Long.parseLong(meta[1]);
                int receivedTime = Integer.parseInt(meta[2]);
                if (this.getId() == receiverId)
                    receivedEvent(senderId, receivedTime);
            }
        } catch (Exception e) {
            System.err.println("LC Failed: " + e);
        }
    }

    public static void main(String[] args) {
        String input;

        System.setProperty("java.net.preferIPv4Stack" , "true");

        try {
            int n = Integer.parseInt(args[0]);
            LamportClock[] clocks = new LamportClock[n];
            InetAddress group = InetAddress.getByName("224.255.255.255");
            for (int i = 0; i < n; ++i) {
                int port = 8888;
                LamportClock lc = new LamportClock(group, port);
                lc.start();
                clocks[i] = lc;
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while ((input = in.readLine()) != "exit") {
                /**
                 * A message format is of the following:
                 * EVENT_NAME ID_OF_SENDER (ID_OF_RECEIVER)
                 * 
                 * EVENT_NAME is of the following:
                 *  - SEND
                 *  - LOCAL
                 * 
                 * For example:
                 * SEND 1 2 (process 1 sends a message to process 2)
                 * LOCAL 3 (process 3 performs a local event)
                 */

                // perform a string split operation based on space
                String[] splits = input.split(" ");
                if (splits.length == 0) {
                    continue;
                }
                switch(splits[0].toUpperCase()) {
                    case "SEND":
                        int firstProcessId = Integer.parseInt(splits[1]);
                        long secondProcessId = clocks[Integer.parseInt(splits[2])].getId();
                        clocks[firstProcessId].sendEvent(secondProcessId);
                        break;
                    case "LOCAL":
                        firstProcessId = Integer.parseInt(splits[1]);
                        clocks[firstProcessId].localEvent();
                        break;
                    default:
                        throw new RuntimeException("Invalid event name");
                }
            }
        } catch(Exception e) {
            System.err.println(e);
        }
    }

}