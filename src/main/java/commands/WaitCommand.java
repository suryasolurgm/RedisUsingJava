package commands;

import server.RedisServer;

import java.nio.ByteBuffer;


public class WaitCommand implements Command {
    private  RedisServer server;

    public WaitCommand() {

    }

    @Override
    public ByteBuffer execute(String[] args) {
        int replicaCount = server.getReplicaCount();
        String response = ":" + replicaCount + "\r\n"; // RESP Integer format
        return ByteBuffer.wrap(response.getBytes());
    }

    public void setServer(RedisServer redisServer) {
        this.server = redisServer;
    }
}
