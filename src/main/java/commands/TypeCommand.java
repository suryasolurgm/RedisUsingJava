package commands;

import java.nio.ByteBuffer;
import java.util.Map;

public class TypeCommand implements Command {
    private final Map<String, String> dataStore;

    public TypeCommand(Map<String, String> dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public ByteBuffer execute(String[] args) {
        String key = args[1];
        String response;

        if (dataStore.containsKey(key)) {
            response = "+string\r\n";
        } else {
            response = "+none\r\n";
        }

        return ByteBuffer.wrap(response.getBytes());
    }
}
