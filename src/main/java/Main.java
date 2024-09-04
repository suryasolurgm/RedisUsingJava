import commands.Command;
import factories.CommandFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Main {
    private static final int BUFFER_SIZE = 1024;
    private static final Map<String, String> dataStore = new HashMap<>();
    private static final Map<String, Long> expiryStore = new HashMap<>();
    private static String dir = "/tmp";
    private static String dbfilename = "dump.rdb";
    private static CommandFactory commandFactory ;

    public static void main(String[] args) {
        int port = 6379;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dir":
                    if (i + 1 < args.length) {
                        dir = args[++i];
                    }
                    break;
                case "--dbfilename":
                    if (i + 1 < args.length) {
                        dbfilename = args[++i];
                    }
                    break;
                default:
                    try {
                        port = Integer.parseInt(args[i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid port number. Using default port 6379.");
                    }
                    break;
            }
        }

        commandFactory = new CommandFactory(dataStore, expiryStore, dir, dbfilename);
        loadRDBFile();

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
    private static void loadRDBFile() {
        File rdbFile = new File(dir, dbfilename);
        if (!rdbFile.exists()) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(rdbFile)) {
            byte[] header = new byte[9];
            if (fis.read(header) != 9 || !new String(header).equals("REDIS0011")) {
                throw new IOException("Invalid RDB file header");
            }

            while (fis.available() > 0) {
                int type = fis.read();
                if (type == 0xFE) { // Database section
                    fis.read(); // Skip database index
                    fis.read(); // Skip table size info
                    fis.read(); // Skip hash table size
                    fis.read(); // Skip expiry time hash table size
                } else if (type == 0xFC || type == 0xFD) { // Expire information
                    byte[] expireBytes = new byte[type == 0xFC ? 8 : 4];
                    fis.read(expireBytes);
                } else if (type == 0x00) { // String value
                    String key = readString(fis);
                    System.out.println("Key: " + key);
                    String value = readString(fis);
                    dataStore.put(key, value);
                } else if (type == 0xFF) { // End of file
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading RDB file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private static String readString(FileInputStream fis) throws IOException {
        int length = readSize(fis);
        byte[] strBytes = new byte[length];
        fis.read(strBytes);
        return new String(strBytes);
    }
    private static int readSize(FileInputStream fis) throws IOException {
        int firstByte = fis.read();
        int size;
        if ((firstByte & 0xC0) == 0x00) {
            size = firstByte & 0x3F;
            System.out.println("Size: " + size);
        } else if ((firstByte & 0xC0) == 0x40) {
            size = ((firstByte & 0x3F) << 8) | fis.read();
        } else if ((firstByte & 0xC0) == 0x80) {
            size = (fis.read() << 24) | (fis.read() << 16) | (fis.read() << 8) | fis.read();
        } else {
            throw new IOException("Invalid size encoding");
        }
        return size;
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

    private static String[] parseRESP(String command) {
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


