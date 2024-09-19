package commands;

import server.RedisServer;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.*;


public class WaitCommand implements Command, Callable<ByteBuffer> {
    private  RedisServer server;
    private SocketChannel clientChannel;
    public WaitCommand() {

    }

    @Override
    public ByteBuffer execute(String[] args) {

        return ByteBuffer.wrap("not ok".getBytes());

    }

    public void setServer(RedisServer redisServer) {
        this.server = redisServer;
    }
    public void setClientChannel(SocketChannel clientChannel) {
        this.clientChannel = clientChannel;
    }

    @Override
    public ByteBuffer call() throws Exception {
        try {
            long ackCount = 0;
            ackCount = server.getProcessedReplicaCount(server.getCurrentOffset());
            if(ackCount==0){
                ackCount = server.getReplicaCount();
            }
            String response = ":" + ackCount + "\r\n"; // RESP Integer format
            clientChannel.write(ByteBuffer.wrap(response.getBytes()));
            return ByteBuffer.wrap(response.getBytes());
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

}
