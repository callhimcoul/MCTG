package at.technikum_wien;

import at.technikum_wien.cards.Card;
import at.technikum_wien.cards.MonsterCard;
import at.technikum_wien.cards.SpellCard;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradingDatabase {

    public static boolean createTradingDeal(TradingDeal deal) {
        String insertDeal = "INSERT INTO trading_deals " +
                "(id, owner, card_to_trade, required_type, required_element, minimum_damage) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertDeal)) {

            // For UUID columns, use setObject(..., UUID.fromString(...))
            stmt.setObject(1, UUID.fromString(deal.getId()));          // id (UUID)
            stmt.setString(2, deal.getOwner());                        // owner (text)
            stmt.setObject(3, UUID.fromString(deal.getCardToTrade())); // card_to_trade (UUID)
            stmt.setString(4, deal.getRequiredType());                 // required_type (text)
            stmt.setString(5, deal.getRequiredElement());              // required_element (text)

            if (deal.getMinimumDamage() != null) {
                stmt.setDouble(6, deal.getMinimumDamage());
            } else {
                stmt.setNull(6, java.sql.Types.DOUBLE);
            }

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static List<TradingDeal> getAllTradingDeals() {
        String query = "SELECT * FROM trading_deals";
        List<TradingDeal> deals = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                TradingDeal deal = new TradingDeal();
                deal.setId(rs.getString("id"));
                deal.setOwner(rs.getString("owner"));
                deal.setCardToTrade(rs.getString("card_to_trade"));
                deal.setRequiredType(rs.getString("required_type"));
                deal.setRequiredElement(rs.getString("required_element"));
                deal.setMinimumDamage(rs.getDouble("minimum_damage"));
                deals.add(deal);
            }
            return deals;

        } catch (SQLException e) {
            e.printStackTrace();
            return deals;
        }
    }

    public static boolean acceptTradingDeal(String dealId, String buyer, String offeredCardId) {
        String selectDeal = "SELECT * FROM trading_deals WHERE id = ?";
        String deleteDeal = "DELETE FROM trading_deals WHERE id = ?";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement selectStmt = conn.prepareStatement(selectDeal);
                 PreparedStatement deleteStmt = conn.prepareStatement(deleteDeal)) {

                // Handelsangebot abrufen
                selectStmt.setString(1, dealId);
                ResultSet rs = selectStmt.executeQuery();
                if (!rs.next()) {
                    conn.rollback();
                    return false;
                }

                TradingDeal deal = new TradingDeal();
                deal.setId(rs.getString("id"));
                deal.setOwner(rs.getString("owner"));
                deal.setCardToTrade(rs.getString("card_to_trade"));
                deal.setRequiredType(rs.getString("required_type"));
                deal.setRequiredElement(rs.getString("required_element"));
                deal.setMinimumDamage(rs.getDouble("minimum_damage"));

                // Überprüfen, ob der Käufer die Anforderungen erfüllt
                Card offeredCard = UserDatabase.getCardById(offeredCardId);
                if (offeredCard == null || !offeredCard.getType().equalsIgnoreCase(deal.getRequiredType())) {
                    conn.rollback();
                    return false;
                }

                if (deal.getRequiredElement() != null) {
                    String elementType = offeredCard instanceof MonsterCard ?
                            ((MonsterCard) offeredCard).getElementType() :
                            ((SpellCard) offeredCard).getElementType();

                    if (!elementType.equalsIgnoreCase(deal.getRequiredElement())) {
                        conn.rollback();
                        return false;
                    }
                }

                if (deal.getMinimumDamage() != null && offeredCard.getDamage() < deal.getMinimumDamage()) {
                    conn.rollback();
                    return false;
                }

                // Karten zwischen den Benutzern tauschen
                boolean transfer1 = UserDatabase.transferCard(deal.getOwner(), buyer, deal.getCardToTrade());
                boolean transfer2 = UserDatabase.transferCard(buyer, deal.getOwner(), offeredCardId);

                if (transfer1 && transfer2) {
                    // Handelsangebot löschen
                    deleteStmt.setString(1, dealId);
                    deleteStmt.executeUpdate();

                    conn.commit();
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
}
