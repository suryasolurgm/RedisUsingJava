package commands;

import java.nio.ByteBuffer;

public class ReplconfCommand implements Command {
    @Override
    public ByteBuffer execute(String[] args) {
        return ByteBuffer.wrap("+OK\r\n".getBytes());
    }
}