package commands;

import java.nio.ByteBuffer;

public class WaitCommand implements Command {
    @Override
    public ByteBuffer execute(String[] args) {
        String response = ":0\r\n"; // RESP Integer format for 0
        return ByteBuffer.wrap(response.getBytes());
    }
}
