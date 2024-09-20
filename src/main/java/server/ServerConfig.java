package server;

public class ServerConfig {
    private final int port;
    private final String role;
    private final String masterHost;
    private final int masterPort;

    public ServerConfig(int port, String role, String masterHost, int masterPort) {
        this.port = port;
        this.role = role;
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }

    public int getPort() { return port; }
    public boolean isSlave() { return "slave".equals(role); }
    public String getMasterHost() { return masterHost; }
    public int getMasterPort() { return masterPort; }
}
