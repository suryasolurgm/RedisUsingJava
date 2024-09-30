package commands;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

public class XRangeCommand implements Command {
    private final Map<String, TreeMap<String, Map<String, String>>> streamDataStore;

    public XRangeCommand(Map<String, TreeMap<String, Map<String, String>>> streamDataStore) {
        this.streamDataStore = streamDataStore;
    }

    @Override
    public ByteBuffer execute(String[] args) {
        if (args.length != 4) {
            return ByteBuffer.wrap("-ERR wrong number of arguments for 'XRANGE' command\r\n".getBytes());
        }

        String streamKey = args[1];
        String startId = normalizeId(args[2]);
        System.out.println("startId: "+startId);
        String endId = normalizeId(args[3]);

        TreeMap<String, Map<String, String>> stream = streamDataStore.get(streamKey);
        if (stream == null || stream.isEmpty()) {
            return ByteBuffer.wrap("*0\r\n".getBytes());
        }

        StringBuilder response = new StringBuilder();
        Map<String, Map<String, String>> range = stream.subMap(startId, true, endId, true);

        response.append("*").append(range.size()).append("\r\n");

        for (Map.Entry<String, Map<String, String>> entry : range.entrySet()) {
            String entryId = entry.getKey();
            Map<String, String> fields = entry.getValue();

            response.append("*2\r\n");
            response.append("$").append(entryId.length()).append("\r\n").append(entryId).append("\r\n");

            response.append("*").append(fields.size() * 2).append("\r\n");
            for (Map.Entry<String, String> field : fields.entrySet()) {
                response.append("$").append(field.getKey().length()).append("\r\n").append(field.getKey()).append("\r\n");
                response.append("$").append(field.getValue().length()).append("\r\n").append(field.getValue()).append("\r\n");
            }
        }

        return ByteBuffer.wrap(response.toString().getBytes());
    }

    private String normalizeId(String id) {
        if (!id.contains("-")) {
            return id + "-0";
        }
        return id;
    }
}
