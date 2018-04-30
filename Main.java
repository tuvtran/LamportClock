import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {

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