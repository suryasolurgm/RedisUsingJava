package commands;

import java.nio.ByteBuffer;

public interface Command {
    ByteBuffer execute(String[] args);
}
