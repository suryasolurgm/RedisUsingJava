package commands;

import java.nio.ByteBuffer;

public class EchoCommand implements Command {
    @Override
    public ByteBuffer execute(String[] args) {
        if (args.length > 1) {
            String message = args[1];
            String respMessage = "$" + message.length() + "\r\n" + message + "\r\n";
            return ByteBuffer.wrap(respMessage.getBytes());
        }
        return ByteBuffer.wrap("-ERR wrong number of arguments for 'echo' command\r\n".getBytes());
    }
}
