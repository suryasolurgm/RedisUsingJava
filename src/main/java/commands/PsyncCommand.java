package commands;

import java.nio.ByteBuffer;
import java.util.Base64;

public class PsyncCommand implements Command {
    private final String replicationId;
    private final long replicationOffset;
    private static final String EMPTY_RDB_BASE64 = "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";

    public PsyncCommand(String replicationId, long replicationOffset) {
        this.replicationId = replicationId;
        this.replicationOffset = replicationOffset;
    }

    @Override
    public ByteBuffer execute(String[] args) {
        String fullResyncResponse = "+FULLRESYNC " + replicationId + " " + replicationOffset + "\r\n";
        ByteBuffer fullResyncBuffer = ByteBuffer.wrap(fullResyncResponse.getBytes());

        // Decode the base64 encoded RDB file
        byte[] rdbFileBytes = Base64.getDecoder().decode(EMPTY_RDB_BASE64);
        String rdbFileHeader = "$" + rdbFileBytes.length + "\r\n";
        ByteBuffer rdbFileHeaderBuffer = ByteBuffer.wrap(rdbFileHeader.getBytes());
        ByteBuffer rdbFileBuffer = ByteBuffer.wrap(rdbFileBytes);

        // Combine the buffers
        ByteBuffer combinedBuffer = ByteBuffer.allocate(fullResyncBuffer.remaining() + rdbFileHeaderBuffer.remaining() + rdbFileBuffer.remaining());
        combinedBuffer.put(fullResyncBuffer);
        combinedBuffer.put(rdbFileHeaderBuffer);
        combinedBuffer.put(rdbFileBuffer);
        combinedBuffer.flip();

        return combinedBuffer;
    }
}
