package commands;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class InfoCommand implements Command {
    private final String role;

    public InfoCommand(String role) {
        this.role = role;
    }
    @Override
    public ByteBuffer execute(String[] args) {
        String response;
        if (args.length > 0 && "replication".equalsIgnoreCase(args[1])) {
            response = "role:" + role;
        } else {
            return ByteBuffer.wrap("$-1\r\n".getBytes()); // Return null bulk string if the argument is not "replication"
        }
        String bulkString = "$" + response.length() + "\r\n" + response + "\r\n";
        return ByteBuffer.wrap(bulkString.getBytes(StandardCharsets.UTF_8));
    }
}
