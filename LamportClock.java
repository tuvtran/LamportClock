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
        return ++this.time;
    }

    public int sendEvent() {
        return ++this.time;
    }

    public int receivedEvent(int receivedTime) {
        int cur = getTime();

        if (receivedTime < cur)
            return cur;

        this.time = Math.max(receivedTime, this.time) + 1;
        return this.time;
    }

    public void send(String msg) throws Exception {
        byte[] data = msg.getBytes();
        System.out.println(this.getId() + " sending "+data);
        DatagramPacket d = new DatagramPacket(data,data.length,group,port);
        sock.send(d);
    }

    public void run() {
        System.out.println("Process " + this.getId() + " is waiting for event!");
        try {
            while (true) {
                DatagramPacket d = new DatagramPacket(new byte[256], 256);
                sock.receive(d);
                String s = new String(d.getData());
                System.out.println(this.getId() + " receved " + s);
            }
        } catch (Exception e) {
            System.err.println("LC Failed: " + e);
        }
    }

    public static void main(String[] args) {
        String input;

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
                switch(splits[0]) {
                    case "SEND":
                        clocks[0].send(input);
                        break;
                    case "RECEIVE":
                        clocks[0].send(input);
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