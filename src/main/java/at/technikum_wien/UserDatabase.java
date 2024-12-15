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
            stmt.setString(2, password);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Löscht einen Benutzer aus der Datenbank.
     *
     * @param username Der zu löschende Benutzername
     * @return true, wenn der Benutzer gelöscht wurde, sonst false
     */
    public static boolean deleteUser(String username) {
        String deleteUserCards = "DELETE FROM user_cards WHERE username = ?";
        String deleteDeck = "DELETE FROM decks WHERE username = ?";
        String deleteUser = "DELETE FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtDeleteCards = conn.prepareStatement(deleteUserCards);
                 PreparedStatement stmtDeleteDeck = conn.prepareStatement(deleteDeck);
                 PreparedStatement stmtDeleteUser = conn.prepareStatement(deleteUser)) {

                stmtDeleteCards.setString(1, username);
                stmtDeleteCards.executeUpdate();

                stmtDeleteDeck.setString(1, username);
                stmtDeleteDeck.executeUpdate();

                stmtDeleteUser.setString(1, username);
                int rows = stmtDeleteUser.executeUpdate();

                conn.commit();
                return rows > 0;
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

    public static boolean authenticateUser(String username, String password) {
        String query = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                return password.equals(storedPassword);
            } else {
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

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

    public static boolean setUserDeck(String username, List<String> cardIds) {
        if (cardIds.size() != 4) {
            return false;
        }

        String deleteOldDeck = "DELETE FROM decks WHERE username = ?";
        String insertDeckCard = "INSERT INTO decks (username, card_id) VALUES (?, ?)";
        String checkOwnership = "SELECT COUNT(*) FROM user_cards WHERE username = ? AND card_id = ?";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteOldDeck);
                 PreparedStatement insertStmt = conn.prepareStatement(insertDeckCard);
                 PreparedStatement checkStmt = conn.prepareStatement(checkOwnership)) {

                for (String cardId : cardIds) {
                    checkStmt.setString(1, username);
                    checkStmt.setObject(2, UUID.fromString(cardId));
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        if (count == 0) {
                            conn.rollback();
                            return false;
                        }
                    } else {
                        conn.rollback();
                        return false;
                    }
                }

                deleteStmt.setString(1, username);
                deleteStmt.executeUpdate();

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

    public static int getUserElo(String username) {
        String query = "SELECT elo FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("elo");
            } else {
                return 100;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return 100;
        }
    }

    public static boolean updateUserElo(String username, int eloChange) {
        String updateElo = "UPDATE users SET elo = GREATEST(0, elo + ?) WHERE username = ?";
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

    public static User getUser(String username) {
        String query = "SELECT username, coins, elo, bio, image, games_played, booster_card_id FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String uname = rs.getString("username");
                int coins = rs.getInt("coins");
                int elo = rs.getInt("elo");
                String bio = rs.getString("bio");
                String image = rs.getString("image");
                int gamesPlayed = rs.getInt("games_played");
                String boosterCardId = rs.getString("booster_card_id");
                return new User(uname, coins, elo, bio, image, gamesPlayed, boosterCardId);
            } else {
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean updateUserProfile(String username, String bio, String image) {
        String updateProfile = "UPDATE users SET bio = ?, image = ? WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateProfile)) {

            stmt.setString(1, bio);
            stmt.setString(2, image);
            stmt.setString(3, username);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateUserPassword(String username, String newPassword) {
        String updatePassword = "UPDATE users SET password = ? WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updatePassword)) {

            stmt.setString(1, newPassword);
            stmt.setString(2, username);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void addCardToUser(String username, String cardId) {
        String query = "INSERT INTO user_cards (username, card_id) VALUES (?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setObject(2, UUID.fromString(cardId));
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeCardFromUser(String username, String cardId) {
        String query = "DELETE FROM user_cards WHERE username = ? AND card_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setObject(2, UUID.fromString(cardId));
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Card getCardById(String cardId) {
        String query = "SELECT * FROM cards WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setObject(1, UUID.fromString(cardId));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                double damage = rs.getDouble("damage");
                String type = rs.getString("type");
                String element = rs.getString("element");

                if ("Monster".equalsIgnoreCase(type)) {
                    return new MonsterCard(id, name, damage, element);
                } else {
                    return new SpellCard(id, name, damage, element);
                }
            } else {
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isCardOwnedByUser(String username, String cardId) {
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

    public static boolean isCardInDeck(String username, String cardId) {
        String query = "SELECT COUNT(*) FROM decks WHERE username = ? AND card_id = ?";
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

    public static List<User> getAllUsersSortedByElo() {
        String query = "SELECT username, coins, elo, bio, image, booster_card_id FROM users ORDER BY elo DESC";
        List<User> users = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String username = rs.getString("username");
                int coins = rs.getInt("coins");
                int elo = rs.getInt("elo");
                String bio = rs.getString("bio");
                String image = rs.getString("image");
                String boosterCardId = rs.getString("booster_card_id");
                User u = new User(username, coins, elo, bio, image, 0, boosterCardId);
                // gamesPlayed not retrieved here, default 0 is okay for scoreboard display
                users.add(u);
            }
            return users;

        } catch (SQLException e) {
            e.printStackTrace();
            return users;
        }
    }

    public static boolean incrementGamesPlayed(String username) {
        String updateGames = "UPDATE users SET games_played = games_played + 1 WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateGames)) {

            stmt.setString(1, username);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

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

    public static boolean updateUserBoosterCard(String username, String boosterCardId) {
        String updateBooster = "UPDATE users SET booster_card_id = ? WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateBooster)) {

            if (boosterCardId != null) {
                stmt.setObject(1, UUID.fromString(boosterCardId));
            } else {
                stmt.setNull(1, Types.OTHER);
            }
            stmt.setString(2, username);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
