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

            // Read and parse HTTP headers
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                System.out.println("Header: " + headerLine);
                String[] headerParts = headerLine.split(": ", 2);
                if (headerParts.length == 2) {
                    headers.put(headerParts[0], headerParts[1]);
                }
            }

            // Read body if Content-Length is set
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

            // Handle the request line
            if (requestLine != null && !requestLine.isEmpty()) {
                String[] requestParts = requestLine.split(" ");
                if (requestParts.length < 3) {
                    sendResponse(writer, "Bad Request", 400);
                } else {
                    String method = requestParts[0];
                    String path = requestParts[1];

                    // ------------------- Routing Logic ------------------- //
                    if (method.equals("GET") && path.equals("/")) {
                        sendResponse(writer, "Hello, World!", 200);

                    } else if (method.equals("POST") && path.equals("/users")) {
                        // Register user
                        handleUserRegistration(body, writer, objectMapper);

                    } else if (method.equals("POST") && path.equals("/sessions")) {
                        // Login user
                        handleUserLogin(body, writer, objectMapper);

                    } else if (method.equals("POST") && path.equals("/packages")) {
                        // Create package (admin only)
                        handlePackageCreation(body, headers.get("Authorization"), writer, objectMapper);

                    } else if (method.equals("POST") && path.equals("/transactions/packages")) {
                        // Acquire (buy) package
                        handlePackagePurchase(headers.get("Authorization"), writer, objectMapper);

                    } else if (method.equals("GET") && path.equals("/cards")) {
                        // Show all user cards
                        handleGetUserCards(headers.get("Authorization"), writer, objectMapper);

                    } else if (method.equals("GET") && path.equals("/deck")) {
                        // Show user deck
                        handleGetDeck(headers.get("Authorization"), writer, objectMapper);

                    } else if (method.equals("PUT") && path.equals("/deck")) {
                        // Configure deck (PUT new card IDs)
                        handleSetDeck(body, headers.get("Authorization"), writer, objectMapper);

                    } else if (method.equals("GET") && path.matches("/users/[^/]+")) {
                        // Get user profile by username
                        String username = path.substring("/users/".length());
                        handleGetUser(username, writer, objectMapper, headers);

                    } else if (method.equals("PUT") && path.matches("/users/[^/]+$")) {
                        // Update user data (bio, image)
                        String username = path.substring("/users/".length());
                        handleUpdateUser(username, body, headers.get("Authorization"), writer, objectMapper);

                    } else if (method.equals("PUT") && path.matches("/users/[^/]+/booster")) {
                        // Set booster card
                        String userPart = path.substring("/users/".length());
                        String username = userPart.substring(0, userPart.indexOf("/booster"));
                        handleSetBoosterCard(username, body, headers.get("Authorization"), writer, objectMapper);

                    } else if (method.equals("POST") && path.equals("/battles")) {
                        // Start battle
                        keepConnectionOpen = true;
                        handleBattleRequest(headers.get("Authorization"), writer, objectMapper);

                    } else if (method.equals("GET") && path.equals("/scoreboard")) {
                        // Show scoreboard
                        handleGetScoreboard(headers.get("Authorization"), writer, objectMapper);

                    } else if (method.equals("GET") && path.equals("/stats")) {
                        // Show stats for current user
                        handleGetUserStats(headers.get("Authorization"), writer, objectMapper);

                    } else if (method.equals("POST") && path.equals("/tradings")) {
                        // Create new trading deal
                        handleCreateTradingDeal(body, headers.get("Authorization"), writer, objectMapper);

                    } else if (method.equals("GET") && path.equals("/tradings")) {
                        // Retrieve all trading deals
                        handleGetTradingDeals(headers.get("Authorization"), writer, objectMapper);

                    } else if (method.equals("POST") && path.matches("/tradings/[^/]+")) {
                        // Accept a specific trading deal
                        String dealId = path.substring("/tradings/".length());
                        handleAcceptTradingDeal(dealId, body, headers.get("Authorization"), writer, objectMapper);

                    }
                    // ------------ NEW: DELETE user ------------
                    else if (method.equals("DELETE") && path.matches("/users/[^/]+$")) {
                        String username = path.substring("/users/".length());
                        handleDeleteUser(username, headers.get("Authorization"), writer, objectMapper);

                    }
                    // ---------- NEW: Update user coins ----------
                    else if (method.equals("PUT") && path.matches("/users/[^/]+/coins")) {
                        // Optional route to set user coin balance
                        String userPart = path.substring("/users/".length());
                        String username = userPart.substring(0, userPart.indexOf("/coins"));
                        handleUpdateUserCoins(username, body, headers.get("Authorization"), writer, objectMapper);

                    } else {
                        // Route not found
                        sendResponse(writer, "Not Found", 404);
                    }
                }
            }

            if (!keepConnectionOpen) {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // handleUserRegistration, handleUserLogin, handlePackageCreation,
    // handlePackagePurchase, handleGetUserCards, handleGetDeck, handleSetDeck,
    // handleGetUser, handleUpdateUser, handleSetBoosterCard, handleBattleRequest,
    // handleGetScoreboard, handleGetUserStats, handleCreateTradingDeal,
    // handleGetTradingDeals, handleAcceptTradingDeal, etc.

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

    private void handlePackageCreation(String body, String authHeader,
                                       BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        if (token == null || !isAdmin(token)) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

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

    private void handlePackagePurchase(String authHeader, BufferedWriter writer,
                                       ObjectMapper objectMapper) throws IOException {
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

    private void handleGetUserCards(String authHeader, BufferedWriter writer,
                                    ObjectMapper objectMapper) throws IOException {
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

    private void handleGetDeck(String authHeader, BufferedWriter writer,
                               ObjectMapper objectMapper) throws IOException {
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

    private void handleSetDeck(String body, String authHeader, BufferedWriter writer,
                               ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String username = UserDatabase.getUsernameByToken(token);
        if (username == null) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

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

    private void handleGetUser(String username, BufferedWriter writer, ObjectMapper objectMapper,
                               Map<String, String> headers) throws IOException {
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

    private void handleUpdateUser(String username, String body, String authHeader,
                                  BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String requester = UserDatabase.getUsernameByToken(token);
        if (requester == null || (!requester.equals(username) && !isAdmin(token))) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

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

    private void handleSetBoosterCard(String username, String body, String authHeader,
                                      BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String requester = UserDatabase.getUsernameByToken(token);

        if (requester == null || (!requester.equals(username) && !isAdmin(token))) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        try {
            Map<String, String> requestData = objectMapper.readValue(body, new TypeReference<>() {});
            String boosterCardId = requestData.get("CardId");

            // Check if user owns this card
            if (!UserDatabase.isCardOwnedByUser(username, boosterCardId)) {
                sendResponse(writer, "Forbidden: You don't own this card.", 403);
                return;
            }

            boolean updated = UserDatabase.updateUserBoosterCard(username, boosterCardId);
            if (updated) {
                sendResponse(writer, "Booster card set successfully", 200);
            } else {
                sendResponse(writer, "Not Found", 404);
            }
        } catch (JsonProcessingException e) {
            sendResponse(writer, "Bad Request", 400);
        }
    }

    private final Object battleLock = new Object();

    private void handleBattleRequest(String authHeader, BufferedWriter writer,
                                     ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String username = UserDatabase.getUsernameByToken(token);
        if (username == null) {
            sendResponse(writer, "Unauthorized", 401);
            writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            return;
        }

        List<Card> deck = UserDatabase.getUserDeck(username);
        if (deck == null || deck.size() != 4) {
            sendResponse(writer, "Deck not configured properly. Ensure you have exactly 4 cards in your deck.", 400);
            writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            return;
        }

        User user = UserDatabase.getUser(username);
        String boosterCardId = (user != null) ? user.getBoosterCardId() : null;

        Player player = new Player(username, deck, writer, battleLock, boosterCardId);
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

    private void handleGetScoreboard(String authHeader, BufferedWriter writer,
                                     ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String username = UserDatabase.getUsernameByToken(token);
        if (username == null) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        List<User> users = UserDatabase.getAllUsersSortedByElo();
        String jsonResponse = objectMapper.writeValueAsString(users);
        sendJsonResponse(writer, jsonResponse, 200);
    }

    private void handleGetUserStats(String authHeader, BufferedWriter writer,
                                    ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String username = UserDatabase.getUsernameByToken(token);
        if (username == null) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        User user = UserDatabase.getUser(username);
        if (user != null) {
            String jsonResponse = objectMapper.writeValueAsString(user);
            sendJsonResponse(writer, jsonResponse, 200);
        } else {
            sendResponse(writer, "User not found", 404);
        }
    }

    private void handleCreateTradingDeal(String body, String authHeader, BufferedWriter writer,
                                         ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String username = UserDatabase.getUsernameByToken(token);
        if (username == null) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        try {
            TradingDeal deal = objectMapper.readValue(body, TradingDeal.class);
            deal.setOwner(username);

            if (!UserDatabase.isCardOwnedByUser(username, deal.getCardToTrade())
                    || UserDatabase.isCardInDeck(username, deal.getCardToTrade())) {
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

    private void handleGetTradingDeals(String authHeader, BufferedWriter writer,
                                       ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String username = UserDatabase.getUsernameByToken(token);
        if (username == null) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        List<TradingDeal> deals = TradingDatabase.getAllTradingDeals();
        String jsonResponse = objectMapper.writeValueAsString(deals);
        sendJsonResponse(writer, jsonResponse, 200);
    }

    private void handleAcceptTradingDeal(String dealId, String body, String authHeader,
                                         BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String buyer = UserDatabase.getUsernameByToken(token);
        if (buyer == null) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        try {
            Map<String, String> requestData = objectMapper.readValue(body, new TypeReference<>() {});
            String offeredCardId = requestData.get("cardId");

            if (!UserDatabase.isCardOwnedByUser(buyer, offeredCardId)
                    || UserDatabase.isCardInDeck(buyer, offeredCardId)) {
                sendResponse(writer, "Forbidden: You don't own this card or it's in your deck.", 403);
                return;
            }

            boolean success = TradingDatabase.acceptTradingDeal(dealId, buyer, offeredCardId);
            if (success) {
                sendResponse(writer, "Trading deal accepted successfully.", 200);
            } else {
                sendResponse(writer, "Not Found or Conflict: Could not accept trading deal.", 409);
            }
        } catch (JsonProcessingException e) {
            sendResponse(writer, "Bad Request", 400);
        }
    }


    /**
     * Delete a user (only by themselves or an admin).
     */
    private void handleDeleteUser(String username, String authHeader,
                                  BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String requestor = UserDatabase.getUsernameByToken(token);

        if (requestor == null) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        // Allow only the same user or an admin to delete the account
        if (!requestor.equals(username) && !isAdmin(token)) {
            sendResponse(writer, "Forbidden", 403);
            return;
        }

        boolean deleted = UserDatabase.deleteUser(username);
        if (deleted) {
            sendResponse(writer, "User deleted successfully.", 200);
        } else {
            sendResponse(writer, "User not found or deletion failed.", 404);
        }
    }

    /**
     * Update user coins (optional).
     */
    private void handleUpdateUserCoins(String username, String body, String authHeader,
                                       BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String requestor = UserDatabase.getUsernameByToken(token);

        if (requestor == null) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        // Allow if user is themselves OR an admin
        if (!requestor.equals(username) && !isAdmin(token)) {
            sendResponse(writer, "Forbidden", 403);
            return;
        }

        try {
            // Expecting JSON: {"coins": 999}
            Map<String, Integer> coinData = objectMapper.readValue(body, new TypeReference<>() {});
            Integer newCoinValue = coinData.get("coins");
            if (newCoinValue == null) {
                sendResponse(writer, "Bad Request", 400);
                return;
            }

            boolean updated = UserDatabase.updateUserCoins(username, newCoinValue);
            if (updated) {
                sendResponse(writer, "Coins updated successfully.", 200);
            } else {
                sendResponse(writer, "User not found or update failed.", 404);
            }

        } catch (JsonProcessingException e) {
            sendResponse(writer, "Bad Request", 400);
        }
    }

    // ------------------ Utility Methods ------------------ //

    private String getTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    private boolean isAdmin(String token) {
        return "admin-mtcgToken".equals(token);
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
