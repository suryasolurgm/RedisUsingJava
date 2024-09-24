package commands;

import java.nio.ByteBuffer;
import java.util.Map;

public class TypeCommand implements Command {
    private final Map<String, String> dataStore;
    private final Map<String, Map<String, String>> streamDataStore;

    public TypeCommand(Map<String, String> dataStore, Map<String, Map<String, String>> streamDataStore) {
        this.dataStore = dataStore;
        this.streamDataStore = streamDataStore;
    }

    @Override
    public ByteBuffer execute(String[] args) {
        if (args.length != 2) {
            return ByteBuffer.wrap("-ERR wrong number of arguments for 'TYPE' command\r\n".getBytes());
        }
        String key = args[1];
        String response;

        if (dataStore.containsKey(key)) {
            response = "+string\r\n";
        } else if (streamDataStore.containsKey(key)) {
            response = "+stream\r\n";
        } else {
            response = "+none\r\n";
        }

        return ByteBuffer.wrap(response.getBytes());
    }
}
