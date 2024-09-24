package commands;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XAddCommand implements Command {
    private final Map<String, Map<String, String>> streamDataStore;

    public XAddCommand(Map<String, Map<String, String>> dataStore) {
        this.streamDataStore = dataStore;
    }

    @Override
    public ByteBuffer execute(String[] args) {
        if (args.length < 4 ) {
            return ByteBuffer.wrap("-ERR wrong number of arguments for 'XADD' command\r\n".getBytes());
        }

        String streamKey = args[1];
        String entryId = args[2];
        Map<String, String> stream = streamDataStore.computeIfAbsent(streamKey, k -> new ConcurrentHashMap<>());

        for (int i = 3; i < args.length; i += 2) {
            stream.put(args[i], args[i + 1]);
        }

        String response = "$" + entryId.length() + "\r\n" + entryId + "\r\n";
        return ByteBuffer.wrap(response.getBytes());
    }
}
