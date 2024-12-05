package at.technikum_wien;

import at.technikum_wien.cards.Card;
import at.technikum_wien.cards.MonsterCard;
import at.technikum_wien.cards.SpellCard;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Player {
    private String username;
    private List<Card> deck;
    private BufferedWriter writer;
    private Object battleLock;

    public Player(String username, List<Card> deck, BufferedWriter writer, Object battleLock) {
        this.username = username;
        this.deck = new ArrayList<>(deck);
        this.writer = writer;
        this.battleLock = battleLock;
    }

    public String getUsername() {
        return username;
    }

    public boolean hasCards() {
        return !deck.isEmpty();
    }

    public Card drawRandomCard() {
        Random rand = new Random();
        int index = rand.nextInt(deck.size());
        return deck.get(index);
    }

    public void addCard(Card card) {
        deck.add(card);
        UserDatabase.addCardToUser(username, card.getId());
    }

    public void removeCard(Card card) {
        deck.remove(card);
        UserDatabase.removeCardFromUser(username, card.getId());
    }

    public void sendBattleResult(String battleLog) throws IOException {
        sendResponse(writer, battleLog, 200);

        // Nach dem Senden des Ergebnisses den Thread benachrichtigen
        synchronized (battleLock) {
            battleLock.notify();
        }
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
