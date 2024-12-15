package at.technikum_wien;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TradingTest {

    @BeforeEach
    void setup() {
        UserDatabase.createUser("traderA", "passA");
        UserDatabase.createUser("traderB", "passB");
        String cardIdA = java.util.UUID.randomUUID().toString();
        String cardIdB = java.util.UUID.randomUUID().toString();
        UserDatabase.addCardToUser("traderA", cardIdA);
        UserDatabase.addCardToUser("traderB", cardIdB);

        TradingDeal deal = new TradingDeal();
        deal.setOwner("traderA");
        deal.setCardToTrade(cardIdA);
        deal.setRequiredType("Monster");
        deal.setMinimumDamage(10.0);
        TradingDatabase.createTradingDeal(deal);
    }

    @Test
    void testGetAllTradingDeals() {
        var deals = TradingDatabase.getAllTradingDeals();
        assertFalse(deals.isEmpty(), "Should have at least one trading deal");
    }

    @Test
    void testAcceptTradingDeal() {
        var deals = TradingDatabase.getAllTradingDeals();
        TradingDeal deal = deals.get(0);

        var traderBCards = UserDatabase.getUserCards("traderB");
        assertFalse(traderBCards.isEmpty(), "TraderB should have a card");
        String offeredCard = traderBCards.get(0).getId();
        // Assume offered card meets requirements
        boolean accepted = TradingDatabase.acceptTradingDeal(deal.getId(), "traderB", offeredCard);
        assertTrue(accepted, "Deal should be accepted if requirements match");
    }

    @Test
    void testAcceptTradingDealNoMatch() {
        var deals = TradingDatabase.getAllTradingDeals();
        TradingDeal deal = deals.get(0);
        // Try a random card that doesn't exist or doesn't meet requirements
        boolean accepted = TradingDatabase.acceptTradingDeal(deal.getId(), "traderB", java.util.UUID.randomUUID().toString());
        assertFalse(accepted, "Deal should not be accepted if the card does not meet requirements");
    }
}
