import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ReplicaClient {
    private static final int BUFFER_SIZE = 1024;
    private final String masterHost;
    private final int masterPort;
    private final int replicaPort;
    public ReplicaClient(String masterHost, int masterPort, int replicaPort) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.replicaPort = replicaPort;
    }

    public void start() {
        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress(masterHost, masterPort));
            System.out.println("Connected to master at " + masterHost + ":" + masterPort);

            String pingCommand = "*1\r\n$4\r\nPING\r\n";
            ByteBuffer buffer = ByteBuffer.wrap(pingCommand.getBytes());
            socketChannel.write(buffer);
            System.out.println("Sent PING command to master");
            // Read PING response
            buffer = ByteBuffer.allocate(BUFFER_SIZE); // Increase buffer size
            socketChannel.read(buffer);
            buffer.flip();
            String pingResponse = new String(buffer.array(), 0, buffer.limit());
            System.out.println("Received response: " + pingResponse);

            // Send REPLCONF listening-port command
            String replconfListeningPortCommand = "*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$" + String.valueOf(replicaPort).length() + "\r\n" + replicaPort + "\r\n";
            buffer = ByteBuffer.wrap(replconfListeningPortCommand.getBytes());
            socketChannel.write(buffer);
            System.out.println("Sent REPLCONF listening-port command to master");

            // Read REPLCONF listening-port response
            buffer = ByteBuffer.allocate(BUFFER_SIZE); // Increase buffer size
            socketChannel.read(buffer);
            buffer.flip();
            String replconfListeningPortResponse = new String(buffer.array(), 0, buffer.limit());
            System.out.println("Received response: " + replconfListeningPortResponse);

            // Send REPLCONF capa psync2 command
            String replconfCapaPsync2Command = "*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n";
            buffer = ByteBuffer.wrap(replconfCapaPsync2Command.getBytes());
            socketChannel.write(buffer);
            System.out.println("Sent REPLCONF capa psync2 command to master");

            // Read REPLCONF capa psync2 response
            buffer = ByteBuffer.allocate(BUFFER_SIZE); // Increase buffer size
            socketChannel.read(buffer);
            buffer.flip();
            String replconfCapaPsync2Response = new String(buffer.array(), 0, buffer.limit());
            System.out.println("Received response: " + replconfCapaPsync2Response);

            // Send PSYNC command
            String psyncCommand = "*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n";
            buffer = ByteBuffer.wrap(psyncCommand.getBytes());
            socketChannel.write(buffer);
            System.out.println("Sent PSYNC command to master");

            // Read PSYNC response
            buffer = ByteBuffer.allocate(BUFFER_SIZE); // Increase buffer size
            socketChannel.read(buffer);
            buffer.flip();
            String psyncResponse = new String(buffer.array(), 0, buffer.limit());
            System.out.println("Received response: " + psyncResponse);
        } catch (IOException e) {
            System.err.println("Failed to connect to master: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
