package at.technikum_wien.cards;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MonsterCard.class, name = "Monster"),
        @JsonSubTypes.Type(value = SpellCard.class, name = "Spell")
})
public abstract class Card {
    private String id; // Eindeutige Kennung (UUID)
    private String name;
    private double damage;

    // Leerer Konstruktor für die JSON-Deserialisierung
    public Card() {}

    public Card(String id, String name, double damage) {
        this.id = id;
        this.name = name;
        this.damage = damage;
    }

    // Getter-Methoden
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getDamage() {
        return damage;
    }

    // Abstrakte Methode, die von den Unterklassen implementiert wird
    public abstract String getType();
}
