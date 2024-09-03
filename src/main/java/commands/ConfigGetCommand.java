package commands;

import java.nio.ByteBuffer;

public class ConfigGetCommand implements Command {
    private final String dir;
    private final String dbfilename;

    public ConfigGetCommand(String dir, String dbfilename) {
        this.dir = dir;
        this.dbfilename = dbfilename;
    }

    @Override
    public ByteBuffer execute(String[] args) {
        if (args.length > 2 && "GET".equalsIgnoreCase(args[1])) {
            String param = args[2];
            String value;
            if ("dir".equalsIgnoreCase(param)) {
                value = dir;
            } else if ("dbfilename".equalsIgnoreCase(param)) {
                value = dbfilename;
            } else {
                return ByteBuffer.wrap(("-ERR unknown parameter '" + param + "'\r\n").getBytes());
            }
            String response = "*2\r\n$" + param.length() + "\r\n" + param + "\r\n$" + value.length() + "\r\n" + value + "\r\n";
            return ByteBuffer.wrap(response.getBytes());
        }
        return ByteBuffer.wrap("-ERR wrong number of arguments for 'CONFIG GET' command\r\n".getBytes());
    }
}