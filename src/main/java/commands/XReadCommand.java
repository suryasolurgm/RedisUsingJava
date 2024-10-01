package commands;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class XReadCommand implements Command {
    private final Map<String, TreeMap<String, Map<String, String>>> streamDataStore;

    public XReadCommand(Map<String, TreeMap<String, Map<String, String>>> streamDataStore) {
        this.streamDataStore = streamDataStore;
    }

    @Override
    public ByteBuffer execute(String[] args) {
        if (args.length < 4 || !args[1].equals("streams")) {
            return ByteBuffer.wrap("-ERR wrong number of arguments for 'XREAD' command\r\n".getBytes());
        }

        List<String> streamKeys = new ArrayList<>();
        List<String> startIds = new ArrayList<>();

        int midPoint = (args.length - 2) / 2 + 2;
        for (int i = 2; i < midPoint; i++) {
            streamKeys.add(args[i]);
        }
        for (int i = midPoint; i < args.length; i++) {
            startIds.add(args[i]);
        }

        if (streamKeys.size() != startIds.size()) {
            return ByteBuffer.wrap("-ERR wrong number of arguments for 'XREAD' command\r\n".getBytes());
        }

        StringBuilder response = new StringBuilder();
        response.append("*").append(streamKeys.size()).append("\r\n");

        for (int i = 0; i < streamKeys.size(); i++) {
            String streamKey = streamKeys.get(i);
            String startId = startIds.get(i);

            TreeMap<String, Map<String, String>> stream = streamDataStore.get(streamKey);
            if (stream == null || stream.isEmpty()) {
                response.append("*0\r\n");
                continue;
            }

            response.append("*2\r\n$").append(streamKey.length()).append("\r\n").append(streamKey).append("\r\n");

            Map.Entry<String, Map<String, String>> entry = stream.higherEntry(startId);
            if (entry != null) {
                String entryId = entry.getKey();
                Map<String, String> fields = entry.getValue();

                response.append("*1\r\n*2\r\n$").append(entryId.length()).append("\r\n").append(entryId).append("\r\n");
                response.append("*").append(fields.size() * 2).append("\r\n");
                for (Map.Entry<String, String> field : fields.entrySet()) {
                    response.append("$").append(field.getKey().length()).append("\r\n").append(field.getKey()).append("\r\n");
                    response.append("$").append(field.getValue().length()).append("\r\n").append(field.getValue()).append("\r\n");
                }
            } else {
                response.append("*0\r\n");
            }
        }

        return ByteBuffer.wrap(response.toString().getBytes());
    }
}

