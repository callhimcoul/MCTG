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
        InputStream input = null;
        OutputStream output = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            input = clientSocket.getInputStream();
            output = clientSocket.getOutputStream();
            reader = new BufferedReader(new InputStreamReader(input));
            writer = new BufferedWriter(new OutputStreamWriter(output));

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

            boolean keepConnectionOpen = false;

            if (requestLine != null && !requestLine.isEmpty()) {
                String[] requestParts = requestLine.split(" ");
                if (requestParts.length < 3) {
                    sendResponse(writer, "Bad Request", 400);
                } else {
                    String method = requestParts[0];
                    String path = requestParts[1];

                    // Public endpoints (no token required)
                    if (method.equals("GET") && path.equals("/")) {
                        sendResponse(writer, "Hello, World!", 200);
                    } else if (method.equals("POST") && path.equals("/users")) {
                        // Registration is public
                        handleUserRegistration(body, writer, objectMapper);
                    } else if (method.equals("POST") && path.equals("/sessions")) {
                        // Login is public
                        handleUserLogin(body, writer, objectMapper);

                        // Admin-only endpoint
                    } else if (method.equals("POST") && path.equals("/packages")) {
                        // requires admin
                        String token = getTokenFromHeader(headers.get("Authorization"));
                        if (!requireAdmin(token, writer)) {
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        handlePackageCreation(body, token, writer, objectMapper);

                        // Authenticated endpoints
                    } else if (method.equals("POST") && path.equals("/transactions/packages")) {
                        String username = requireUser(headers.get("Authorization"), writer);
                        if (username == null) {
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        handlePackagePurchase(username, writer, objectMapper);

                    } else if (method.equals("GET") && path.equals("/cards")) {
                        String username = requireUser(headers.get("Authorization"), writer);
                        if (username == null) {
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        handleGetUserCards(username, writer, objectMapper);

                    } else if (method.equals("GET") && path.equals("/deck")) {
                        String username = requireUser(headers.get("Authorization"), writer);
                        if (username == null) {
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        handleGetDeck(username, writer, objectMapper);

                    } else if (method.equals("PUT") && path.equals("/deck")) {
                        String username = requireUser(headers.get("Authorization"), writer);
                        if (username == null) {
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        handleSetDeck(body, username, writer, objectMapper);

                    } else if (method.equals("GET") && path.matches("/users/[^/]+")) {
                        String token = getTokenFromHeader(headers.get("Authorization"));
                        String requester = UserDatabase.getUsernameByToken(token);
                        if (requester == null) {
                            sendResponse(writer, "Unauthorized", 401);
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        String username = path.substring("/users/".length());
                        if (!requireSameUserOrAdmin(requester, token, username, writer)) {
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        handleGetUser(username, writer, objectMapper);

                    } else if (method.equals("PUT") && path.matches("/users/[^/]+")) {
                        String token = getTokenFromHeader(headers.get("Authorization"));
                        String requester = UserDatabase.getUsernameByToken(token);
                        if (requester == null) {
                            sendResponse(writer, "Unauthorized", 401);
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        String username = path.substring("/users/".length());
                        if (!requireSameUserOrAdmin(requester, token, username, writer)) {
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        handleUpdateUser(username, body, writer, objectMapper);

                    } else if (method.equals("POST") && path.equals("/battles")) {
                        String username = requireUser(headers.get("Authorization"), writer);
                        if (username == null) {
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        keepConnectionOpen = true;
                        handleBattleRequest(username, writer, objectMapper);

                    } else if (method.equals("GET") && path.equals("/scoreboard")) {
                        String username = requireUser(headers.get("Authorization"), writer);
                        if (username == null) {
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        handleGetScoreboard(writer, objectMapper);

                    } else if (method.equals("GET") && path.equals("/stats")) {
                        String username = requireUser(headers.get("Authorization"), writer);
                        if (username == null) {
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        handleGetUserStats(username, writer, objectMapper);

                    } else if (method.equals("POST") && path.equals("/tradings")) {
                        String username = requireUser(headers.get("Authorization"), writer);
                        if (username == null) {
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        handleCreateTradingDeal(body, username, writer, objectMapper);

                    } else if (method.equals("GET") && path.equals("/tradings")) {
                        String username = requireUser(headers.get("Authorization"), writer);
                        if (username == null) {
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        handleGetTradingDeals(writer, objectMapper);

                    } else if (method.equals("POST") && path.matches("/tradings/[^/]+")) {
                        String buyer = requireUser(headers.get("Authorization"), writer);
                        if (buyer == null) {
                            closeResources(reader, writer, clientSocket);
                            return;
                        }
                        String dealId = path.substring("/tradings/".length());
                        handleAcceptTradingDeal(dealId, body, buyer, writer, objectMapper);

                    } else {
                        sendResponse(writer, "Not Found", 404);
                    }
                }
            }

            if (!keepConnectionOpen) {
                closeResources(reader, writer, clientSocket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Security and Helper Methods ---

    private void closeResources(BufferedReader reader, BufferedWriter writer, Socket socket) throws IOException {
        if (reader != null) reader.close();
        if (writer != null) writer.close();
        if (socket != null && !socket.isClosed()) socket.close();
    }

    private String requireUser(String authHeader, BufferedWriter writer) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String username = UserDatabase.getUsernameByToken(token);
        if (username == null) {
            sendResponse(writer, "Unauthorized", 401);
            return null;
        }
        return username;
    }

    private boolean requireAdmin(String token, BufferedWriter writer) throws IOException {
        if (token == null || !isAdmin(token)) {
            sendResponse(writer, "Unauthorized (Admin required)", 401);
            return false;
        }
        return true;
    }

    private boolean requireSameUserOrAdmin(String requester, String token, String targetUser, BufferedWriter writer) throws IOException {
        if (!requester.equals(targetUser) && !isAdmin(token)) {
            sendResponse(writer, "Unauthorized", 401);
            return false;
        }
        return true;
    }

    private String getTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    private boolean isAdmin(String token) {
        return "admin-mtcgToken".equals(token);
    }

    // --- Handlers below (adjusted to use the new security methods) ---

    private void handleUserRegistration(String body, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        try {
            Map<String, String> userData = objectMapper.readValue(body, new TypeReference<>() {});
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
            Map<String, String> credentials = objectMapper.readValue(body, new TypeReference<>() {});
            String username = credentials.get("Username");
            String password = credentials.get("Password");

            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                sendResponse(writer, "Bad Request", 400);
                return;
            }

            if (UserDatabase.authenticateUser(username, password)) {
                String token = username + "-mtcgToken";
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

    private void handlePackageCreation(String body, String token, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        try {
            List<Card> cards = objectMapper.readValue(body, new TypeReference<>() {});
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

    private void handlePackagePurchase(String username, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        boolean success = PackageDatabase.purchasePackage(username);
        if (success) {
            sendResponse(writer, "Package Purchased", 200);
        } else {
            sendResponse(writer, "Conflict", 409);
        }
    }

    private void handleGetUserCards(String username, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        List<Card> cards = UserDatabase.getUserCards(username);
        if (cards != null) {
            String jsonResponse = objectMapper.writeValueAsString(cards);
            sendJsonResponse(writer, jsonResponse, 200);
        } else {
            sendResponse(writer, "Internal Server Error", 500);
        }
    }

    private void handleGetDeck(String username, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        List<Card> deck = UserDatabase.getUserDeck(username);
        if (deck != null) {
            String jsonResponse = objectMapper.writeValueAsString(deck);
            sendJsonResponse(writer, jsonResponse, 200);
        } else {
            sendResponse(writer, "Internal Server Error", 500);
        }
    }

    private void handleSetDeck(String body, String username, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        try {
            List<String> cardIds = objectMapper.readValue(body, new TypeReference<>() {});
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

    private void handleGetUser(String username, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        User user = UserDatabase.getUser(username);
        if (user != null) {
            String jsonResponse = objectMapper.writeValueAsString(user);
            sendJsonResponse(writer, jsonResponse, 200);
        } else {
            sendResponse(writer, "Not Found", 404);
        }
    }

    private void handleUpdateUser(String username, String body, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        try {
            Map<String, String> userData = objectMapper.readValue(body, new TypeReference<>() {});
            String newBio = userData.getOrDefault("Bio", "");
            String newImage = userData.getOrDefault("Image", "");

            boolean updated = UserDatabase.updateUserProfile(username, newBio, newImage);
            if (updated) {
                sendResponse(writer, "Profile updated successfully", 200);
            } else {
                sendResponse(writer, "Not Found", 404);
            }

        } catch (JsonProcessingException e) {
            sendResponse(writer, "Bad Request", 400);
        }
    }

    private final Object battleLock = new Object();

    private void handleBattleRequest(String username, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        List<Card> deck = UserDatabase.getUserDeck(username);
        if (deck == null || deck.size() != 4) {
            sendResponse(writer, "Deck not configured properly. Ensure you have exactly 4 cards in your deck.", 400);
            writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            return;
        }

        Player player = new Player(username, deck, writer, battleLock);
        BattleHandler.enqueuePlayer(player);

        synchronized (battleLock) {
            try {
                battleLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (writer != null) writer.close();
        if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
    }

    private void handleGetUserStats(String username, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        User user = UserDatabase.getUser(username);
        if (user != null) {
            String jsonResponse = objectMapper.writeValueAsString(user);
            sendJsonResponse(writer, jsonResponse, 200);
        } else {
            sendResponse(writer, "User not found", 404);
        }
    }

    private void handleCreateTradingDeal(String body, String username, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        try {
            TradingDeal deal = objectMapper.readValue(body, TradingDeal.class);
            deal.setOwner(username);

            if (!UserDatabase.isCardOwnedByUser(username, deal.getCardToTrade()) || UserDatabase.isCardInDeck(username, deal.getCardToTrade())) {
                sendResponse(writer, "Forbidden: You don't own this card or it's in your deck.", 403);
                return;
            }

            boolean created = TradingDatabase.createTradingDeal(deal);
            if (created) {
                sendResponse(writer, "Trading deal created successfully.", 201);
            } else {
                sendResponse(writer, "Conflict: Could not create trading deal.", 409);
            }

        } catch (JsonProcessingException e) {
            sendResponse(writer, "Bad Request", 400);
        }
    }

    private void handleGetTradingDeals(BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        List<TradingDeal> deals = TradingDatabase.getAllTradingDeals();
        String jsonResponse = objectMapper.writeValueAsString(deals);
        sendJsonResponse(writer, jsonResponse, 200);
    }

    private void handleAcceptTradingDeal(String dealId, String body, String buyer, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        try {
            Map<String, String> requestData = objectMapper.readValue(body, new TypeReference<>() {});
            String offeredCardId = requestData.get("cardId");

            if (!UserDatabase.isCardOwnedByUser(buyer, offeredCardId) || UserDatabase.isCardInDeck(buyer, offeredCardId)) {
                sendResponse(writer, "Forbidden: You don't own this card or it's in your deck.", 403);
                return;
            }

            boolean success = TradingDatabase.acceptTradingDeal(dealId, buyer, offeredCardId);
            if (success) {
                sendResponse(writer, "Trading deal accepted successfully", 200);
            } else {
                sendResponse(writer, "Not Found or Conflict: Could not accept trading deal.", 409);
            }

        } catch (JsonProcessingException e) {
            sendResponse(writer, "Bad Request", 400);
        }
    }

    private void handleGetScoreboard(BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        List<User> users = UserDatabase.getAllUsersSortedByElo();
        String jsonResponse = objectMapper.writeValueAsString(users);
        sendJsonResponse(writer, jsonResponse, 200);
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
