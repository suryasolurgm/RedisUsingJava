import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;



public class Main {
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) {
        int port = 6379;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port 6379.");
            }
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

    private static void acceptNewClient(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel clientSocket = serverSocket.accept();
        clientSocket.configureBlocking(false);
        clientSocket.register(selector, SelectionKey.OP_READ);
        System.out.println("New client connected: " + clientSocket.getRemoteAddress());
    }

    private static void handleClient(SelectionKey key) throws IOException {
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

        ByteBuffer response;
        if (!command.isEmpty()) {
            response = ByteBuffer.wrap("+PONG\r\n".getBytes());
        } else {
            String errorMessage = "-ERR unknown command '" + command + "'\r\n";
            response = ByteBuffer.wrap(errorMessage.getBytes());
        }

        clientSocket.write(response);
    }
}
