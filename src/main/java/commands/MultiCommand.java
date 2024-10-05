// src/main/java/commands/MultiCommand.java
package commands;

import java.nio.ByteBuffer;

public class MultiCommand implements Command {
    @Override
    public ByteBuffer execute(String[] args) {
        return ByteBuffer.wrap("+OK\r\n".getBytes());
    }
}
