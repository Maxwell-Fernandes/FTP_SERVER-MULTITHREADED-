import java.io.*;
import java.net.Socket;

class DownloadThread extends Thread {
    private final String filename;
    private final long startByte, endByte;
    private final int threadId;
    private static final int BUFFER_SIZE = 8192;

    public DownloadThread(String filename, long startByte, long endByte, int threadId) {
        this.filename = filename;
        this.startByte = startByte;
        this.endByte = endByte;
        this.threadId = threadId;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket("127.0.0.1", 2121);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             RandomAccessFile raf = new RandomAccessFile(filename, "rw")) {

            dos.writeUTF("DOWNLOAD " + filename + " " + startByte + " " + endByte);
            String response = dis.readUTF();
            if (!response.equals("OK")) return;

            long chunkSize = dis.readLong();
            byte[] buffer = new byte[BUFFER_SIZE];
            long totalRead = 0;

            raf.seek(startByte);
            while (totalRead < chunkSize) {
                int bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, chunkSize - totalRead));
                if (bytesRead == -1) {
                    break;
                }
                raf.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            System.out.println("[Thread " + threadId + "] Downloaded: " + startByte + " to " + endByte);

            try {
                String confirmation = dis.readUTF();
                System.out.println("[Thread " + threadId + "] Received: " + confirmation);
                if (!confirmation.equals("CHUNK_RECEIVED")) {
                    System.err.println("[Thread " + threadId + "] ERROR: Chunk not received properly!");
                }
            } catch (IOException e) {
                System.err.println("[Thread " + threadId + "] ERROR: Did not receive CHUNK_RECEIVED");
            }

        } catch (IOException e) {
            System.err.println("[Thread " + threadId + "] Error: " + e.getMessage());
        }
    }
}
