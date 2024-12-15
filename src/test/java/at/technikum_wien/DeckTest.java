package at.technikum_wien;

import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DeckTest {

    // Predefined card IDs for testing - these must be stable and known
    private static final String CARD1_ID = "11111111-1111-1111-1111-111111111111";
    private static final String CARD2_ID = "22222222-2222-2222-2222-222222222222";
    private static final String CARD3_ID = "33333333-3333-3333-3333-333333333333";
    private static final String CARD4_ID = "44444444-4444-4444-4444-444444444444";

    // We'll use a random username for each test to avoid duplicate key conflicts
    private String username;

    @BeforeAll
    static void beforeAll() {
        // Insert the test cards once before all tests.
        // If they exist, ignore errors.
        insertCardIfNotExists(CARD1_ID, "TestCard1", 10, "Monster", "normal");
        insertCardIfNotExists(CARD2_ID, "TestCard2", 20, "Monster", "fire");
        insertCardIfNotExists(CARD3_ID, "TestCard3", 30, "Monster", "water");
        insertCardIfNotExists(CARD4_ID, "TestCard4", 40, "Monster", "normal");
    }

    @BeforeEach
    void setup() {
        // Unique username per test
        username = "deckuser_" + UUID.randomUUID();

        // Create the user
        boolean created = UserDatabase.createUser(username, "deckpass");
        assertTrue(created, "User should be created successfully for test");

        // Add 4 known cards to the user
        UserDatabase.addCardToUser(username, CARD1_ID);
        UserDatabase.addCardToUser(username, CARD2_ID);
        UserDatabase.addCardToUser(username, CARD3_ID);
        UserDatabase.addCardToUser(username, CARD4_ID);
    }

    @AfterEach
    void teardown() {
        // Optionally, remove the user and their cards from DB if desired
        // Not strictly required if unique username per test is used
        // But let's clean up to avoid clutter:
        executeUpdate("DELETE FROM user_cards WHERE username = '" + username + "'");
        executeUpdate("DELETE FROM decks WHERE username = '" + username + "'");
        executeUpdate("DELETE FROM users WHERE username = '" + username + "'");
    }

    @Test
    void testSetDeckWithFourCards() {
        List<String> cardIds = UserDatabase.getUserCards(username).stream().map(c -> c.getId()).toList();
        assertEquals(4, cardIds.size(), "User should have 4 cards");
        boolean result = UserDatabase.setUserDeck(username, cardIds);
        assertTrue(result, "Setting deck with 4 owned cards should succeed");
    }

    @Test
    void testSetDeckWrongCardCount() {
        // Try with fewer cards (just 2)
        List<String> cardIds = UserDatabase.getUserCards(username).stream().map(c -> c.getId()).limit(2).toList();
        boolean result = UserDatabase.setUserDeck(username, cardIds);
        assertFalse(result, "Setting deck with fewer than 4 cards should fail");
    }

    @Test
    void testSetDeckNonOwnedCard() {
        // Add a random, non-existing card id
        List<String> originalCards = UserDatabase.getUserCards(username).stream().map(c -> c.getId()).toList();
        String nonOwnedCardId = "99999999-9999-9999-9999-999999999999"; // not inserted into cards table
        List<String> cardIds = List.of(originalCards.get(0), originalCards.get(1), originalCards.get(2), nonOwnedCardId);

        boolean result = UserDatabase.setUserDeck(username, cardIds);
        assertFalse(result, "Setting deck with a non-owned card should fail");
    }

    /**
     * Inserts a card into the cards table if it does not exist.
     */
    private static void insertCardIfNotExists(String cardId, String name, double damage, String type, String element) {
        // We'll try to insert and if fails due to duplicate, we ignore.
        // Also, handle any SQL exception here.
        String sql = "INSERT INTO cards (id, name, damage, type, element) VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, UUID.fromString(cardId));
            stmt.setString(2, name);
            stmt.setDouble(3, damage);
            stmt.setString(4, type);
            stmt.setString(5, element);
            stmt.executeUpdate();

        } catch (SQLException e) {
            // Ignore errors if card exists, or rethrow as runtime.
            // Just swallow here as simplest approach.
        }
    }

    /**
     * Executes an update SQL, ignoring exceptions.
     */
    private void executeUpdate(String sql) {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            // Swallow or rethrow as unchecked to avoid checked exception propagation
        }
    }
}
