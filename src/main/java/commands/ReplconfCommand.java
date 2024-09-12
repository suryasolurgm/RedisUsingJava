package commands;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class ReplconfCommand implements Command {


    @Override
    public ByteBuffer execute(String[] args) {

        return ByteBuffer.wrap("+OK\r\n".getBytes());

    }


}