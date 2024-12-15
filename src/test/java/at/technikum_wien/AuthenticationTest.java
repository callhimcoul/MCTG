package at.technikum_wien;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationTest {

    @BeforeEach
    void setup() {
        // Ensure we start fresh; delete user if it exists
        UserDatabase.deleteUser("authuser2");
        UserDatabase.createUser("authuser2", "secret2");
    }

    @AfterEach
    void teardown() {
        // Clean up after each test to avoid duplicate key errors
        UserDatabase.deleteUser("authuser2");
    }

    @Test
    void testGetUsernameByToken() {
        UserDatabase.updateToken("authuser2", "authuser2-mtcgToken");
        String username = UserDatabase.getUsernameByToken("authuser2-mtcgToken");
        assertEquals("authuser2", username, "Username should match token");
    }

    @Test
    void testUnauthorizedToken() {
        assertNull(UserDatabase.getUsernameByToken("non-existent-token"), "Should return null for unknown token");
    }

    @Test
    void testIsAdmin() {
        ClientHandler handler = new ClientHandler(null);
        assertTrue(invokeIsAdmin(handler, "admin-mtcgToken"), "Admin token should return true");
        assertFalse(invokeIsAdmin(handler, "randomToken"), "Non-admin token should return false");
    }

    // Reflectively call isAdmin since it's private
    private boolean invokeIsAdmin(ClientHandler handler, String token) {
        try {
            var method = ClientHandler.class.getDeclaredMethod("isAdmin", String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(handler, token);
        } catch (Exception e) {
            fail("Reflection failed", e);
            return false;
        }
    }
}
