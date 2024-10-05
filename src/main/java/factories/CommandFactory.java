package factories;

import commands.*;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import server.RedisServer;

public class CommandFactory {
    private final Map<String, Command> commandMap = new HashMap<>();

    public CommandFactory(Map<String, String> dataStore, Map<String, Long> expiryStore,String dir, String dbfilename, String role,
                          String replicationId, long replicationOffset,Map<SocketChannel, Long> replicaOffsets, Map<String, TreeMap<String, Map<String, String>>> streamDataStore,Map<String,String> lastEntryIdStore) {
        commandMap.put("PING", new PingCommand());
        commandMap.put("ECHO", new EchoCommand());
        commandMap.put("SET", new SetCommand(dataStore, expiryStore));
        commandMap.put("GET", new GetCommand(dataStore, expiryStore));
        commandMap.put("CONFIG", new ConfigGetCommand(dir, dbfilename));
        commandMap.put("KEYS", new KeysCommand(dataStore));
        commandMap.put("INFO", new InfoCommand(role, replicationId, replicationOffset));
        commandMap.put("REPLCONF", new ReplconfCommand(replicaOffsets));
        commandMap.put("PSYNC", new PsyncCommand(replicationId, replicationOffset));
        commandMap.put("WAIT", new WaitCommand());
        commandMap.put("TYPE", new TypeCommand(dataStore, streamDataStore));
        commandMap.put("XADD", new XAddCommand(streamDataStore,lastEntryIdStore ));
        commandMap.put("XRANGE", new XRangeCommand(streamDataStore));
        commandMap.put("XREAD", new XReadCommand(streamDataStore));
        commandMap.put("INCR", new IncrCommand(dataStore));
        commandMap.put("MULTI", new MultiCommand());
        commandMap.put("EXEC", new ExecCommand());
    }
    public boolean isWriteCommand(String commandName) {
        switch (commandName.toUpperCase()) {
            case "SET":
            case "DEL":
            case "XADD":
            case "XRANGE":
            case "XREAD":
            case "INCR":
            case "MULTI":
            case "EXEC":
                return true;
            default:
                return false;
        }
    }
    public Command getCommand(String commandName) {
        return commandMap.get(commandName.toUpperCase());
    }
}
