package commands;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReplconfCommand implements Command {
    private final Map<SocketChannel, Long> replicaOffsets;

    public ReplconfCommand(Map<SocketChannel, Long> replicaOffsets) {
        this.replicaOffsets = replicaOffsets;
    }

    @Override
    public ByteBuffer execute(String[] args) {
        return ByteBuffer.wrap("+OK\r\n".getBytes());

    }


}