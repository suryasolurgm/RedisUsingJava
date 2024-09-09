package commands;

import java.nio.ByteBuffer;

public class PsyncCommand implements Command {
    private final String replicationId;
    private final long replicationOffset;

    public PsyncCommand(String replicationId, long replicationOffset) {
        this.replicationId = replicationId;
        this.replicationOffset = replicationOffset;
    }

    @Override
    public ByteBuffer execute(String[] args) {
        String response = "+FULLRESYNC " + replicationId + " " + replicationOffset + "\r\n";
        return ByteBuffer.wrap(response.getBytes());
    }
}
