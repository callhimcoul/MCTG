package at.technikum_wien;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.technikum_wien.cards.Card;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                InputStream input = clientSocket.getInputStream();
                OutputStream output = clientSocket.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output))
        ) {
            String requestLine = reader.readLine();
            System.out.println("Request Line: " + requestLine);

            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                System.out.println("Header: " + headerLine);
                String[] headerParts = headerLine.split(": ", 2);
                if (headerParts.length == 2) {
                    headers.put(headerParts[0], headerParts[1]);
                }
            }

            String body = "";
            if (headers.containsKey("Content-Length")) {
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                char[] bodyChars = new char[contentLength];
                int read = reader.read(bodyChars, 0, contentLength);
                if (read != -1) {
                    body = new String(bodyChars, 0, read);
                }
            }

            ObjectMapper objectMapper = new ObjectMapper();

            if (requestLine != null && !requestLine.isEmpty()) {
                String[] requestParts = requestLine.split(" ");
                if (requestParts.length < 3) {
                    sendResponse(writer, "Bad Request", 400);
                } else {
                    String method = requestParts[0];
                    String path = requestParts[1];
                    String httpVersion = requestParts[2];

                    if (method.equals("GET") && path.equals("/")) {
                        sendResponse(writer, "Hello, World!", 200);
                    } else if (method.equals("POST") && path.equals("/users")) {
                        handleUserRegistration(body, writer, objectMapper);
                    } else if (method.equals("POST") && path.equals("/sessions")) {
                        handleUserLogin(body, writer, objectMapper);
                    } else if (method.equals("POST") && path.equals("/packages")) {
                        handlePackageCreation(body, headers.get("Authorization"), writer, objectMapper);
                    } else if (method.equals("POST") && path.equals("/transactions/packages")) {
                        handlePackagePurchase(headers.get("Authorization"), writer, objectMapper);
                    } else if (method.equals("GET") && path.equals("/cards")) {
                        handleGetUserCards(headers.get("Authorization"), writer, objectMapper);
                    } else if (method.equals("GET") && path.equals("/deck")) {
                        handleGetDeck(headers.get("Authorization"), writer, objectMapper);
                    } else if (method.equals("PUT") && path.equals("/deck")) {
                        handleSetDeck(body, headers.get("Authorization"), writer, objectMapper);
                    }

                    else if (method.equals("GET") && path.matches("/users/[^/]+")) {
                        String username = path.substring("/users/".length());
                        handleGetUser(username, writer, objectMapper, headers);
                    } else if (method.equals("PUT") && path.matches("/users/[^/]+")) {
                        String username = path.substring("/users/".length());
                        handleUpdateUser(username, body, headers.get("Authorization"), writer, objectMapper);
                    }
                    else {
                        sendResponse(writer, "Not Found", 404);
                    }
                }
            }
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleUserRegistration(String body, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        try {
            Map<String, String> userData = objectMapper.readValue(body, new TypeReference<Map<String, String>>() {});
            String username = userData.get("Username");
            String password = userData.get("Password");

            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                sendResponse(writer, "Bad Request", 400);
                return;
            }
            boolean userCreated = UserDatabase.createUser(username, password);
            if (userCreated) {
                sendResponse(writer, "Created", 201);
            } else {
                sendResponse(writer, "Conflict", 409);
            }
        } catch (JsonProcessingException e) {
            sendResponse(writer, "Bad Request", 400);
        }
    }

    private void handleUserLogin(String body, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        try {
            Map<String, String> credentials = objectMapper.readValue(body, new TypeReference<Map<String, String>>() {});
            String username = credentials.get("Username");
            String password = credentials.get("Password");

            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                sendResponse(writer, "Bad Request", 400);
                return;
            }

            if (UserDatabase.authenticateUser(username, password)) {
                String token = username + "-mctgToken";
                boolean tokenUpdated = UserDatabase.updateToken(username, token);
                if (tokenUpdated) {
                    Map<String, String> response = new HashMap<>();
                    response.put("token", token);
                    String jsonResponse = objectMapper.writeValueAsString(response);
                    sendJsonResponse(writer, jsonResponse, 200);
                } else {
                    sendResponse(writer, "Internal Server Error", 500);
                }
            } else {
                sendResponse(writer, "Unauthorized", 401);
            }
        } catch (JsonProcessingException e) {
            sendResponse(writer, "Bad Request", 400);
        }
    }

    private void handlePackageCreation(String body, String authHeader, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        if (token == null || !isAdmin(token)) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        try {
            List<Card> cards = objectMapper.readValue(body, new TypeReference<List<Card>>() {});
            boolean packageCreated = PackageDatabase.createPackage(cards);
            if (packageCreated) {
                sendResponse(writer, "Package Created", 201);
            } else {
                sendResponse(writer, "Internal Server Error", 500);
            }
        } catch (JsonProcessingException e) {
            sendResponse(writer, "Bad Request", 400);
        }
    }

    private void handlePackagePurchase(String authHeader, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String username = UserDatabase.getUsernameByToken(token);
        if (username == null) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        boolean success = PackageDatabase.purchasePackage(username);
        if (success) {
            sendResponse(writer, "Package Purchased", 200);
        } else {
            sendResponse(writer, "Conflict", 409);
        }
    }

    private void handleGetUserCards(String authHeader, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String username = UserDatabase.getUsernameByToken(token);
        if (username == null) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        List<Card> cards = UserDatabase.getUserCards(username);
        if (cards != null) {
            String jsonResponse = objectMapper.writeValueAsString(cards);
            sendJsonResponse(writer, jsonResponse, 200);
        } else {
            sendResponse(writer, "Internal Server Error", 500);
        }
    }

    private void handleGetDeck(String authHeader, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String username = UserDatabase.getUsernameByToken(token);
        if (username == null) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        List<Card> deck = UserDatabase.getUserDeck(username);
        if (deck != null) {
            String jsonResponse = objectMapper.writeValueAsString(deck);
            sendJsonResponse(writer, jsonResponse, 200);
        } else {
            sendResponse(writer, "Internal Server Error", 500);
        }
    }


    private void handleSetDeck(String body, String authHeader, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String username = UserDatabase.getUsernameByToken(token);
        if (username == null) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        try {
            List<String> cardIds = objectMapper.readValue(body, new TypeReference<List<String>>() {});
            boolean deckSet = UserDatabase.setUserDeck(username, cardIds);
            if (deckSet) {
                sendResponse(writer, "Deck Set", 200);
            } else {
                sendResponse(writer, "Conflict", 409);
            }

        } catch (JsonProcessingException e) {
            sendResponse(writer, "Bad Request", 400);
        }
    }

    private void handleGetUser(String username, BufferedWriter writer, ObjectMapper objectMapper, Map<String, String> headers) throws IOException {
        String authHeader = headers.get("Authorization");
        String token = getTokenFromHeader(authHeader);
        String requester = UserDatabase.getUsernameByToken(token);

        if (requester == null || (!requester.equals(username) && !isAdmin(token))) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        User user = UserDatabase.getUser(username);
        if (user != null) {
            String jsonResponse = objectMapper.writeValueAsString(user);
            sendJsonResponse(writer, jsonResponse, 200);
        } else {
            sendResponse(writer, "Not Found", 404);
        }
    }


    private void handleUpdateUser(String username, String body, String authHeader, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String requester = UserDatabase.getUsernameByToken(token);
        if (requester == null || (!requester.equals(username) && !isAdmin(token))) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        try {
            Map<String, String> userData = objectMapper.readValue(body, new TypeReference<Map<String, String>>() {});
            String newPassword = userData.get("Password"); // Beispiel: Nur Passwort aktualisieren

            if (newPassword == null || newPassword.isEmpty()) {
                sendResponse(writer, "Bad Request", 400);
                return;
            }
            boolean updated = UserDatabase.updateUserPassword(username, newPassword);
            if (updated) {
                sendResponse(writer, "OK", 200);
            } else {
                sendResponse(writer, "Not Found", 404);
            }
        } catch (JsonProcessingException e) {
            sendResponse(writer, "Bad Request", 400);
        }
    }
    private String getTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    private boolean isAdmin(String token) {
        return "admin-mctgToken".equals(token);
    }

    private void sendResponse(BufferedWriter writer, String body, int statusCode) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + getReasonPhrase(statusCode) + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.getBytes("UTF-8").length + "\r\n" +
                "\r\n" +
                body;
        writer.write(response);
        writer.flush();
    }

    private void sendJsonResponse(BufferedWriter writer, String jsonBody, int statusCode) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + getReasonPhrase(statusCode) + "\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + jsonBody.getBytes("UTF-8").length + "\r\n" +
                "\r\n" +
                jsonBody;
        writer.write(response);
        writer.flush();
    }

    private String getReasonPhrase(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 500 -> "Internal Server Error";
            default -> "";
        };
    }
}
