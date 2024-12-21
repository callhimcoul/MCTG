package at.technikum_wien;

import at.technikum_wien.cards.Card;
import at.technikum_wien.cards.MonsterCard;
import at.technikum_wien.cards.SpellCard;
import org.junit.jupiter.api.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectUnitTests {

    @Test
    @DisplayName("A) Player hasCards: returns false when deck is empty")
    void testPlayerHasNoCardsWhenEmpty() {
        List<Card> emptyDeck = new ArrayList<>();
        Player player = new Player("EmptyDeckPlayer", emptyDeck, null, new Object());
        assertFalse(player.hasCards(), "Player should have zero cards, so hasCards() should be false");
    }

    @Test
    @DisplayName("B) Player hasCards: returns true when deck has at least one card")
    void testPlayerHasCardsWhenNotEmpty() {
        List<Card> deck = new ArrayList<>();
        deck.add(new MonsterCard("id1", "Monster1", 10.0, "fire"));
        Player player = new Player("NonEmptyDeckPlayer", deck, null, new Object());
        assertTrue(player.hasCards(), "Player deck has 1 card, so hasCards() should be true");
    }

    @Test
    @DisplayName("C) MonsterCard constructor sets ID, name, damage, and element correctly")
    void testMonsterCardConstructorSimple() {
        MonsterCard mc = new MonsterCard("m-id", "MyMonster", 25.0, "water");
        assertEquals("m-id", mc.getId());
        assertEquals("MyMonster", mc.getName());
        assertEquals(25.0, mc.getDamage(), 0.0001);
        assertEquals("water", mc.getElementType());
    }

    @Test
    @DisplayName("D) SpellCard constructor sets ID, name, damage, and element correctly")
    void testSpellCardConstructorSimple() {
        SpellCard sc = new SpellCard("s-id", "MySpell", 30.0, "fire");
        assertEquals("s-id", sc.getId());
        assertEquals("MySpell", sc.getName());
        assertEquals(30.0, sc.getDamage(), 0.0001);
        assertEquals("fire", sc.getElementType());
    }

    @Test
    @DisplayName("E) Battle getEffectivenessMultiplier: unrecognized elements => 1.0")
    void testEffectivenessMultiplierUnrecognizedElements() {
        // If neither attacker nor defender has recognized elements
        Battle battle = new Battle(null, null);
        double multiplier = battle.getEffectivenessMultiplier("lightning", "shadow");
        assertEquals(1.0, multiplier, 0.0001,
                "Unrecognized element combos should return a multiplier of 1.0");
    }

    @Test
    @DisplayName("1) MonsterCard: getElementType returns correct element")
    void testMonsterCardElementType() {
        MonsterCard mc = new MonsterCard("123e4567-e89b-12d3-a456-426614174000", "FireDragon", 50.0, "fire");
        assertEquals("fire", mc.getElementType());
    }

    @Test
    @DisplayName("2) MonsterCard: getType returns 'Monster'")
    void testMonsterCardType() {
        MonsterCard mc = new MonsterCard("id", "Goblin", 20.0, "normal");
        assertEquals("Monster", mc.getType());
    }

    @Test
    @DisplayName("3) SpellCard: getElementType returns correct element")
    void testSpellCardElementType() {
        SpellCard sc = new SpellCard("id", "WaterSpell", 15.0, "water");
        assertEquals("water", sc.getElementType());
    }

    @Test
    @DisplayName("4) SpellCard: getType returns 'Spell'")
    void testSpellCardType() {
        SpellCard sc = new SpellCard("id", "AnySpell", 10.0, "normal");
        assertEquals("Spell", sc.getType());
    }

    @Test
    @DisplayName("5) Battle getEffectivenessMultiplier: water vs fire => 2.0")
    void testEffectivenessMultiplierWaterFire() {
        Battle battle = new Battle(null, null);
        double multiplier = battle.getEffectivenessMultiplier("water", "fire");
        assertEquals(2.0, multiplier);
    }

    @Test
    @DisplayName("6) Battle getEffectivenessMultiplier: fire vs water => 0.5")
    void testEffectivenessMultiplierFireWater() {
        Battle battle = new Battle(null, null);
        double multiplier = battle.getEffectivenessMultiplier("fire", "water");
        assertEquals(0.5, multiplier);
    }

    @Test
    @DisplayName("7) Battle getEffectivenessMultiplier: fire vs normal => 2.0")
    void testEffectivenessMultiplierFireNormal() {
        Battle battle = new Battle(null, null);
        double multiplier = battle.getEffectivenessMultiplier("fire", "normal");
        assertEquals(2.0, multiplier);
    }

    @Test
    @DisplayName("8) Battle getEffectivenessMultiplier: normal vs fire => 0.5")
    void testEffectivenessMultiplierNormalFire() {
        Battle battle = new Battle(null, null);
        double multiplier = battle.getEffectivenessMultiplier("normal", "fire");
        assertEquals(0.5, multiplier);
    }

    @Test
    @DisplayName("9) Battle getEffectivenessMultiplier: normal vs water => 2.0")
    void testEffectivenessMultiplierNormalWater() {
        Battle battle = new Battle(null, null);
        double multiplier = battle.getEffectivenessMultiplier("normal", "water");
        assertEquals(2.0, multiplier);
    }

    @Test
    @DisplayName("10) Battle getEffectivenessMultiplier: water vs normal => 0.5")
    void testEffectivenessMultiplierWaterNormal() {
        Battle battle = new Battle(null, null);
        double multiplier = battle.getEffectivenessMultiplier("water", "normal");
        assertEquals(0.5, multiplier);
    }

    @Test
    @DisplayName("11) Battle getEffectivenessMultiplier: normal vs normal => 1.0")
    void testEffectivenessMultiplierNormalNormal() {
        Battle battle = new Battle(null, null);
        double multiplier = battle.getEffectivenessMultiplier("normal", "normal");
        assertEquals(1.0, multiplier);
    }


    @Test
    @DisplayName("16) applySpecialRules: FireElf vs Dragon => FireElves can evade => returns true (draw)")
    void testApplySpecialRulesFireElfDragon() {
        Battle battle = new Battle(null, null);
        Card fireElf = new MonsterCard("id1", "FireElf", 15.0, "fire");
        Card dragon = new MonsterCard("id2", "Dragon", 80.0, "fire");
        boolean applied = battle.applySpecialRules(fireElf, dragon);
        assertTrue(applied);
    }

    @Test
    @DisplayName("17) applySpecialRules: No special case => returns false")
    void testApplySpecialRulesNoSpecial() {
        Battle battle = new Battle(null, null);
        Card monster = new MonsterCard("id1", "RegularMonster", 10.0, "fire");
        Card anotherMonster = new MonsterCard("id2", "AnotherMonster", 12.0, "water");
        boolean applied = battle.applySpecialRules(monster, anotherMonster);
        assertFalse(applied);
    }

    @Test
    @DisplayName("18) Player isBoosterCard: returns true if IDs match and not used yet")
    void testPlayerIsBoosterCardTrue() {
        Card c = new MonsterCard("id1", "BoosterMonster", 50, "normal");
        Player p = new Player("Alice", new ArrayList<>(), null, new Object(), "id1");
        assertTrue(p.isBoosterCard(c));
    }

    @Test
    @DisplayName("19) Player isBoosterCard: returns false if boosterUsed is already true")
    void testPlayerIsBoosterCardAlreadyUsed() {
        Card c = new MonsterCard("id1", "BoosterMonster", 50, "normal");
        Player p = new Player("Alice", new ArrayList<>(), null, new Object(), "id1");
        p.markBoosterUsed(); // booster is now used
        assertFalse(p.isBoosterCard(c));
    }

    @Test
    @DisplayName("21) Player constructor sets username and deck properly")
    void testPlayerConstructor() {
        List<Card> deck = new ArrayList<>();
        deck.add(new MonsterCard("id1", "Monster1", 10, "fire"));
        Player p = new Player("Bob", deck, null, new Object());
        assertEquals("Bob", p.getUsername());
        assertTrue(p.hasCards());
    }


    @Test
    @DisplayName("24) Battle constructor sets players properly")
    void testBattleConstructor() {
        Player p1 = new Player("Alice", new ArrayList<>(), null, new Object());
        Player p2 = new Player("Bob", new ArrayList<>(), null, new Object());
        Battle b = new Battle(p1, p2);
        // There's no direct getter, but we can check that no exception was thrown
        assertNotNull(b);
    }

    @Test
    @DisplayName("25) SpellCard constructor sets all fields properly")
    void testSpellCardConstructor() {
        SpellCard card = new SpellCard("id123", "WaterSpell", 30.0, "water");
        assertEquals("id123", card.getId());
        assertEquals("WaterSpell", card.getName());
        assertEquals(30.0, card.getDamage());
        assertEquals("Spell", card.getType());
        assertEquals("water", card.getElementType());
    }
}
