import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8050);
        System.out.println("FTP server is running");

        while (true){
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected at "+ clientSocket.getInetAddress().getHostAddress());

        new Thread(new ClientHandler(clientSocket)).start();
    }
        }
}

class ClientHandler implements Runnable{
    private Socket socket;
    public ClientHandler(Socket clientSocket) {
        socket=clientSocket;
    }

    @Override
    public void run() {
//        try {
//            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            PrintWriter out = new PrintWriter(socket.getOutputStream());
//
//            String inputLine;
//            while((inputLine=in.readLine())!=null){
//                if(".".equals(inputLine)){
//                    break;
//                }
//                out.println("echo "+ inputLine);
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }finally {
//            try {
//                socket.close();
//                System.out.println("Socket closed");
//            } catch (IOException e) {
//                System.out.println("Error while closing socket");
//                throw new RuntimeException(e);
//            }
//        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            String command;
            while ((command=in.readLine())!=null){
                String[] parts = command.split(" ");
                switch (parts[0].toUpperCase()){
                    case "USER":
                        out.println("user 1 OK");
                        break;
                    case "PASS":
                        out.println("login successful");
//                        TODO: actual implementation of login
                        break;
                    case "LIST":


                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
