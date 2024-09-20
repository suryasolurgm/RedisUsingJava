package server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ConnectionManager {
    public void closeConnection(SelectionKey key, SocketChannel clientSocket) {
        try {
            Logger.info("Client disconnected: " + clientSocket.getRemoteAddress());
            key.cancel();
            clientSocket.close();
        } catch (IOException e) {
            Logger.error("Error closing connection: " + e.getMessage(), e);
        }
    }
}
