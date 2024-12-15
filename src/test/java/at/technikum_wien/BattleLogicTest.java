package at.technikum_wien;

import org.junit.jupiter.api.*;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BattleLogicTest {

    private String userA;
    private String userB;
    private String cardA1, cardA2, cardA3, cardA4;
    private String cardB1, cardB2, cardB3, cardB4;

    @BeforeEach
    void setup() throws SQLException {
        // Generate unique usernames
        userA = "battleA_" + System.currentTimeMillis();
        userB = "battleB_" + System.currentTimeMillis();

        // Clean up if they exist
        deleteUserIfExists(userA);
        deleteUserIfExists(userB);

        // Create users
        UserDatabase.createUser(userA, "bapass");
        UserDatabase.createUser(userB, "bbpass");

        // Generate card IDs
        cardA1 = UUID.randomUUID().toString();
        cardA2 = UUID.randomUUID().toString();
        cardA3 = UUID.randomUUID().toString();
        cardA4 = UUID.randomUUID().toString();

        cardB1 = UUID.randomUUID().toString();
        cardB2 = UUID.randomUUID().toString();
        cardB3 = UUID.randomUUID().toString();
        cardB4 = UUID.randomUUID().toString();

        // Clean up cards if any exist with these IDs
        deleteCardIfExists(cardA1);
        deleteCardIfExists(cardA2);
        deleteCardIfExists(cardA3);
        deleteCardIfExists(cardA4);
        deleteCardIfExists(cardB1);
        deleteCardIfExists(cardB2);
        deleteCardIfExists(cardB3);
        deleteCardIfExists(cardB4);

        // Insert cards into DB
        insertCard(cardA1, "CardA1", 20.0, "Monster", "fire");
        insertCard(cardA2, "CardA2", 30.0, "Monster", "water");
        insertCard(cardA3, "CardA3", 25.0, "Spell", "normal");
        insertCard(cardA4, "CardA4", 10.0, "Monster", "normal");

        insertCard(cardB1, "CardB1", 15.0, "Monster", "fire");
        insertCard(cardB2, "CardB2", 35.0, "Spell", "water");
        insertCard(cardB3, "CardB3", 50.0, "Monster", "normal");
        insertCard(cardB4, "CardB4", 22.0, "Spell", "fire");

        // Add cards to users
        UserDatabase.addCardToUser(userA, cardA1);
        UserDatabase.addCardToUser(userA, cardA2);
        UserDatabase.addCardToUser(userA, cardA3);
        UserDatabase.addCardToUser(userA, cardA4);

        UserDatabase.addCardToUser(userB, cardB1);
        UserDatabase.addCardToUser(userB, cardB2);
        UserDatabase.addCardToUser(userB, cardB3);
        UserDatabase.addCardToUser(userB, cardB4);

        // Set decks
        UserDatabase.setUserDeck(userA, List.of(cardA1, cardA2, cardA3, cardA4));
        UserDatabase.setUserDeck(userB, List.of(cardB1, cardB2, cardB3, cardB4));
    }

    @AfterEach
    void teardown() throws SQLException {
        // Cleanup deck, cards, users
        deleteDeck(userA);
        deleteDeck(userB);
        deleteUserCards(userA);
        deleteUserCards(userB);
        deleteUserIfExists(userA);
        deleteUserIfExists(userB);

        deleteCardIfExists(cardA1);
        deleteCardIfExists(cardA2);
        deleteCardIfExists(cardA3);
        deleteCardIfExists(cardA4);
        deleteCardIfExists(cardB1);
        deleteCardIfExists(cardB2);
        deleteCardIfExists(cardB3);
        deleteCardIfExists(cardB4);
    }

    @Test
    void testBoosterCardEffect() throws Exception {
        UserDatabase.updateUserBoosterCard(userA, cardA1);

        // Provide a dummy writer to avoid NullPointerException
        BufferedWriter dummyWriter = new BufferedWriter(new OutputStreamWriter(new ByteArrayOutputStream()));
        Player p1 = new Player(userA, UserDatabase.getUserDeck(userA), dummyWriter, new Object(), UserDatabase.getUser(userA).getBoosterCardId());
        Player p2 = new Player(userB, UserDatabase.getUserDeck(userB), dummyWriter, new Object());

        Battle battle = new Battle(p1, p2);
        assertDoesNotThrow(battle::start, "Battle should run without exceptions");
    }

    @Test
    void testRegularBattleNoExceptions() {
        BufferedWriter dummyWriter = new BufferedWriter(new OutputStreamWriter(new ByteArrayOutputStream()));
        Player p1 = new Player(userA, UserDatabase.getUserDeck(userA), dummyWriter, new Object());
        Player p2 = new Player(userB, UserDatabase.getUserDeck(userB), dummyWriter, new Object());

        Battle battle = new Battle(p1, p2);
        assertDoesNotThrow(battle::start, "Battle should run without exceptions");
    }

    @Test
    void testEloChangeOnWin() throws Exception {
        int eloA_before = UserDatabase.getUser(userA).getElo();
        int eloB_before = UserDatabase.getUser(userB).getElo();

        BufferedWriter dummyWriter = new BufferedWriter(new OutputStreamWriter(new ByteArrayOutputStream()));
        Player p1 = new Player(userA, UserDatabase.getUserDeck(userA), dummyWriter, new Object());
        Player p2 = new Player(userB, UserDatabase.getUserDeck(userB), dummyWriter, new Object());

        Battle battle = new Battle(p1, p2);
        battle.start();

        int eloA_after = UserDatabase.getUser(userA).getElo();
        int eloB_after = UserDatabase.getUser(userB).getElo();

        boolean changed = (eloA_after != eloA_before) || (eloB_after != eloB_before);
        assertTrue(changed, "ELO should change unless it's a complete draw");
    }

    @Test
    void testGamesPlayedIncrement() throws Exception {
        int gamesA_before = UserDatabase.getUser(userA).getGamesPlayed();
        int gamesB_before = UserDatabase.getUser(userB).getGamesPlayed();

        BufferedWriter dummyWriter = new BufferedWriter(new OutputStreamWriter(new ByteArrayOutputStream()));
        Player p1 = new Player(userA, UserDatabase.getUserDeck(userA), dummyWriter, new Object());
        Player p2 = new Player(userB, UserDatabase.getUserDeck(userB), dummyWriter, new Object());

        Battle battle = new Battle(p1, p2);
        battle.start();

        int gamesA_after = UserDatabase.getUser(userA).getGamesPlayed();
        int gamesB_after = UserDatabase.getUser(userB).getGamesPlayed();

        assertEquals(gamesA_before + 1, gamesA_after, "gamesPlayed for player A should increment by 1");
        assertEquals(gamesB_before + 1, gamesB_after, "gamesPlayed for player B should increment by 1");
    }

    private void insertCard(String id, String name, double damage, String type, String element) throws SQLException {
        String insert = "INSERT INTO cards (id, name, damage, type, element) VALUES (?,?,?,?,?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insert)) {
            stmt.setObject(1, UUID.fromString(id));
            stmt.setString(2, name);
            stmt.setDouble(3, damage);
            stmt.setString(4, type);
            stmt.setString(5, element);
            stmt.executeUpdate();
        }
    }

    private void deleteCardIfExists(String cardId) throws SQLException {
        String delete = "DELETE FROM cards WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(delete)) {
            stmt.setObject(1, UUID.fromString(cardId));
            stmt.executeUpdate();
        }
    }

    private void deleteUserIfExists(String username) throws SQLException {
        // First remove decks and user_cards
        deleteDeck(username);
        deleteUserCards(username);
        String delete = "DELETE FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(delete)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }

    private void deleteUserCards(String username) throws SQLException {
        String delete = "DELETE FROM user_cards WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(delete)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }

    private void deleteDeck(String username) throws SQLException {
        String delete = "DELETE FROM decks WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(delete)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }
}
