// src/main/java/commands/ExecCommand.java
package commands;

import javax.swing.plaf.PanelUI;
import java.nio.ByteBuffer;
import java.security.PublicKey;

public class ExecCommand implements Command {


    @Override
    public ByteBuffer execute(String[] args) {

        return ByteBuffer.wrap("*0\r\n".getBytes());
    }

}
