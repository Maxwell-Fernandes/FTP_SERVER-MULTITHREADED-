import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLOutput;

import static java.lang.System.out;
import static java.lang.System.setOut;

public class FTPServer {

    static final int port = 2121;
    static final String dir ="files";

    public static void main(String[] args) throws IOException {
        File diretory = new File(dir);
        if(!diretory.exists()){
            diretory.mkdir();
        }

        try(ServerSocket serverSocket = new ServerSocket(port)){
            out.println("Server started on port "+port);
                Socket clientSocket = serverSocket.accept();
                out.println("client connected "+ clientSocket.getInetAddress());

                new Thread(new FTPclienthandler(clientSocket)).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class FTPclienthandler implements Runnable{
    private Socket clientSocket;
    private static final String dir = "files";

    FTPclienthandler(Socket clientSocket){
        this.clientSocket= clientSocket;
    }
    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream((clientSocket.getOutputStream()))
        ) {
            while (true) {
                String command = in.readUTF();
                if (command.startsWith("UPLOAD")) {
                    recieveFile(in, command.substring(7));
                } else if (command.startsWith("DOWNLOAD")) {

                    sendFile(out, command.substring((9)));
                } else if (command.startsWith("LIST")) {
                    listFiles(out);
                }else if(command.startsWith("EXIT")){
                    System.out.println("Exiting");
                    break;
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            try
            {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void listFiles(DataOutputStream out) throws IOException {
        File folder = new File("files");
        File []filesList = folder.listFiles();
        if(filesList==null || filesList.length == 0){
            out.writeUTF("NO files found");
            out.flush();
            return ;
        }
//        StringBuilder flist = new StringBuilder("Available files \n");

        for(File file : filesList){
//            flist.append("-").append(file.getName()).append("\n");
            out.writeUTF(file.getName());
        }
        out.writeUTF("END_OF_FILE");
        out.flush();
        System.out.println("listed sucdessfully");
    }

    private void sendFile(DataOutputStream out, String substring) throws IOException {
        substring = substring.trim();
        File file = new File(dir + "/"+substring);
        System.out.println("Looking for files in "+file.getAbsolutePath());
        if(!file.exists()){
            out.writeUTF("file not exist");
            return ;
        }
        out.writeUTF("OK");
        out.writeLong(file.length());

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))){
            byte[] buffer = new byte[8192];
            int bytesread;
            while ((bytesread = bis.read(buffer))!=-1){
                out.write(buffer,0,bytesread);
            }
        }
        out.writeUTF("OK");
        System.out.println("File sent "+substring);
    }

    private void recieveFile(DataInputStream in, String filename) throws IOException {
        filename=filename.trim();
        File file = new File(dir,filename);
        out.println("recieving files in "+file.getAbsolutePath());
        try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))){
            long fileSize = in.readLong();
            byte[] buffer = new byte[8192];
            int byteRead;
            int totalRead = 0;

            while(totalRead<fileSize && (byteRead = in.read(buffer))!=-1){
                bos.write(buffer,0,byteRead);
                totalRead+=byteRead;
            }
        }
        out.println("file recieved "+filename);
    }
}
