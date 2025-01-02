package at.technikum_wien;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class Main {
    public static void main(String[] args) {

        int port = 10001;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server gestartet und hört auf Port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wartet auf eingehende Verbindungen
                // Für jede Verbindung einen neuen Thread starten
                new Thread(new ClientHandler(clientSocket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
