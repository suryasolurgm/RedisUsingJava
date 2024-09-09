package factories;

import commands.*;

import java.util.HashMap;
import java.util.Map;

public class CommandFactory {
    private final Map<String, Command> commandMap = new HashMap<>();

    public CommandFactory(Map<String, String> dataStore, Map<String, Long> expiryStore,String dir,
                          String dbfilename, String role,String replicationId, long replicationOffset) {
        commandMap.put("PING", new PingCommand());
        commandMap.put("ECHO", new EchoCommand());
        commandMap.put("SET", new SetCommand(dataStore, expiryStore));
        commandMap.put("GET", new GetCommand(dataStore, expiryStore));
        commandMap.put("CONFIG", new ConfigGetCommand(dir, dbfilename));
        commandMap.put("KEYS", new KeysCommand(dataStore));
        commandMap.put("INFO", new InfoCommand(role, replicationId, replicationOffset));
        commandMap.put("REPLCONF", new ReplconfCommand());
        commandMap.put("PSYNC", new PsyncCommand(replicationId, replicationOffset));

    }

    public Command getCommand(String commandName) {
        return commandMap.get(commandName.toUpperCase());
    }
}
