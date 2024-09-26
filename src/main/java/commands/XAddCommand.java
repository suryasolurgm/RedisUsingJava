package commands;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XAddCommand implements Command {
    private final Map<String, Map<String, String>> streamDataStore;
    private final Map<String, String> lastEntryIdStore;

    public XAddCommand(Map<String, Map<String, String>> streamDataStore, Map<String, String> lastEntryIdStore) {
        this.streamDataStore = streamDataStore;
        this.lastEntryIdStore = lastEntryIdStore;
    }

    @Override
    public ByteBuffer execute(String[] args) {
        if (args.length < 4) {
            return ByteBuffer.wrap("-ERR wrong number of arguments for 'XADD' command\r\n".getBytes());
        }

        String streamKey = args[1];
        String entryId = args[2];
        if (entryId.equals("*")) {
            entryId = generateNextEntryId(streamKey);
        } else if (entryId.endsWith("-*")) {
            entryId = generateNextEntryId(entryId, streamKey);
        }
        if (!isValidEntryId(entryId, streamKey)) {
            if (entryId.equals("0-0")) {
                return ByteBuffer.wrap(("-ERR The ID specified in XADD must be greater than 0-0\r\n").getBytes());

            }
            return ByteBuffer.wrap(("-ERR The ID specified in XADD is equal or smaller than the target stream top item\r\n").getBytes());
        }

        Map<String, String> stream = streamDataStore.computeIfAbsent(streamKey, k -> new ConcurrentHashMap<>());

        for (int i = 3; i < args.length; i += 2) {
            stream.put(args[i], args[i + 1]);
        }

        lastEntryIdStore.put(streamKey, entryId);

        String response = "$" + entryId.length() + "\r\n" + entryId + "\r\n";
        return ByteBuffer.wrap(response.getBytes());
    }
    private String generateNextEntryId(String streamKey) {
        long millisecondsTime = System.currentTimeMillis();
        long sequenceNumber = 0;

        String lastEntryId = lastEntryIdStore.get(streamKey);
        if (lastEntryId != null) {
            String[] lastIdParts = lastEntryId.split("-");
            long lastMilliseconds = Long.parseLong(lastIdParts[0]);
            long lastSequence = Long.parseLong(lastIdParts[1]);

            if (millisecondsTime == lastMilliseconds) {
                sequenceNumber = lastSequence + 1;
            }
        }

        return millisecondsTime + "-" + sequenceNumber;
    }
    private String generateNextEntryId(String entryId, String streamKey) {
        String[] idParts = entryId.split("-");
        long millisecondsTime = Long.parseLong(idParts[0]);
        long sequenceNumber = 0;

        String lastEntryId = lastEntryIdStore.get(streamKey);
        if (lastEntryId != null) {
            String[] lastIdParts = lastEntryId.split("-");
            long lastMilliseconds = Long.parseLong(lastIdParts[0]);
            long lastSequence = Long.parseLong(lastIdParts[1]);

            if (millisecondsTime == lastMilliseconds) {
                sequenceNumber = lastSequence + 1;
            }
        }

        if (millisecondsTime == 0 && sequenceNumber == 0) {
            sequenceNumber = 1;
        }

        return millisecondsTime + "-" + sequenceNumber;
    }
    private boolean isValidEntryId(String entryId, String streamKey) {
        if (entryId.equals("0-0")) {
            return false;
        }

        String lastEntryId = lastEntryIdStore.get(streamKey);
        if (lastEntryId == null) {
            return true;
        }

        String[] lastIdParts = lastEntryId.split("-");
        String[] newIdParts = entryId.split("-");

        long lastMilliseconds = Long.parseLong(lastIdParts[0]);
        long newMilliseconds = Long.parseLong(newIdParts[0]);

        if (newMilliseconds < lastMilliseconds) {
            return false;
        } else if (newMilliseconds == lastMilliseconds) {
            long lastSequence = Long.parseLong(lastIdParts[1]);
            long newSequence = Long.parseLong(newIdParts[1]);
            return newSequence > lastSequence;
        }

        return true;
    }
}
