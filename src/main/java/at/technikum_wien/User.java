package at.technikum_wien;

public class User {
    private String username;
    private int coins;
    private int elo;
    private String bio;
    private String image;

    // Konstruktor mit den ursprünglichen Feldern
    public User(String username, int coins, int elo) {
        this.username = username;
        this.coins = coins;
        this.elo = elo;
        this.bio = "";   // Standardwert
        this.image = ""; // Standardwert
    }

    // Neuer Konstruktor mit allen Feldern
    public User(String username, int coins, int elo, String bio, String image) {
        this.username = username;
        this.coins = coins;
        this.elo = elo;
        this.bio = bio;
        this.image = image;
    }

    // Getter-Methoden
    public String getUsername() {
        return username;
    }

    public int getCoins() {
        return coins;
    }

    public int getElo() {
        return elo;
    }

    public String getBio() {
        return bio;
    }

    public String getImage() {
        return image;
    }

    // Setter-Methoden
    public void setCoins(int coins) {
        this.coins = coins;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
