package server;

public class CommandParser {
    public static String[] parseRESP(String command) {
        String[] lines = command.split("\r\n");
        if (lines.length < 3 || !lines[0].startsWith("*")) {
            return null;
        }

        int numArgs = Integer.parseInt(lines[0].substring(1));
        String[] result = new String[numArgs];
        int index = 0;

        for (int i = 1; i < lines.length; i++) {
            if (lines[i].startsWith("$")) {
                result[index++] = lines[++i];
            }
        }

        return result;
    }
}
