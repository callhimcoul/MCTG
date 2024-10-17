package at.technikum_wien;

public class User {
    private String username;
    private int coins;
    private int elo;

    public User(String username, int coins, int elo) {
        this.username = username;
        this.coins = coins;
        this.elo = elo;
    }
    public String getUsername() {
        return username;
    }

    public int getCoins() {
        return coins;
    }

    public int getElo() {
        return elo;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }
}
