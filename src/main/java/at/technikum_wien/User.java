package at.technikum_wien;

public class User {
    private String username;
    private int coins;
    private int elo;
    private String bio;
    private String image;
    private int gamesPlayed;
    private String boosterCardId; // New field for unique feature

    // Konstruktor mit den ursprünglichen Feldern
    public User(String username, int coins, int elo) {
        this.username = username;
        this.coins = coins;
        this.elo = elo;
        this.bio = "";
        this.image = "";
        this.gamesPlayed = 0;
        this.boosterCardId = null;
    }

    // Konstruktor mit allen Feldern außer gamesPlayed und boosterCardId
    public User(String username, int coins, int elo, String bio, String image) {
        this.username = username;
        this.coins = coins;
        this.elo = elo;
        this.bio = bio;
        this.image = image;
        this.gamesPlayed = 0;
        this.boosterCardId = null;
    }

    // Neuer Konstruktor mit allen Feldern einschließlich gamesPlayed und boosterCardId
    public User(String username, int coins, int elo, String bio, String image, int gamesPlayed, String boosterCardId) {
        this.username = username;
        this.coins = coins;
        this.elo = elo;
        this.bio = bio;
        this.image = image;
        this.gamesPlayed = gamesPlayed;
        this.boosterCardId = boosterCardId;
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

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public String getBoosterCardId() {
        return boosterCardId;
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

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public void setBoosterCardId(String boosterCardId) {
        this.boosterCardId = boosterCardId;
    }
}
