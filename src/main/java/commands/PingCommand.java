package commands;

import java.nio.ByteBuffer;

public class PingCommand implements Command {
    @Override
    public ByteBuffer execute(String[] args) {
        return ByteBuffer.wrap("+PONG\r\n".getBytes());
    }
}
