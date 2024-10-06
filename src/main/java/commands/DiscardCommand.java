// src/main/java/commands/DiscardCommand.java
package commands;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;

public class DiscardCommand implements Command {
    private  Map<SocketChannel, Boolean> inTransaction;
    private  Map<SocketChannel, List<String[]>> transactionQueue;
    private SocketChannel clientSocket;
    public DiscardCommand(){

    }

    @Override
    public ByteBuffer execute(String[] args) {
        if (inTransaction.get(clientSocket) == null || !inTransaction.get(clientSocket)) {
            return ByteBuffer.wrap("-ERR DISCARD without MULTI\r\n".getBytes());
        }
        inTransaction.put(clientSocket, false);
        transactionQueue.remove(clientSocket);
        return ByteBuffer.wrap("+OK\r\n".getBytes());
    }
    public void setClientSocket(SocketChannel clientSocket) {
        this.clientSocket = clientSocket;
    }
    public void setInTransaction(Map<SocketChannel, Boolean> inTransaction) {
        this.inTransaction = inTransaction;
    }
    public void setTransactionQueue(Map<SocketChannel, List<String[]>> transactionQueue) {
        this.transactionQueue = transactionQueue;
    }
}
