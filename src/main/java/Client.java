import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;

public class Client {
    // Directory to save downloaded files
    public static final String DOWNLOAD_DIR = "downloads" + File.separator;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Client <serverURL> <port_number>");
            return;
        }

        String serverURL = args[0];
        int port = Integer.parseInt(args[1]);

        // Create download directory if it doesn't exist
        File downloadDir = new File(DOWNLOAD_DIR);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        List<Long> roundTripTimes = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);

        try (Socket socket = new Socket(serverURL, port);
             // For text communication
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             // For receiving file data
             InputStream dataIn = socket.getInputStream()
        ) {
            System.out.println("Connected to server");
            // Read and display the greeting
            String greeting = in.readLine();
            System.out.println("Server: " + greeting);

            while (true) {
                System.out.print("Enter a file name (or 'bye' to exit): ");
                String fileName = scanner.nextLine();

                // For measuring round-trip time, start the timer before sending the file name
                long startTime = System.nanoTime();
                out.println(fileName);

                if (fileName.equalsIgnoreCase("bye")) {
                    String response = in.readLine();
                    if (response != null && response.equalsIgnoreCase("disconnected")) {
                        System.out.println("Server disconnected. Exiting.");
                    }
                    break;
                }

                // Read server's response header
                String response = in.readLine();
                if (response == null) {
                    System.out.println("No response from server. Exiting.");
                    break;
                }

                if (response.startsWith("File not found") || response.startsWith("Error")) {
                    System.out.println("Server: " + response);
                    continue;
                }

                if (response.startsWith("FOUND")) {
                    // Parse the file size from the header ("FOUND <fileSize>")
                    String[] parts = response.split(" ");
                    long fileSize = Long.parseLong(parts[1]);

                    // Prepare to receive the file bytes
                    File outputFile = new File(DOWNLOAD_DIR + fileName);
                    try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[4096];
                        long remaining = fileSize;
                        int bytesRead;
                        while (remaining > 0 && (bytesRead = dataIn.read(buffer, 0, (int)Math.min(buffer.length, remaining))) != -1) {
                            fileOut.write(buffer, 0, bytesRead);
                            remaining -= bytesRead;
                        }
                        fileOut.flush();
                    } catch (IOException e) {
                        System.err.println("Error saving file: " + e.getMessage());
                        continue;
                    }

                    long endTime = System.nanoTime();
                    long roundTripTime = (endTime - startTime) / 1_000_000; // in milliseconds
                    roundTripTimes.add(roundTripTime);

                    System.out.println("File received and saved as: " + outputFile.getAbsolutePath());
                    System.out.println("Round-trip time: " + roundTripTime + " ms");
                }
            }

            // Compute and display statistics if at least one successful transfer occurred
            if (!roundTripTimes.isEmpty()) {
                long min = Collections.min(roundTripTimes);
                long max = Collections.max(roundTripTimes);
                double sum = 0;
                for (Long t : roundTripTimes) {
                    sum += t;
                }
                double mean = sum / roundTripTimes.size();

                double variance = 0;
                for (Long t : roundTripTimes) {
                    variance += (t - mean) * (t - mean);
                }
                variance /= roundTripTimes.size();
                double stdDev = Math.sqrt(variance);

                System.out.println("----- Round-trip Time Statistics (ms) -----");
                System.out.println("Minimum: " + min);
                System.out.println("Maximum: " + max);
                System.out.println("Mean   : " + mean);
                System.out.println("StdDev : " + stdDev);
            }

        } catch (IOException e) {
            System.err.println("Client exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
