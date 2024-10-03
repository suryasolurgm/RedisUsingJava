// src/main/java/commands/IncrCommand.java
package commands;

import java.nio.ByteBuffer;
import java.util.Map;

public class IncrCommand implements Command {
    private final Map<String, String> dataStore;

    public IncrCommand(Map<String, String> dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public ByteBuffer execute(String[] args) {
        if (args.length != 2) {
            return ByteBuffer.wrap("-ERR wrong number of arguments for 'incr' command\r\n".getBytes());
        }

        String key = args[1];
        String value = dataStore.get(key);

        if (value == null) {
            return ByteBuffer.wrap("-ERR no such key\r\n".getBytes());
        }

        try {
            int intValue = Integer.parseInt(value);
            intValue++;
            dataStore.put(key, String.valueOf(intValue));
            return ByteBuffer.wrap((":" + intValue + "\r\n").getBytes());
        } catch (NumberFormatException e) {
            return ByteBuffer.wrap("-ERR value is not an integer or out of range\r\n".getBytes());
        }
    }
}
