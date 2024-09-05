import factories.CommandFactory;
import java.util.HashMap;
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
        RDBLoader rdbLoader = new RDBLoader(dir, dbfilename, dataStore);
        rdbLoader.load();

        Server server = new Server(commandFactory, port);
        server.start();

    }

}


