package at.technikum_wien;

import at.technikum_wien.cards.Card;
import at.technikum_wien.cards.MonsterCard;
import at.technikum_wien.cards.SpellCard;
import at.technikum_wien.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;




public class UserDatabase {

    /**
     * Erstellt einen neuen Benutzer in der Datenbank.
     *
     * @param username Benutzername
     * @param password Passwort (im Klartext, sollte gehasht gespeichert werden)
     * @return true, wenn der Benutzer erfolgreich erstellt wurde, sonst false
     */
    public static boolean createUser(String username, String password) {
        String insertUser = "INSERT INTO users (username, password, coins, elo) VALUES (?, ?, 20, 100)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertUser)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // Passwort im Klartext speichern (nicht empfohlen für Produktion)
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }





    public static String getCurrentDatabase(Connection conn) {
        String query = "SELECT current_database()";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Authentifiziert einen Benutzer anhand von Benutzername und Passwort.
     *
     * @param username Benutzername
     * @param password Passwort (im Klartext)
     * @return true, wenn die Anmeldedaten korrekt sind, sonst false
     */
    public static boolean authenticateUser(String username, String password) {
        String query = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                // Vergleiche das eingegebene Passwort mit dem gespeicherten Passwort
                return password.equals(storedPassword); // In der Praxis sollte ein Hash-Vergleich erfolgen
            } else {
                return false; // Benutzer existiert nicht
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Ruft die Karten eines Benutzers ab.
     *
     * @param username Benutzername
     * @return Liste der Karten des Benutzers oder null bei Fehler
     */
    public static List<Card> getUserCards(String username) {
        String query = "SELECT c.id, c.name, c.damage, c.type, c.element FROM user_cards uc " +
                "JOIN cards c ON uc.card_id = c.id WHERE uc.username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            List<Card> cards = new ArrayList<>();
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                double damage = rs.getDouble("damage");
                String type = rs.getString("type");
                String element = rs.getString("element");

                Card card;
                if ("Monster".equalsIgnoreCase(type)) {
                    card = new MonsterCard(id, name, damage, element);
                } else {
                    card = new SpellCard(id, name, damage, element);
                }
                cards.add(card);
            }
            return cards;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Ruft das Deck eines Benutzers ab.
     *
     * @param username Benutzername
     * @return Liste der Karten im Deck des Benutzers oder null bei Fehler
     */
    public static List<Card> getUserDeck(String username) {
        String query = "SELECT c.id, c.name, c.damage, c.type, c.element FROM decks d " +
                "JOIN cards c ON d.card_id = c.id WHERE d.username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            List<Card> deck = new ArrayList<>();
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                double damage = rs.getDouble("damage");
                String type = rs.getString("type");
                String element = rs.getString("element");

                Card card;
                if ("Monster".equalsIgnoreCase(type)) {
                    card = new MonsterCard(id, name, damage, element);
                } else {
                    card = new SpellCard(id, name, damage, element);
                }
                deck.add(card);
            }
            return deck;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Setzt das Deck eines Benutzers auf die angegebenen Karten.
     *
     * @param username Benutzername
     * @param cardIds  Liste der Karten-IDs
     * @return true, wenn das Deck erfolgreich gesetzt wurde, sonst false
     */
    public static boolean setUserDeck(String username, List<String> cardIds) {
        if (cardIds.size() != 4) {
            return false; // Deck muss genau 4 Karten enthalten
        }

        String deleteOldDeck = "DELETE FROM decks WHERE username = ?";
        String insertDeckCard = "INSERT INTO decks (username, card_id) VALUES (?, ?)";
        String checkOwnership = "SELECT COUNT(*) FROM user_cards WHERE username = ? AND card_id = ?";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteOldDeck);
                 PreparedStatement insertStmt = conn.prepareStatement(insertDeckCard);
                 PreparedStatement checkStmt = conn.prepareStatement(checkOwnership)) {

                // Überprüfe, ob der Benutzer alle angegebenen Karten besitzt
                for (String cardId : cardIds) {
                    checkStmt.setString(1, username);
                    checkStmt.setObject(2, UUID.fromString(cardId));
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        if (count == 0) {
                            conn.rollback();
                            return false; // Benutzer besitzt die Karte nicht
                        }
                    } else {
                        conn.rollback();
                        return false;
                    }
                }

                // Altes Deck löschen
                deleteStmt.setString(1, username);
                deleteStmt.executeUpdate();

                // Neues Deck setzen
                for (String cardId : cardIds) {
                    insertStmt.setString(1, username);
                    insertStmt.setObject(2, UUID.fromString(cardId));
                    insertStmt.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;

            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Aktualisiert das Token eines Benutzers in der Datenbank.
     *
     * @param username Benutzername
     * @param token    Authentifizierungstoken
     * @return true, wenn das Token erfolgreich aktualisiert wurde, sonst false
     */
    public static boolean updateToken(String username, String token) {
        String updateToken = "UPDATE users SET token = ? WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateToken)) {

            stmt.setString(1, token);
            stmt.setString(2, username);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Ruft den Benutzernamen anhand des Tokens ab.
     *
     * @param token Authentifizierungstoken
     * @return Benutzername oder null, wenn kein Benutzer gefunden wurde
     */
    public static String getUsernameByToken(String token) {
        String query = "SELECT username FROM users WHERE token = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            } else {
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Ruft die Coins eines Benutzers ab.
     *
     * @param username Benutzername
     * @return Anzahl der Coins oder -1 bei Fehler
     */
    public static int getUserCoins(String username) {
        String query = "SELECT coins FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("coins");
            } else {
                return -1;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Aktualisiert die Coins eines Benutzers.
     *
     * @param username Benutzername
     * @param coins    Neue Anzahl der Coins
     * @return true, wenn die Coins erfolgreich aktualisiert wurden, sonst false
     */
    public static boolean updateUserCoins(String username, int coins) {
        String updateCoins = "UPDATE users SET coins = ? WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateCoins)) {

            stmt.setInt(1, coins);
            stmt.setString(2, username);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Ruft die ELO eines Benutzers ab.
     *
     * @param username Benutzername
     * @return ELO-Wert oder 100 bei Fehler
     */
    public static int getUserElo(String username) {
        String query = "SELECT elo FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("elo");
            } else {
                return 100; // Standardwert
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return 100; // Standardwert bei Fehler
        }
    }

    /**
     * Aktualisiert die ELO eines Benutzers.
     *
     * @param username   Benutzername
     * @param eloChange Veränderung des ELO-Werts
     * @return true, wenn die ELO erfolgreich aktualisiert wurde, sonst false
     */
    public static boolean updateUserElo(String username, int eloChange) {
        String updateElo = "UPDATE users SET elo = elo + ? WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateElo)) {

            stmt.setInt(1, eloChange);
            stmt.setString(2, username);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Transferiert eine Karte von einem Benutzer zu einem anderen.
     *
     * @param fromUser Benutzer, der die Karte abgibt
     * @param toUser   Benutzer, der die Karte erhält
     * @param cardId   ID der zu transferierenden Karte
     * @return true, wenn der Transfer erfolgreich war, sonst false
     */
    public static boolean transferCard(String fromUser, String toUser, String cardId) {
        String deleteCard = "DELETE FROM user_cards WHERE username = ? AND card_id = ?";
        String insertCard = "INSERT INTO user_cards (username, card_id) VALUES (?, ?)";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteCard);
                 PreparedStatement insertStmt = conn.prepareStatement(insertCard)) {

                deleteStmt.setString(1, fromUser);
                deleteStmt.setString(2, cardId);
                int deleted = deleteStmt.executeUpdate();
                if (deleted != 1) {
                    conn.rollback();
                    return false;
                }

                insertStmt.setString(1, toUser);
                insertStmt.setString(2, cardId);
                int inserted = insertStmt.executeUpdate();
                if (inserted != 1) {
                    conn.rollback();
                    return false;
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Ruft die Benutzerdaten für die GET /users/{username} Anfrage ab.
     *
     * @param username Benutzername
     * @return User Objekt oder null bei Fehler
     */
    public static User getUser(String username) {
        String query = "SELECT username, coins, elo FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String uname = rs.getString("username");
                int coins = rs.getInt("coins");
                int elo = rs.getInt("elo");
                return new User(uname, coins, elo);
            } else {
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Aktualisiert die Benutzerdaten (z.B. Passwort) für die PUT /users/{username} Anfrage.
     *
     * @param username    Benutzername
     * @param newPassword Neues Passwort (im Klartext, sollte gehasht gespeichert werden)
     * @return true, wenn die Aktualisierung erfolgreich war, sonst false
     */
    public static boolean updateUserPassword(String username, String newPassword) {
        String updatePassword = "UPDATE users SET password = ? WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updatePassword)) {

            stmt.setString(1, newPassword); // In der Praxis sollte das Passwort gehasht werden
            stmt.setString(2, username);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Überprüft, ob eine Karte im Stack eines Benutzers ist (nicht im Deck).
     *
     * @param username Benutzername
     * @param cardId   Karten-ID
     * @return true, wenn die Karte im Stack ist, sonst false
     */
    public static boolean isCardInStack(String username, String cardId) {
        String query = "SELECT COUNT(*) FROM user_cards WHERE username = ? AND card_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setObject(2, UUID.fromString(cardId));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            } else {
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
