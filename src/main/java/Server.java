import commands.Command;
import factories.CommandFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Server {
    private static final int BUFFER_SIZE = 1024;
    private final CommandFactory commandFactory;
    private final int port;
    private final String role;
    private final String masterHost;
    private final int masterPort;
    public Server(CommandFactory commandFactory, int port,String role, String masterHost, int masterPort) {
        this.commandFactory = commandFactory;
        this.port = port;
        this.role = role;
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }

    public void start() {
        if ("slave".equals(role)) {
            ReplicaClient replicaClient = new ReplicaClient(masterHost, masterPort,port);
            replicaClient.start();
        }


            try (Selector selector = Selector.open();
                 ServerSocketChannel serverSocket = ServerSocketChannel.open()) {

                serverSocket.bind(new InetSocketAddress(port));
                serverSocket.configureBlocking(false);
                serverSocket.register(selector, SelectionKey.OP_ACCEPT);

                System.out.println("Server is listening on port " + port);

                while (true) {
                    selector.select();
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();

                        if (!key.isValid()) {
                            continue;
                        }

                        if (key.isAcceptable()) {
                            acceptNewClient(selector, serverSocket);
                        } else if (key.isReadable()) {
                            handleClient(key);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Server exception: " + e.getMessage());
                e.printStackTrace();
            }

    }

    private void acceptNewClient(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel clientSocket = serverSocket.accept();
        clientSocket.configureBlocking(false);
        clientSocket.register(selector, SelectionKey.OP_READ);
        System.out.println("New client connected: " + clientSocket.getRemoteAddress());
    }

    private void handleClient(SelectionKey key) throws IOException {
        SocketChannel clientSocket = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        int bytesRead;
        try {
            bytesRead = clientSocket.read(buffer);
        } catch (IOException e) {
            System.out.println("Client disconnected: " + clientSocket.getRemoteAddress());
            key.cancel();
            clientSocket.close();
            return;
        }

        if (bytesRead == -1) {
            System.out.println("Client disconnected: " + clientSocket.getRemoteAddress());
            key.cancel();
            clientSocket.close();
            return;
        }

        buffer.flip();
        String command = new String(buffer.array(), 0, buffer.limit()).trim();

        String[] parsedCommand = parseRESP(command);
        if (parsedCommand == null || parsedCommand.length == 0) {
            return;
        }

        Command cmd = commandFactory.getCommand(parsedCommand[0]);
        ByteBuffer response;
        if (cmd != null) {
            response = cmd.execute(parsedCommand);
        } else {
            String errorMessage = "-ERR unknown command '" + parsedCommand[0] + "'\r\n";
            response = ByteBuffer.wrap(errorMessage.getBytes());
        }

        clientSocket.write(response);
    }

    private String[] parseRESP(String command) {
        String[] lines = command.split("\r\n");
        if (lines.length < 3 || !lines[0].startsWith("*")) {
            return null;
        }

        int numArgs = Integer.parseInt(lines[0].substring(1));
        String[] result = new String[numArgs];
        int index = 0;

        for (int i = 1; i < lines.length; i++) {
            if (lines[i].startsWith("$")) {
                result[index++] = lines[++i];
            }
        }

        return result;
    }
}
