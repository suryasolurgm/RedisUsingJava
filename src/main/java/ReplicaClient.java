import commands.Command;
import commands.ReplconfCommand;
import factories.CommandFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class ReplicaClient implements Runnable {
    private static final int BUFFER_SIZE = 1024;
    private final String masterHost;
    private final int masterPort;
    private final int replicaPort;
    private final CommandFactory commandFactory;
    private final Semaphore semaphore;

    public ReplicaClient(String masterHost, int masterPort, int replicaPort, CommandFactory commandFactory, Semaphore semaphore) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.replicaPort = replicaPort;
        this.commandFactory = commandFactory;
        this.semaphore = semaphore;
    }

    @Override
    public void run() {
        try {
            semaphore.acquire();
            try (SocketChannel socketChannel = SocketChannel.open()) {
                socketChannel.connect(new InetSocketAddress(masterHost, masterPort));
                socketChannel.configureBlocking(true);
                System.out.println("Connected to master at " + masterHost + ":" + masterPort);

                // Perform handshake with master
                performHandshake(socketChannel);
                semaphore.release();
                // Process commands from master

            } catch (IOException e) {
                System.err.println("Failed to connect to master: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("ReplicaClient interrupted: " + e.getMessage());
        } finally {
            semaphore.release();
        }
    }

    private void performHandshake(SocketChannel socketChannel) throws IOException {
        sendCommand(socketChannel, "*1\r\n$4\r\nPING\r\n");
        readResponse(socketChannel);

        sendCommand(socketChannel, "*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$" + String.valueOf(replicaPort).length() + "\r\n" + replicaPort + "\r\n");
        readResponse(socketChannel);

        sendCommand(socketChannel, "*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n");
        readResponse(socketChannel);

        sendCommand(socketChannel, "*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n");
        processRDBAndCommands(socketChannel);

    }

    private void sendCommand(SocketChannel socketChannel, String command) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(command.getBytes());
        socketChannel.write(buffer);
    }

    private void readResponse(SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        socketChannel.read(buffer);
        buffer.flip();
        String response = new String(buffer.array(), 0, buffer.limit());
        System.out.println("Received response: " + response);
    }

    private void processRDBAndCommands(SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        StringBuilder commandBuilder = new StringBuilder();
        boolean rdbProcessed = false;
        boolean fullResyncProcessed = false;
        int rdbBytesToSkip = 88; // Number of bytes to skip after the $88\r\n header
        boolean rdbHeaderSkipped = false; // To track if the $88\r\n part is skipped

        while (true) {
            buffer.clear();
            int bytesRead = socketChannel.read(buffer);
            if (bytesRead == -1) {
                break;
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                if (!fullResyncProcessed) {
                    // Collect characters until we have the full +FULLRESYNC response
                    char c = (char) buffer.get();
                    commandBuilder.append(c);
                    if (commandBuilder.toString().endsWith("\r\n")) {
                        String response = commandBuilder.toString().trim();
                        if (response.startsWith("+FULLRESYNC")) {
                            // We expect something like "+FULLRESYNC <id> <offset>\r\n"
                            // Skip it by marking as processed
                            System.out.println("Received FULLRESYNC response: " + response);
                            fullResyncProcessed = true;
                        }
                        commandBuilder.setLength(0); // Clear for the next command
                    }
                } else if (!rdbProcessed) {
                    // Skip the RDB header
                    if (!rdbHeaderSkipped) {
                        // Read until the $88\r\n part is fully skipped
                        char c = (char) buffer.get();
                        commandBuilder.append(c);
                        if (commandBuilder.toString().endsWith("\r\n")) {
                            String header = commandBuilder.toString().trim();
                            if (header.startsWith("$88")) {
                                // We've skipped the RDB length header ($88\r\n)
                                System.out.println("Skipped RDB header: " + header);
                                rdbHeaderSkipped = true;
                            }
                            commandBuilder.setLength(0); // Clear the header part
                        }
                    } else {
                        // Skip the next 88 bytes of actual RDB data
                        int bytesToSkip = Math.min(buffer.remaining(), rdbBytesToSkip);
                        buffer.position(buffer.position() + bytesToSkip);
                        rdbBytesToSkip -= bytesToSkip;
                        if (rdbBytesToSkip == 0) {
                            rdbProcessed = true; // We have now skipped the RDB part
                            System.out.println((buffer.remaining() > 0) ? "Remaining bytes: afer rdb " + buffer.remaining() : "No remaining bytes");


                        }
                    }
                } else {
                    // Process regular commands after FULLRESYNC and RDB are done
                    while (buffer.hasRemaining()) {
                        char c = (char) buffer.get();
                        System.out.println("Received byte: " + c);
                        commandBuilder.append(c);

                        // Check if we have a complete Redis command
                        if (commandBuilder.toString().startsWith("*") && isCompleteRedisCommand(commandBuilder.toString())) {
                            buffer.get();
                            buffer.get();
                            String command = commandBuilder.toString().trim();
                            System.out.println("Received command: " + command.trim());
                            System.out.println((buffer.remaining() > 0) ? "Remaining bytes: " + buffer.remaining() : "No remaining bytes");
                            processCommand(command); // Handle each command
                            commandBuilder.setLength(0); // Clear for the next command
                        }
                    }
                }
            }
        }
    }
    private boolean isCompleteRedisCommand(String command) {
        // This method checks if the command is a complete Redis command
        if (!command.startsWith("*")) {
            return false;
        }

        String[] lines = command.split("\r\n");
        if (lines.length < 2) {
            return false;
        }

        int expectedArgs = Integer.parseInt(lines[0].substring(1));
        int currentArgs = 0;

        for (int i = 1; i < lines.length; i += 2) {
            if (i + 1 >= lines.length) {
                return false;
            }
            if (lines[i].startsWith("$")) {
                int expectedLength = Integer.parseInt(lines[i].substring(1));
                if (lines[i + 1].length() != expectedLength) {
                    return false;
                }
                currentArgs++;
            }
        }

        return currentArgs == expectedArgs;
    }






    private void processCommand(String command) {
        String[] parsedCommand = parseRESP(command);
        if (parsedCommand == null || parsedCommand.length == 0) {
            return;
        }
        Command cmd = commandFactory.getCommand(parsedCommand[0]);
        System.out.println("Command: " + parsedCommand[0]);
        ByteBuffer response;
        if (cmd != null) {
            response = cmd.execute(parsedCommand);

        } else {
            String errorMessage = "-ERR unknown command '" + parsedCommand[0] + "'\r\n";
            response = ByteBuffer.wrap(errorMessage.getBytes());
        }
    }

    private String[] parseRESP(String command) {
        String[] lines = command.split("\r\n");
        System.out.println("Lines: " + Arrays.toString(lines));
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