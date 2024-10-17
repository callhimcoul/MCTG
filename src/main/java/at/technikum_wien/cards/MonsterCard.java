package at.technikum_wien.cards;

public class MonsterCard extends Card {
    private String elementType; // 'fire', 'water', 'normal'

    // Leerer Konstruktor für die JSON-Deserialisierung
    public MonsterCard() {
        super();
    }

    public MonsterCard(String id, String name, double damage, String elementType) {
        super(id, name, damage);
        this.elementType = elementType;
    }

    @Override
    public String getType() {
        return "Monster";
    }

    public String getElementType() {
        return elementType;
    }
}
