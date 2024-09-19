package server;

import commands.Command;
import commands.ReplconfCommand;
import commands.WaitCommand;
import connections.ReplicaClient;
import factories.CommandFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;

public class RedisServer {
    private static final int BUFFER_SIZE = 1024;
    private final CommandFactory commandFactory;
    private static RedisServer instance;
    private final int port;
    private final String role;
    private final String masterHost;
    private final int masterPort;
    private final List<SocketChannel> replicaChannels = new ArrayList<>();
    private final Semaphore semaphore = new Semaphore(1);
    private final Map<SocketChannel, Long> replicaOffsets = new ConcurrentHashMap<>();
    private long currentOffset = 0;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    private RedisServer(CommandFactory commandFactory, int port, String role, String masterHost, int masterPort) {
        this.commandFactory = commandFactory;
        this.port = port;
        this.role = role;
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }
    public static synchronized RedisServer getInstance(CommandFactory commandFactory, int port, String role, String masterHost, int masterPort) {
        if (instance == null) {
            instance = new RedisServer(commandFactory, port, role, masterHost, masterPort);
        }
        return instance;
    }

    public void start() throws InterruptedException {
        if ("slave".equals(role)) {
            ReplicaClient replicaClient = new ReplicaClient(masterHost, masterPort, port, commandFactory, semaphore);
            Thread thread = new Thread(replicaClient);
            //thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
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
        ByteBuffer response ;
        if (cmd != null) {
            if(cmd instanceof WaitCommand) {
                if(replicaChannels.isEmpty()){
                    response = ByteBuffer.wrap(":0\r\n".getBytes());
                    clientSocket.write(response);
                    return;
                }
                System.out.println("Current offset after propogating inside redis server is "+currentOffset);
                 sendGetAckToReplicas();
                 System.out.println("Sent ack to replicas");
                ((WaitCommand) cmd).setServer(this);
                ((WaitCommand) cmd).setClientChannel(clientSocket);
                long timeout = Long.parseLong(parsedCommand[2]);
                scheduler.schedule(((WaitCommand) cmd), timeout, java.util.concurrent.TimeUnit.MILLISECONDS);

            }
                if (cmd instanceof ReplconfCommand && "ack".equalsIgnoreCase(parsedCommand[1])) {
                    long offset = Long.parseLong(parsedCommand[2]);
                    System.out.println("Received ack from replica at offset " + offset);
                    System.out.println("Replica channel is " + clientSocket);
                    replicaOffsets.put(clientSocket, offset);
                    System.out.println("Replica offset in hashmap " + replicaOffsets.get(clientSocket) + " for channel " + clientSocket);
                }
                response = cmd.execute(parsedCommand);
                if (commandFactory.isWriteCommand(parsedCommand[0]) && !replicaChannels.isEmpty()) {
                    System.out.println("Propagating command to replicas");
                    System.out.println("command is "+command);
                    propagateCommandToReplica(buffer);
                }

        } else {
            String errorMessage = "-ERR unknown command '" + parsedCommand[0] + "'\r\n";
            response = ByteBuffer.wrap(errorMessage.getBytes());
        }
        if (!(cmd instanceof ReplconfCommand && "ack".equalsIgnoreCase(parsedCommand[1])) && !(cmd instanceof WaitCommand)) {
            System.out.println("Writing response to client"+response+" for command "+command+" for channel "+clientSocket);
            clientSocket.write(response);
        }

        if (cmd instanceof ReplconfCommand && "listening-port".equals(parsedCommand[1])) {
            replicaChannels.add(clientSocket);
        }
    }

    private void propagateCommandToReplica(ByteBuffer buffer) throws IOException {
        if(currentOffset == 0) {
            currentOffset+=buffer.remaining();
        }else{
            currentOffset+=buffer.remaining()+37;
        }

        for (SocketChannel replicaChannel : replicaChannels) {
            buffer.rewind();
            replicaChannel.write(buffer);
        }

//        System.out.println("Current offset after propogating inside redis server is "+currentOffset);
//
//        sendGetAckToReplicas();
//        System.out.println("Sent ack to replicas");

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
    public void sendGetAckToReplicas() throws IOException {
        String getAckCommand = "*3\r\n$8\r\nREPLCONF\r\n$6\r\nGETACK\r\n$1\r\n*\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(getAckCommand.getBytes());
        for (SocketChannel replicaChannel : replicaChannels) {
            buffer.rewind();
            replicaChannel.write(buffer);
        }
    }
    public int getProcessedReplicaCount(long requiredOffset) {
        int count = 0;
        for (Long offset : replicaOffsets.values()) {
            if (offset >= requiredOffset) {
                count++;
            }
        }
        System.out.println("Count of processed replicas is "+count);
        return count;
    }
    public int getReplicaCount() {
        return replicaChannels.size();
    }

    public long getCurrentOffset() {
        return currentOffset;
    }
    public void addOffset(long offset) {
        currentOffset += offset;
    }
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

}