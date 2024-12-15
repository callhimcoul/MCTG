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
    private String boosterCardId;
    private boolean boosterUsed;

    public Player(String username, List<Card> deck, BufferedWriter writer, Object battleLock) {
        this.username = username;
        this.deck = new ArrayList<>(deck);
        this.writer = writer;
        this.battleLock = battleLock;
        this.boosterCardId = null;
        this.boosterUsed = false;
    }

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

    /**
     * Adds a card to the player's deck and DB, but only if the player does not already own this card.
     */
    public void addCard(Card card) {
        if (!UserDatabase.isCardOwnedByUser(username, card.getId())) {
            UserDatabase.addCardToUser(username, card.getId());
        }
        // Ensure the deck list contains the card
        if (!deckContainsCard(card.getId())) {
            deck.add(card);
        }
    }

    /**
     * Removes a card from the player's deck and from the DB.
     */
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

    public boolean isBoosterCard(Card card) {
        if (boosterCardId == null) return false;
        return card.getId().equals(boosterCardId) && !boosterUsed;
    }

    public void markBoosterUsed() {
        this.boosterUsed = true;
    }

    private boolean deckContainsCard(String cardId) {
        for (Card c : deck) {
            if (c.getId().equals(cardId)) return true;
        }
        return false;
    }
}
