package server;

public class Logger {
    public static void info(String message) {
        System.out.println("[INFO] " + message);
    }

    public static void error(String message, Throwable e) {
        System.err.println("[ERROR] " + message);
        e.printStackTrace();
    }
}
