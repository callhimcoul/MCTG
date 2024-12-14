package at.technikum_wien;

import at.technikum_wien.cards.Card;

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
    private String boosterCardId; // The chosen booster card
    private boolean boosterUsed;  // Tracks if booster was used this battle

    public Player(String username, List<Card> deck, BufferedWriter writer, Object battleLock) {
        this.username = username;
        this.deck = new ArrayList<>(deck);
        this.writer = writer;
        this.battleLock = battleLock;
        this.boosterCardId = null;
        this.boosterUsed = false;
    }

    // New constructor with booster card
    public Player(String username, List<Card> deck, BufferedWriter writer, Object battleLock, String boosterCardId) {
        this.username = username;
        this.deck = new ArrayList<>(deck);
        this.writer = writer;
        this.battleLock = battleLock;
        this.boosterCardId = boosterCardId;
        this.boosterUsed = false;
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
    }

    private void sendResponse(BufferedWriter writer, String body, int statusCode) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.getBytes("UTF-8").length + "\r\n" +
                "\r\n" +
                body;
        writer.write(response);
        writer.flush();
    }

    public Object getBattleLock() {
        return battleLock;
    }

    // Unique feature logic: Check if this card is the booster card and if booster is not used yet
    public boolean isBoosterCard(Card card) {
        if (boosterCardId == null) return false;
        return card.getId().equals(boosterCardId) && !boosterUsed;
    }

    public void markBoosterUsed() {
        this.boosterUsed = true;
    }
}
