package at.technikum_wien;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserDatabaseTest {

    @BeforeEach
    void setup() {
        // Optionally clean up or ensure no conflicts
    }

    @AfterEach
    void teardown() {
        // Optionally remove created users
    }

    @Test
    void testCreateUser() {
        boolean created = UserDatabase.createUser("testuser", "testpass");
        assertTrue(created, "User should be created successfully");
    }

    @Test
    void testAuthenticateUser() {
        UserDatabase.createUser("authuser", "secret");
        assertTrue(UserDatabase.authenticateUser("authuser", "secret"), "User should authenticate successfully");
        assertFalse(UserDatabase.authenticateUser("authuser", "wrong"), "Authentication should fail with wrong password");
    }

    @Test
    void testGetUserNotExists() {
        assertNull(UserDatabase.getUser("nonexistent"), "Should return null for non-existing user");
    }

    @Test
    void testTokenUpdate() {
        UserDatabase.createUser("tokenuser", "tokenpass");
        boolean updated = UserDatabase.updateToken("tokenuser", "tokenuser-mtcgToken");
        assertTrue(updated, "Token should be updated");
        String uname = UserDatabase.getUsernameByToken("tokenuser-mtcgToken");
        assertEquals("tokenuser", uname, "Should return correct username from token");
    }

    @Test
    void testUserProfileUpdate() {
        UserDatabase.createUser("profileuser", "profilepass");
        boolean updated = UserDatabase.updateUserProfile("profileuser", "New Bio", "http://image.png");
        assertTrue(updated, "Profile should be updated");
        User u = UserDatabase.getUser("profileuser");
        assertEquals("New Bio", u.getBio());
        assertEquals("http://image.png", u.getImage());
    }
}
