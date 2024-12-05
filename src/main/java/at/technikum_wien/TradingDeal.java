package at.technikum_wien;

import java.util.UUID;

public class TradingDeal {
    private String id;
    private String owner;
    private String cardToTrade;
    private String requiredType; // "Monster" oder "Spell"
    private String requiredElement; // optional, z.B. "fire", "water", "normal"
    private Double minimumDamage; // optional

    public TradingDeal() {
        this.id = UUID.randomUUID().toString();
    }

    // Getter-Methoden
    public String getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getCardToTrade() {
        return cardToTrade;
    }

    public String getRequiredType() {
        return requiredType;
    }

    public String getRequiredElement() {
        return requiredElement;
    }

    public Double getMinimumDamage() {
        return minimumDamage;
    }

    // Setter-Methoden
    public void setId(String id) {
        this.id = id;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setCardToTrade(String cardToTrade) {
        this.cardToTrade = cardToTrade;
    }

    public void setRequiredType(String requiredType) {
        this.requiredType = requiredType;
    }

    public void setRequiredElement(String requiredElement) {
        this.requiredElement = requiredElement;
    }

    public void setMinimumDamage(Double minimumDamage) {
        this.minimumDamage = minimumDamage;
    }
}
