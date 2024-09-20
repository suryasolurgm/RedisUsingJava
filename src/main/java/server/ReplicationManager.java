package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReplicationManager {
    private final List<SocketChannel> replicaChannels = new ArrayList<>();
    private final Map<SocketChannel, Long> replicaOffsets = new ConcurrentHashMap<>();
    private long currentOffset = 0;

    public ReplicationManager(ServerConfig config) {
        // Initialize replication-related configurations
    }

    public void addReplicaChannel(SocketChannel channel) {
        replicaChannels.add(channel);
    }

    public void updateReplicaOffset(SocketChannel channel, long offset) {
        replicaOffsets.put(channel, offset);
    }

    public void propagateCommandToReplicas(ByteBuffer buffer) throws IOException {
        currentOffset += buffer.remaining() + (currentOffset == 0 ? 0 : 37);
        for (SocketChannel replicaChannel : replicaChannels) {
            buffer.rewind();
            replicaChannel.write(buffer);
        }
    }

    public void sendGetAckToReplicas() {
        String getAckCommand = "*3\r\n$8\r\nREPLCONF\r\n$6\r\nGETACK\r\n$1\r\n*\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(getAckCommand.getBytes());
        for (SocketChannel replicaChannel : replicaChannels) {
            try {
                buffer.rewind();
                replicaChannel.write(buffer);
            } catch (IOException e) {
                Logger.error("Error sending GETACK to replica: " + e.getMessage(), e);
            }
        }
    }

    public int getProcessedReplicaCount(long requiredOffset) {
        return (int) replicaOffsets.values().stream().filter(offset -> offset >= requiredOffset).count();
    }

    public int getReplicaCount() {
        return replicaChannels.size();
    }

    public long getCurrentOffset() {
        return currentOffset;
    }

    public void addOffset(long offset) {
        currentOffset += offset;
    }

    public List<SocketChannel> getReplicaChannels() {
        return replicaChannels;
    }
}
