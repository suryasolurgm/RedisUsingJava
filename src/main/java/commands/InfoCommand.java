package commands;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class InfoCommand implements Command {
    private final String role;
    private final String replicationId;
    private final long replicationOffset;

    public InfoCommand(String role, String replicationId, long replicationOffset) {
        this.role = role;
        this.replicationId = replicationId;
        this.replicationOffset = replicationOffset;
    }
    @Override
    public ByteBuffer execute(String[] args) {
        String response;
        if (args.length > 0 && "replication".equalsIgnoreCase(args[1])) {
            response = "role:" + role + "\r\n" +
                    "master_replid:" + replicationId + "\r\n" +
                    "master_repl_offset:" + replicationOffset;
        } else {
            return ByteBuffer.wrap("$-1\r\n".getBytes()); // Return null bulk string if the argument is not "replication"
        }
        String bulkString = "$" + response.length() + "\r\n" + response + "\r\n";
        return ByteBuffer.wrap(bulkString.getBytes(StandardCharsets.UTF_8));
    }
}
