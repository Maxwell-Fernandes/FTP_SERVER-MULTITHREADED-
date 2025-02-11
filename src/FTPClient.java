import java.io.*;
import java.net.*;

public class FTPClient {
    private static final String SERVER_IP = "127.0.0.1"; // Change this if running on another machine
    private static final int PORT = 2121;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, PORT);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to FTP server.");
            System.out.print("Enter command (UPLOAD filename / DOWNLOAD filename): ");
            String command = reader.readLine();
            dos.writeUTF(command);

            if (command.startsWith("UPLOAD")) {
                sendFile(dos, command.substring(7));
            } else if (command.startsWith("DOWNLOAD")) {
                receiveFile(dis, command.substring(9));
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Uploads a file to the FTP server.
     */
    private static void sendFile(DataOutputStream dos, String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        dos.writeLong(file.length());

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        }
        System.out.println("File uploaded successfully: " + fileName);
    }

    /**
     * Downloads a file from the FTP server.
     */
    private static void receiveFile(DataInputStream dis, String fileName) throws IOException {
        String response = dis.readUTF();
        if (!"OK".equals(response)) {
            System.out.println("Error: " + response);
            return;
        }

        long fileSize = dis.readLong();
        File file = new File(fileName);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;

            while (totalRead < fileSize && (bytesRead = dis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
        }
        System.out.println("File downloaded successfully: " + fileName);
    }
}
