package commands;

import java.nio.ByteBuffer;
import java.util.Map;

public class GetCommand implements Command {
    private final Map<String, String> dataStore;
    private final Map<String, Long> expiryStore;

    public GetCommand(Map<String, String> dataStore, Map<String, Long> expiryStore) {
        this.dataStore = dataStore;
        this.expiryStore = expiryStore;
    }

    @Override
    public ByteBuffer execute(String[] args) {
        if (args.length > 1) {
            String keyName = args[1];
            if (expiryStore.containsKey(keyName) && System.currentTimeMillis() > expiryStore.get(keyName)) {
                dataStore.remove(keyName);
                expiryStore.remove(keyName);
            }
            String value = dataStore.get(keyName);
            if (value != null) {
                String respMessage = "$" + value.length() + "\r\n" + value + "\r\n";
                return ByteBuffer.wrap(respMessage.getBytes());
            } else {
                return ByteBuffer.wrap("$-1\r\n".getBytes());
            }
        }
        return ByteBuffer.wrap("-ERR wrong number of arguments for 'get' command\r\n".getBytes());
    }
}