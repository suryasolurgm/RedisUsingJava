import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

public class RDBLoader {
    private final String dir;
    private final String dbfilename;
    private final Map<String, String> dataStore;
    private final Map<String, Long> expiryStore;
    public RDBLoader(String dir, String dbfilename, Map<String, String> dataStore,Map<String, Long> expiryStore) {
        this.dir = dir;
        this.dbfilename = dbfilename;
        this.dataStore = dataStore;
        this.expiryStore = expiryStore;
    }

    public void load() {
        File rdbFile = new File(dir, dbfilename);
        if (!rdbFile.exists()) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(rdbFile)) {
            byte[] header = new byte[9];
            if (fis.read(header) != 9 || !new String(header).equals("REDIS0011")) {
                throw new IOException("Invalid RDB file header");
            }

            while (fis.available() > 0) {
                int type = fis.read();
                if (type == 0xFE) { // Database section
                    fis.read(); // Skip database index
                    fis.read(); // Skip table size info
                    fis.read(); // Skip hash table size
                    fis.read(); // Skip expiry time hash table size
                } else if (type == 0xFC || type == 0xFD) { // Expire information
                    byte[] expireBytes = new byte[type == 0xFC ? 8 : 4];
                    fis.read(expireBytes);
                    long expiryTime = type == 0xFC ? ByteBuffer.wrap(expireBytes).order(ByteOrder.LITTLE_ENDIAN).getLong() : ByteBuffer.wrap(expireBytes).order(ByteOrder.LITTLE_ENDIAN).getInt() * 1000L;
                    fis.read();
                    String key = readString(fis);
                    System.out.println("Key: " + key);
                    String value = readString(fis);
                    dataStore.put(key, value);
                    expiryStore.put(key, expiryTime);
                } else if (type == 0x00) { // String value
                    String key = readString(fis);
                    System.out.println("Key: " + key);
                    String value = readString(fis);
                    dataStore.put(key, value);
                } else if (type == 0xFF) { // End of file
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading RDB file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String readString(FileInputStream fis) throws IOException {
        int length = readSize(fis);
        byte[] strBytes = new byte[length];
        fis.read(strBytes);
        return new String(strBytes);
    }

    private int readSize(FileInputStream fis) throws IOException {
        int firstByte = fis.read();
        int size;
        if ((firstByte & 0xC0) == 0x00) {
            size = firstByte & 0x3F;
            System.out.println("Size: " + size);
        } else if ((firstByte & 0xC0) == 0x40) {
            size = ((firstByte & 0x3F) << 8) | fis.read();
        } else if ((firstByte & 0xC0) == 0x80) {
            size = (fis.read() << 24) | (fis.read() << 16) | (fis.read() << 8) | fis.read();
        } else {
            throw new IOException("Invalid size encoding");
        }
        return size;
    }
}
