import java.io.*;
import java.net.*;

public class FTPClient {
    private static final String SERVER_IP = "127.0.0.1"; // Change this if running on another machine
    private static final int PORT = 2121;

    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket(SERVER_IP, PORT);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to FTP server.");
            while (true) {
                System.out.print("Enter command (UPLOAD filename / DOWNLOAD filename / LIST / EXIT): ");
                String command = reader.readLine();
                dos.writeUTF(command);

                if (command.startsWith("UPLOAD")) {
                    sendFile(dos, command.substring(7));
                } else if (command.startsWith("DOWNLOAD")) {
                    receiveFile(dis, command.substring(9));
                } else if (command.startsWith("LIST")) {
                    Lists(dis);
                }else if (command.equalsIgnoreCase("EXIT")){
                    System.out.println("Exiting....");
                    break;
                }
                String serverResponse = dis.readUTF();
                System.out.println("[Server Response] "+ serverResponse);
            }
            } catch(IOException e){
                System.err.println("Error: " + e.getMessage());
            }

    }

    private static void Lists(DataInputStream dis) throws IOException {
        while(true){
            String response = dis.readUTF();
            if(response.equals("END_OF_FILE")){
                break;
            }
            System.out.println("-"+ response);
        }


    }

    /**
     * Uploads a file to the FTP server.
     */
    private static void sendFile(DataOutputStream dos, String fileName) throws IOException {
        fileName = fileName.trim();
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
        fileName = fileName.trim();
        String response = dis.readUTF();
        if (!"OK".equals(response)) {
            System.out.println("Error: " + response);
            return;
        }

        long fileSize = dis.readLong();
        File file = new File(fileName);
        System.out.println("[Client] Downloading "+ fileName);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;

            while (totalRead < fileSize && (bytesRead = dis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                int progress = (int) ((int)(totalRead * 100)/fileSize);
                System.out.println("Progress :"+progress+"%");
            }
        }
        System.out.println("[Client] File downloaded successfully: " + fileName);
    }
}
