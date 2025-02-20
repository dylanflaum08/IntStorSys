import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Client <server_host> <port_number>");
            return;
        }

        String serverHost = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket socket = new Socket(serverHost, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server");

            String serverMessage = in.readLine();
            System.out.println("Server: " + serverMessage);

            while (true) {
                System.out.print("Enter a string (or 'bye' to exit): ");
                String userInput = scanner.nextLine();

                // Start timing before sending
                long startTime = System.nanoTime();

                out.println(userInput);
                String response = in.readLine();

                // Stop timing after receiving response
                long endTime = System.nanoTime();
                long roundTripTime = (endTime - startTime) / 1_000_000;

                if ("disconnected".equalsIgnoreCase(response)) {
                    System.out.println("Server disconnected.");
                    break;
                }
                System.out.println("Start time: " + startTime);
                System.out.println("End time: " + endTime);
                System.out.println("Server response: " + response);
                System.out.println("Round-trip time: " + roundTripTime + " ms"); // Print measured time
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
