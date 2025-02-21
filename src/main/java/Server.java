import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Server <IP_ADDRESS> <PORT>");
            return;
        }

        String serverIP = args[0];
        int port = Integer.parseInt(args[1]); // Now correctly parsing the port

        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(serverIP))) {
            System.out.println("Server is listening on " + serverIP + ":" + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from " + clientSocket.getInetAddress());

                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println("Hello! Send a string to be capitalized.");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equalsIgnoreCase("bye")) {
                    out.println("disconnected");
                    break;
                } else if (!inputLine.matches("[a-zA-Z]+")) {
                    out.println("Invalid input. Only alphabets allowed.");
                } else {
                    out.println(inputLine.toUpperCase());
                }
            }

            System.out.println("Client disconnected.");
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
