package at.technikum_wien;

import at.technikum_wien.cards.Card;
import at.technikum_wien.cards.MonsterCard;
import at.technikum_wien.cards.SpellCard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class PackageDatabase {

    public static boolean createPackage(List<Card> cards) {
        if (cards.size() != 5) {
            return false;
        }

        String insertPackage = "INSERT INTO packages DEFAULT VALUES RETURNING id";
        String insertCard = "INSERT INTO cards (id, name, damage, type, element) VALUES (?, ?, ?, ?, ?)";
        String insertPackageCard = "INSERT INTO package_cards (package_id, card_id) VALUES (?, ?)";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false); // Beginne Transaktion

            try (PreparedStatement packageStmt = conn.prepareStatement(insertPackage);
                 PreparedStatement cardStmt = conn.prepareStatement(insertCard);
                 PreparedStatement packageCardStmt = conn.prepareStatement(insertPackageCard)) {

                // Paket erstellen
                ResultSet rs = packageStmt.executeQuery();
                if (rs.next()) {
                    int packageId = rs.getInt(1);

                    // Karten hinzufügen
                    for (Card card : cards) {
                        // Karte einfügen
                        cardStmt.setObject(1, UUID.fromString(card.getId()));
                        cardStmt.setString(2, card.getName());
                        cardStmt.setDouble(3, card.getDamage());
                        cardStmt.setString(4, card.getType());
                        String elementType = null;
                        if (card instanceof MonsterCard) {
                            elementType = ((MonsterCard) card).getElementType();
                        } else if (card instanceof SpellCard) {
                            elementType = ((SpellCard) card).getElementType();
                        }
                        cardStmt.setString(5, elementType);
                        cardStmt.executeUpdate();

                        // Beziehung zwischen Paket und Karte erstellen
                        packageCardStmt.setInt(1, packageId);
                        packageCardStmt.setObject(2, UUID.fromString(card.getId()));
                        packageCardStmt.executeUpdate();
                    }

                    conn.commit(); // Transaktion abschließen
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
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

    public static boolean purchasePackage(String username) {
        String getPackage = "SELECT id FROM packages WHERE id NOT IN (SELECT package_id FROM acquired_packages) LIMIT 1";
        String insertUserCard = "INSERT INTO user_cards (username, card_id) VALUES (?, ?)";
        String insertAcquiredPackage = "INSERT INTO acquired_packages (username, package_id) VALUES (?, ?)";
        String getCoins = "SELECT coins FROM users WHERE username = ?";
        String updateCoins = "UPDATE users SET coins = coins - 5 WHERE username = ?";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement packageStmt = conn.prepareStatement(getPackage);
                 PreparedStatement userCardStmt = conn.prepareStatement(insertUserCard);
                 PreparedStatement acquiredPackageStmt = conn.prepareStatement(insertAcquiredPackage);
                 PreparedStatement coinsStmt = conn.prepareStatement(getCoins);
                 PreparedStatement updateCoinsStmt = conn.prepareStatement(updateCoins)) {

                // Überprüfen, ob der Benutzer genug Coins hat
                coinsStmt.setString(1, username);
                ResultSet coinsRs = coinsStmt.executeQuery();
                if (coinsRs.next()) {
                    int coins = coinsRs.getInt("coins");
                    if (coins < 5) {
                        conn.rollback();
                        return false;
                    }
                } else {
                    conn.rollback();
                    return false;
                }

                // Paket auswählen
                ResultSet packageRs = packageStmt.executeQuery();
                if (packageRs.next()) {
                    int packageId = packageRs.getInt("id");

                    // Karten des Pakets abrufen
                    String getCards = "SELECT card_id FROM package_cards WHERE package_id = ?";
                    try (PreparedStatement cardsStmt = conn.prepareStatement(getCards)) {
                        cardsStmt.setInt(1, packageId);
                        ResultSet cardsRs = cardsStmt.executeQuery();

                        // Karten dem Benutzer hinzufügen
                        while (cardsRs.next()) {
                            UUID cardId = (UUID) cardsRs.getObject("card_id");
                            userCardStmt.setString(1, username);
                            userCardStmt.setObject(2, cardId);
                            userCardStmt.executeUpdate();
                        }
                    }

                    // Paket als erworben markieren
                    acquiredPackageStmt.setString(1, username);
                    acquiredPackageStmt.setInt(2, packageId);
                    acquiredPackageStmt.executeUpdate();

                    // Coins abziehen
                    updateCoinsStmt.setString(1, username);
                    updateCoinsStmt.executeUpdate();

                    conn.commit();
                    return true;
                } else {
                    // Keine Pakete verfügbar
                    conn.rollback();
                    return false;
                }
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
}
