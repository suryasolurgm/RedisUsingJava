package server;

import commands.Command;
import commands.ReplconfCommand;
import commands.WaitCommand;
import commands.XReadCommand;
import connections.ReplicaClient;
import factories.CommandFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

public class RedisServer {
    private static final int BUFFER_SIZE = 1024;
    private static RedisServer instance;

    private final CommandFactory commandFactory;
    private final int port;
    private final ServerConfig config;
    private final ReplicationManager replicationManager;
    private final ConnectionManager connectionManager;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;

    private RedisServer(CommandFactory commandFactory, ServerConfig config) {
        this.commandFactory = commandFactory;
        this.port = config.getPort();
        this.config = config;
        this.replicationManager = new ReplicationManager(config);
        this.connectionManager = new ConnectionManager();
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.scheduledExecutorService = Executors.newScheduledThreadPool(10);
    }

    public static synchronized RedisServer getInstance(CommandFactory commandFactory, ServerConfig config) {
        if (instance == null) {
            instance = new RedisServer(commandFactory, config);
        }
        return instance;
    }

    public void start() {
        if (config.isSlave()) {
            startReplicaClient();
        }

        try (Selector selector = Selector.open();
             ServerSocketChannel serverSocket = ServerSocketChannel.open()) {

            setupServerSocket(serverSocket, selector);

            while (true) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        acceptNewClient(selector, serverSocket);
                    } else if (key.isReadable()) {
                        handleClient(key);
                    }
                }
            }
        } catch (IOException e) {
            Logger.error("Server exception: " + e.getMessage(), e);
        }
    }

    private void startReplicaClient() {
        ReplicaClient replicaClient = new ReplicaClient(config.getMasterHost(), config.getMasterPort(),
                config.getPort(), commandFactory, new Semaphore(1));
        executorService.submit(replicaClient);
    }

    private void setupServerSocket(ServerSocketChannel serverSocket, Selector selector) throws IOException {
        serverSocket.bind(new InetSocketAddress(port));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        Logger.info("Server is listening on port " + port);
    }

    private void acceptNewClient(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel clientSocket = serverSocket.accept();
        clientSocket.configureBlocking(false);
        clientSocket.register(selector, SelectionKey.OP_READ);
        Logger.info("New client connected: " + clientSocket.getRemoteAddress());
    }

    private void handleClient(SelectionKey key) {
        SocketChannel clientSocket = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        try {
            int bytesRead = clientSocket.read(buffer);
            if (bytesRead == -1) {
                connectionManager.closeConnection(key, clientSocket);
                return;
            }

            buffer.flip();
            String command = new String(buffer.array(), 0, buffer.limit()).trim();
            String[] parsedCommand = CommandParser.parseRESP(command);

            if (parsedCommand == null || parsedCommand.length == 0) return;

            Command cmd = commandFactory.getCommand(parsedCommand[0]);
            processCommand(cmd, parsedCommand, clientSocket, buffer);

        } catch (IOException e) {
            Logger.error("Error handling client: " + e.getMessage(), e);
            connectionManager.closeConnection(key, clientSocket);
        }
    }

    private void processCommand(Command cmd, String[] parsedCommand, SocketChannel clientSocket, ByteBuffer buffer) throws IOException {
        if (cmd == null) {
            String errorMessage = "-ERR unknown command '" + parsedCommand[0] + "'\r\n";
            clientSocket.write(ByteBuffer.wrap(errorMessage.getBytes()));
            return;
        }

        if (cmd instanceof WaitCommand) {
            handleWaitCommand((WaitCommand) cmd, parsedCommand, clientSocket);
        } else if (cmd instanceof ReplconfCommand && "ack".equalsIgnoreCase(parsedCommand[1])) {
            handleReplconfAck(parsedCommand, clientSocket);
        } else if(cmd instanceof XReadCommand && "block".equalsIgnoreCase(parsedCommand[1])) {
            handleXreadCommand((XReadCommand) cmd, parsedCommand, clientSocket);

        } else {
            ByteBuffer response = cmd.execute(parsedCommand);
            if (commandFactory.isWriteCommand(parsedCommand[0]) && !replicationManager.getReplicaChannels().isEmpty()) {
                replicationManager.propagateCommandToReplicas(buffer);
            }
            if (!(cmd instanceof ReplconfCommand && "ack".equalsIgnoreCase(parsedCommand[1]))) {
                clientSocket.write(response);
            }
        }

        if (cmd instanceof ReplconfCommand && "listening-port".equals(parsedCommand[1])) {
            replicationManager.addReplicaChannel(clientSocket);
        }
    }
    private void handleXreadCommand(XReadCommand cmd, String[] parsedCommand, SocketChannel clientSocket) {
        long timeout = Long.parseLong(parsedCommand[2]);
        System.out.println("timeout: "+timeout);
        String[] filtered = Arrays.stream(parsedCommand)
                .filter(s -> !s.equals("block") && !s.equals(String.valueOf(timeout)))
                .toArray(String[]::new);
        cmd.setArgs(filtered);
        scheduledExecutorService.schedule(cmd, timeout, TimeUnit.MILLISECONDS);
    }
    private void handleWaitCommand(WaitCommand cmd, String[] parsedCommand, SocketChannel clientSocket) {
        if (replicationManager.getReplicaChannels().isEmpty()) {
            try {
                clientSocket.write(ByteBuffer.wrap(":0\r\n".getBytes()));
            } catch (IOException e) {
                Logger.error("Error writing to client: " + e.getMessage(), e);
            }
            return;
        }

        cmd.setServer(this);
        cmd.setClientChannel(clientSocket);
        long timeout = Long.parseLong(parsedCommand[2]);
        replicationManager.sendGetAckToReplicas();
        scheduledExecutorService.schedule(cmd, timeout, TimeUnit.MILLISECONDS);
    }

    private void handleReplconfAck(String[] parsedCommand, SocketChannel clientSocket) {
        long offset = Long.parseLong(parsedCommand[2]);
        Logger.info("Received ack from replica at offset " + offset);
        replicationManager.updateReplicaOffset(clientSocket, offset);
    }

    public int getProcessedReplicaCount(long requiredOffset) {
        return replicationManager.getProcessedReplicaCount(requiredOffset);
    }

    public int getReplicaCount() {
        return replicationManager.getReplicaCount();
    }

    public long getCurrentOffset() {
        return replicationManager.getCurrentOffset();
    }

    public void addOffset(long offset) {
        replicationManager.addOffset(offset);
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}