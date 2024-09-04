package commands;

import java.nio.ByteBuffer;
import java.util.Map;

public class KeysCommand implements Command {
    private final Map<String, String> dataStore;

    public KeysCommand(Map<String, String> dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public ByteBuffer execute(String[] args) {
        StringBuilder response = new StringBuilder();
        response.append("*").append(dataStore.size()).append("\r\n");
        for (String key : dataStore.keySet()) {
            response.append("$").append(key.length()).append("\r\n").append(key).append("\r\n");
        }
        return ByteBuffer.wrap(response.toString().getBytes());
    }
}