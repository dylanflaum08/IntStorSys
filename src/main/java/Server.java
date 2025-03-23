import java.io.*;
import java.net.*;

public class Server {
    // Predefined folder where BMP files are stored
    public static final String BASE_DIR = "files" + File.separator;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Server <port_number>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected from " + socket.getInetAddress());
                // Handle each client in its own thread
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (
                // For text-based communication
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                // For sending file data
                OutputStream dataOut = socket.getOutputStream();
        ) {
            // Send initial greeting
            out.println("Hello! Send a file name to retrieve.");

            String fileName;
            while ((fileName = in.readLine()) != null) {
                System.out.println("Received request: " + fileName);
                if (fileName.equalsIgnoreCase("bye")) {
                    out.println("disconnected");
                    break;
                }

                // Look for the file in the predetermined folder
                File file = new File(Server.BASE_DIR + fileName);
                if (!file.exists() || file.isDirectory()) {
                    out.println("File not found");
                    continue;
                }

                // File exists: send header with file size
                long fileSize = file.length();
                out.println("FOUND " + fileSize);

                // Send the file bytes
                try (FileInputStream fileIn = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileIn.read(buffer)) != -1) {
                        dataOut.write(buffer, 0, bytesRead);
                    }
                    dataOut.flush();
                    System.out.println("Sent file: " + fileName + " (" + fileSize + " bytes)");
                } catch (IOException e) {
                    out.println("Error reading file: " + e.getMessage());
                }
            }

            System.out.println("Client disconnected: " + socket.getInetAddress());
            socket.close();
        } catch (IOException e) {
            System.err.println("ClientHandler exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
