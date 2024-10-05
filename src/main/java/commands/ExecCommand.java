// src/main/java/commands/ExecCommand.java
package commands;

import javax.swing.plaf.PanelUI;
import java.nio.ByteBuffer;
import java.security.PublicKey;

public class ExecCommand implements Command {
    private  boolean inTransaction = false;

    @Override
    public ByteBuffer execute(String[] args) {
        if (!inTransaction) {
            return ByteBuffer.wrap("-ERR EXEC without MULTI\r\n".getBytes());
        }
        // Placeholder for future implementation when MULTI has been called
        return ByteBuffer.wrap("+OK\r\n".getBytes());
    }
    public void setInTransaction(boolean inTransaction) {
        this.inTransaction = inTransaction;
    }
}
