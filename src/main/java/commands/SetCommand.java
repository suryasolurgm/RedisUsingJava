package commands;

import java.nio.ByteBuffer;
import java.util.Map;

public class SetCommand implements Command {
    private final Map<String, String> dataStore;
    private final Map<String, Long> expiryStore;

    public SetCommand(Map<String, String> dataStore, Map<String, Long> expiryStore) {
        this.dataStore = dataStore;
        this.expiryStore = expiryStore;
    }

    @Override
    public ByteBuffer execute(String[] args) {
        if (args.length > 2) {
            String keyName = args[1];
            String value = args[2];
            dataStore.put(keyName, value);
            if (args.length > 4 && args[3].equalsIgnoreCase("PX")) {
                long expiryTime = System.currentTimeMillis() + Long.parseLong(args[4]);
                expiryStore.put(keyName, expiryTime);
            }
            return ByteBuffer.wrap("+OK\r\n".getBytes());
        }
        return ByteBuffer.wrap("-ERR wrong number of arguments for 'set' command\r\n".getBytes());
    }
}
