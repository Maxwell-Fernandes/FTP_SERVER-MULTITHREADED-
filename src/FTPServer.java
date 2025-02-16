import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FTPServer {
    static final int PORT = 2121;
    static final String DIRECTORY = "files";
    static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) {
        File dir = new File(DIRECTORY);
        if (!dir.exists()) dir.mkdirs();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                threadPool.execute(new FTPClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}

class FTPClientHandler implements Runnable {
    private final Socket clientSocket;

    FTPClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            while (true) {
                System.out.println("Waiting for client response");
                String command = in.readUTF();
                System.out.println("Received command: " + command);

                if (command.startsWith("UPLOAD")) {
                    receiveFile(in, command.substring(7));
                    out.writeUTF("File received successfully");
                } else if (command.startsWith("DOWNLOAD")) {
                    String[] parts = command.split(" ");
                    if (parts.length == 2) {
                        sendFullFile(out, parts[1]);
                    } else if (parts.length == 4) {
                        long startByte = Long.parseLong(parts[2]);
                        long endByte = Long.parseLong(parts[3]);
                        sendChunkedFile(out, parts[1], startByte, endByte);
//                        out.writeUTF("downloaded");
                    }
                } else if (command.equals("LIST")) {
                    listFiles(out);
                } else if (command.equals("EXIT")) {
                    break;
                } else {
                    out.writeUTF("Unknown command");
                }
            }
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        }
    }

    private void sendFullFile(DataOutputStream out, String filename) throws IOException {
        File file = new File(FTPServer.DIRECTORY, filename);
        if (!file.exists()) {
            out.writeUTF("File does not exist");
            return;
        }

        out.writeUTF("OK");
        out.writeLong(file.length());

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[64 * 1024];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        out.writeUTF("FILE_SENT");
        System.out.println("File sent: " + filename);
    }

    private void sendChunkedFile(DataOutputStream out, String filename, long startByte, long endByte) throws IOException {
        File file = new File(FTPServer.DIRECTORY, filename);
        if (!file.exists()) {
            out.writeUTF("File does not exist");
            out.flush();
            return;
        }

        out.writeUTF("OK");
        long chunkSize = endByte - startByte + 1;
        out.writeLong(chunkSize);

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(startByte);
            byte[] buffer = new byte[64 * 1024];
            long totalSent = 0;

            while (totalSent < chunkSize) {
                int bytesToRead = (int) Math.min(buffer.length, chunkSize - totalSent);
                int bytesRead = raf.read(buffer, 0, bytesToRead);
                if (bytesRead == -1){
                    break;
                }
                out.write(buffer, 0, bytesRead);
                out.flush();
                totalSent += bytesRead;

            }
        }

        try {
            out.writeUTF("CHUNK_RECEIVED");
            out.flush();
            System.out.println("Chunk sent: " + startByte + " to " + endByte);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to send CHUNK_RECEIVED for " + startByte + " to " + endByte);
        }
    }

    private void receiveFile(DataInputStream in, String filename) throws IOException {
        File file = new File(FTPServer.DIRECTORY, filename);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            long fileSize = in.readLong();
            byte[] buffer = new byte[64 * 1024];
            long totalRead = 0;

            while (totalRead < fileSize) {
                int bytesToRead = (int) Math.min(buffer.length, fileSize - totalRead);
                int bytesRead = in.read(buffer, 0, bytesToRead);
                if (bytesRead == -1) break;
                bos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
        }
        System.out.println("File received: " + filename);
    }

    private void listFiles(DataOutputStream out) throws IOException {
        File folder = new File(FTPServer.DIRECTORY);
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            out.writeUTF("No files available");
            out.flush();
            return;
        }
        for (File file : files) {
            out.writeUTF(file.getName());
            out.flush();
        }
        out.writeUTF("END_OF_LIST");
        out.flush();
        out.writeUTF("FILES LISTED SUCCESSFULLY");
        out.flush();
        System.out.println("Files Listed Successfully");
    }
}
