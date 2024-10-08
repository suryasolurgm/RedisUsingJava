import factories.CommandFactory;
import server.RedisServer;
import server.ServerConfig;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

///mnt/c/Users/surya/IdeaProjects/codecrafters-redis-java

public class Main {
    private static final Map<String, String> dataStore = new ConcurrentHashMap<>();
    private static final Map<String, TreeMap<String, Map<String, String>>> streamDataStore= new ConcurrentHashMap<>();
    private static final Map<String, Long> expiryStore = new ConcurrentHashMap<>();
    private static final Map<String, String> lastEntryIdStore = new ConcurrentHashMap<>();
    private static String dir = "/tmp";
    private static String dbfilename = "dump.rdb";
    private static CommandFactory commandFactory ;
    private static String role = "master";
    private static final String replicationId = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
    private static final long replicationOffset = 0;
    private static String masterHost = null;
    private static int masterPort = 0;
    private static final Map<SocketChannel, Long> replicaOffsets = new ConcurrentHashMap<>();
    public static void main(String[] args) throws InterruptedException {
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
                case "--port":
                    if (i + 1 < args.length) {
                        try {
                            port = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid port number. Using default port 6379.");
                        }
                    }
                    break;
                case "--replicaof":
                    if (i + 1 < args.length) {
                        role = "slave";
                        // master host and port are separated by a space
                        i += 1;
                        String[] masterInput = args[i].split(" ");
                        if (masterInput.length == 2) {
                            masterHost = masterInput[0];
                            try {
                                masterPort = Integer.parseInt(masterInput[1]);
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid master port number. Using default port 6379.");
                            }
                        }
                    }
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    break;
            }
        }
        commandFactory = new CommandFactory(dataStore, expiryStore, dir, dbfilename,
                role, replicationId, replicationOffset,replicaOffsets,streamDataStore, lastEntryIdStore);
        RDBLoader rdbLoader = new RDBLoader(dir, dbfilename, dataStore, expiryStore);
        rdbLoader.load();
        ServerConfig config = new ServerConfig(port, role, masterHost, masterPort);
        RedisServer server = RedisServer.getInstance(commandFactory, config);
        server.start();

    }

}


