package at.technikum_wien.cards;

public class SpellCard extends Card {
    private String elementType;

    public SpellCard() { super(); }

    public SpellCard(String id, String name, double damage, String elementType) {
        super(id, name, damage);
        this.elementType = elementType;
    }

    @Override
    public String getType() {
        return "Spell";
    }

    public String getElementType() {
        return elementType;
    }
}
