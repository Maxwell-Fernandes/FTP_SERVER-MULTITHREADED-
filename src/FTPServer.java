import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

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
            while (true){
                Socket clientSocket = serverSocket.accept();
                out.println("client connected "+ clientSocket.getInetAddress());

                new Thread(new FTPclienthandler(clientSocket)).start();
            }
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
            String command=in.readUTF();
            if(command.startsWith("UPLOAD")){
                recieveFile(in,command.substring(7));
            }else if ( command.startsWith("DOWNLOAD")){

                sendFile(out,command.substring((7)));
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

    private void sendFile(DataOutputStream out, String substring) throws IOException {
        File file = new File(dir + "/"+substring);
        if(!file.exists()){
            out.writeUTF("file not exist");
            return ;
        }
        out.writeUTF("ok");
        out.writeLong(file.length());

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))){
            byte[] buffer = new byte[8192];
            int bytesread;
            while ((bytesread = bis.read(buffer))!=-1){
                out.write(buffer,0,bytesread);
            }
        }
        System.out.println("File sent "+substring);
    }

    private void recieveFile(DataInputStream in, String filename) throws IOException {
        File file = new File(dir,filename);
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
