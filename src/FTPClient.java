import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FTPClient {
    private static final String SERVER_IP = "127.0.0.1"; // Change if running on a different machine
    private static final int PORT = 2121;
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_THREADS = 4; // Adjust as needed

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, PORT);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to FTP server.");
            while (true) {
                System.out.print("Enter command (UPLOAD filename / DOWNLOAD filename / LIST / EXIT): ");
                String command = reader.readLine().trim();
                dos.writeUTF(command);

                if (command.startsWith("UPLOAD")) {
                    sendFile(dos, command.substring(7).trim());
                } else if (command.startsWith("DOWNLOAD")) {
                    receiveFile(dis, dos, command.substring(9).trim());
                } else if (command.equalsIgnoreCase("LIST")) {
                    listFiles(dis);
                } else if (command.equalsIgnoreCase("EXIT")) {
                    System.out.println("Exiting...");
                    break;
                } else {
                    System.out.println("Invalid command. Please try again.");
                }

                String serverResponse = dis.readUTF();
                System.out.println("[Server Response] " + serverResponse);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void listFiles(DataInputStream dis) throws IOException {

      while (true) {

          String response = dis.readUTF();
//          if(dis.available()==0){
//              break;
//          }
          if(response.equals("No files available")){
              System.out.println("NO files available");
              break;
          }
          if (response.equals("END_OF_LIST")){
              break;
          }
          System.out.println("- " + response);

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
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        }
        System.out.println("File uploaded successfully: " + fileName);
    }

    /**
     * Downloads a file from the FTP server using multiple threads.
     */
    private static void receiveFile(DataInputStream dis, DataOutputStream dos, String fileName) throws IOException, InterruptedException {
        dos.writeUTF("DOWNLOAD " + fileName);
        String response = dis.readUTF();

        if (!response.equals("OK")) {
            System.out.println("Server error: " + response);
            return;
        }

        long fileSize = dis.readLong();
        System.out.println("File size: " + fileSize + " bytes");

        File file = new File(fileName);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        int numThreads = (int) Math.min(MAX_THREADS, fileSize / (5 * 1024 * 1024) + 1);
        long chunkSize = fileSize / numThreads;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(fileSize); // Pre-allocate file size
        }

        for (int i = 0; i < numThreads; i++) {
            long startByte = i * chunkSize;
            long endByte = (i == numThreads - 1) ? fileSize - 1 : startByte + chunkSize - 1;
            executor.execute(new DownloadThread(fileName, startByte, endByte, i));
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(100);
        }
        response= dis.readUTF();
        System.out.println(response);
        System.out.println("Download complete: " + fileName);
    }
}
