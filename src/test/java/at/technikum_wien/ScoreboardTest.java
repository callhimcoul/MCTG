package at.technikum_wien;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class ScoreboardTest {

    @BeforeEach
    void setup() {
        UserDatabase.createUser("scoreuser1", "pass1");
        UserDatabase.createUser("scoreuser2", "pass2");
    }

    @Test
    void testGetAllUsersSortedByElo() {
        List<User> users = UserDatabase.getAllUsersSortedByElo();
        assertNotNull(users, "Should return a list of users");
        assertTrue(users.size() >= 2, "Should have at least the newly created users plus any existing ones");
    }

    @Test
    void testScoreboardEloOrder() {
        UserDatabase.updateUserElo("scoreuser1", 10);
        List<User> users = UserDatabase.getAllUsersSortedByElo();
        int indexUser1 = -1, indexUser2 = -1;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equals("scoreuser1")) indexUser1 = i;
            if (users.get(i).getUsername().equals("scoreuser2")) indexUser2 = i;
        }
        if (indexUser1 != -1 && indexUser2 != -1) {
            assertTrue(indexUser1 < indexUser2, "User1 should appear before User2 due to higher ELO");
        }
    }
}
