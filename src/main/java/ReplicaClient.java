import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ReplicaClient {
    private final String masterHost;
    private final int masterPort;

    public ReplicaClient(String masterHost, int masterPort) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }

    public void start() {
        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress(masterHost, masterPort));
            System.out.println("Connected to master at " + masterHost + ":" + masterPort);

            String pingCommand = "*1\r\n$4\r\nPING\r\n";
            ByteBuffer buffer = ByteBuffer.wrap(pingCommand.getBytes());
            socketChannel.write(buffer);
            System.out.println("Sent PING command to master");
        } catch (IOException e) {
            System.err.println("Failed to connect to master: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
