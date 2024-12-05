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

            boolean keepConnectionOpen = false; // Flag, um zu bestimmen, ob die Verbindung offen bleiben soll

            if (requestLine != null && !requestLine.isEmpty()) {
                String[] requestParts = requestLine.split(" ");
                if (requestParts.length < 3) {
                    sendResponse(writer, "Bad Request", 400);
                } else {
                    String method = requestParts[0];
                    String path = requestParts[1];
                    String httpVersion = requestParts[2];

                    // Routing Logik
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
                    // Neue Benutzerverwaltungs-Endpunkte
                    else if (method.equals("GET") && path.matches("/users/[^/]+")) {
                        String username = path.substring("/users/".length());
                        handleGetUser(username, writer, objectMapper, headers);
                    } else if (method.equals("PUT") && path.matches("/users/[^/]+")) {
                        String username = path.substring("/users/".length());
                        handleUpdateUser(username, body, headers.get("Authorization"), writer, objectMapper);
                    }
                    // Battle Endpoint
                    else if (method.equals("POST") && path.equals("/battles")) {
                        keepConnectionOpen = true; // Verbindung offen halten
                        handleBattleRequest(headers.get("Authorization"), writer, objectMapper);
                    } else if (method.equals("GET") && path.equals("/scoreboard")) {
                        handleGetScoreboard(headers.get("Authorization"), writer, objectMapper);
                    } else if (method.equals("GET") && path.equals("/stats")) {
                        handleGetUserStats(headers.get("Authorization"), writer, objectMapper);
                    }
                    // Trading Deals
                    else if (method.equals("POST") && path.equals("/tradings")) {
                        handleCreateTradingDeal(body, headers.get("Authorization"), writer, objectMapper);
                    } else if (method.equals("GET") && path.equals("/tradings")) {
                        handleGetTradingDeals(headers.get("Authorization"), writer, objectMapper);
                    } else if (method.equals("POST") && path.matches("/tradings/[^/]+")) {
                        String dealId = path.substring("/tradings/".length());
                        handleAcceptTradingDeal(dealId, body, headers.get("Authorization"), writer, objectMapper);
                    } else {
                        sendResponse(writer, "Not Found", 404);
                    }
                }
            }

            if (!keepConnectionOpen) {
                // Ressourcen schließen
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            }
            // Wenn die Verbindung offen gehalten wird (z.B. bei Battles), werden die Ressourcen später geschlossen

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /**
     * Behandelt die Benutzerregistrierung (POST /users).
     */
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

    /**
     * Behandelt die Benutzeranmeldung (POST /sessions).
     */
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
                String token = username + "-mtcgToken";
                // Token in der Datenbank speichern
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

    /**
     * Behandelt die Paket-Erstellung (POST /packages).
     */
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

    /**
     * Behandelt den Paket-Kauf (POST /transactions/packages).
     */
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

    /**
     * Behandelt das Abrufen der Benutzerkarten (GET /cards).
     */
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

    /**
     * Behandelt das Abrufen des Benutzerdecks (GET /deck).
     */
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

    /**
     * Behandelt das Setzen des Benutzerdecks (PUT /deck).
     */
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

    /**
     * Behandelt das Abrufen von Benutzerdaten (GET /users/{username}).
     */
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

    /**
     * Behandelt das Aktualisieren von Benutzerdaten (PUT /users/{username}).
     */
    private void handleUpdateUser(String username, String body, String authHeader, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String requester = UserDatabase.getUsernameByToken(token);
        if (requester == null || (!requester.equals(username) && !isAdmin(token))) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        try {
            Map<String, String> userData = objectMapper.readValue(body, new TypeReference<Map<String, String>>() {});
            String newBio = userData.get("Bio");
            String newImage = userData.get("Image");

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

    private void handleBattleRequest(String authHeader, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
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

        Player player = new Player(username, deck, writer, battleLock);

        // Spieler zur Warteschlange hinzufügen
        BattleHandler.enqueuePlayer(player);

        // Warten, bis das Battle abgeschlossen ist
        synchronized (battleLock) {
            try {
                battleLock.wait(); // Thread wartet hier, bis er benachrichtigt wird
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Nach dem Battle
        if (writer != null) writer.close();
        if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
    }



    private void handleGetUserStats(String authHeader, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
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


    private void handleCreateTradingDeal(String body, String authHeader, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String username = UserDatabase.getUsernameByToken(token);
        if (username == null) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        try {
            // Handelsangebot aus dem Body lesen
            TradingDeal deal = objectMapper.readValue(body, TradingDeal.class);
            deal.setOwner(username);

            // Überprüfen, ob die Karte dem Benutzer gehört und nicht im Deck ist
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

    private void handleGetTradingDeals(String authHeader, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
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

    private void handleAcceptTradingDeal(String dealId, String body, String authHeader, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
        String token = getTokenFromHeader(authHeader);
        String buyer = UserDatabase.getUsernameByToken(token);
        if (buyer == null) {
            sendResponse(writer, "Unauthorized", 401);
            return;
        }

        try {
            Map<String, String> requestData = objectMapper.readValue(body, new TypeReference<Map<String, String>>() {});
            String offeredCardId = requestData.get("cardId");

            // Überprüfen, ob die angebotene Karte dem Benutzer gehört und nicht im Deck ist
            if (!UserDatabase.isCardOwnedByUser(buyer, offeredCardId) || UserDatabase.isCardInDeck(buyer, offeredCardId)) {
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

    private void handleGetScoreboard(String authHeader, BufferedWriter writer, ObjectMapper objectMapper) throws IOException {
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






    /**
     * Extrahiert das Token aus dem Authorization-Header.
     *
     * @param authHeader Der Wert des Authorization-Headers
     * @return Das extrahierte Token oder null, wenn kein Token vorhanden ist
     */
    private String getTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    /**
     * Überprüft, ob das Token einem Admin entspricht.
     *
     * @param token Das Authentifizierungstoken
     * @return true, wenn das Token einem Admin gehört, sonst false
     */
    private boolean isAdmin(String token) {
        // Beispielhafte Implementierung: Ein Admin-Token ist "admin-mtcgToken"
        return "admin-mtcgToken".equals(token);
    }

    /**
     * Sendet eine einfache Textantwort.
     *
     * @param writer     BufferedWriter zum Senden der Antwort
     * @param body       Der Antworttext
     * @param statusCode Der HTTP-Statuscode
     * @throws IOException Wenn ein Fehler beim Schreiben auftritt
     */
    private void sendResponse(BufferedWriter writer, String body, int statusCode) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + getReasonPhrase(statusCode) + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.getBytes("UTF-8").length + "\r\n" +
                "\r\n" +
                body;
        writer.write(response);
        writer.flush();
    }

    /**
     * Sendet eine JSON-Antwort.
     *
     * @param writer     BufferedWriter zum Senden der Antwort
     * @param jsonBody   Der JSON-formattierte Antworttext
     * @param statusCode Der HTTP-Statuscode
     * @throws IOException Wenn ein Fehler beim Schreiben auftritt
     */
    private void sendJsonResponse(BufferedWriter writer, String jsonBody, int statusCode) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + getReasonPhrase(statusCode) + "\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + jsonBody.getBytes("UTF-8").length + "\r\n" +
                "\r\n" +
                jsonBody;
        writer.write(response);
        writer.flush();
    }

    /**
     * Gibt die Begründung für den HTTP-Statuscode zurück.
     *
     * @param statusCode Der HTTP-Statuscode
     * @return Die Begründung als String
     */
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
