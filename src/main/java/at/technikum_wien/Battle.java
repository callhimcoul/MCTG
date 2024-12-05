package at.technikum_wien;

import at.technikum_wien.cards.Card;
import at.technikum_wien.cards.MonsterCard;
import at.technikum_wien.cards.SpellCard;

import java.io.IOException;

public class Battle {
    private Player player1;
    private Player player2;
    private StringBuilder battleLog = new StringBuilder();

    public Battle(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
    }

    public void start() throws IOException {
        battleLog.append("Battle between ").append(player1.getUsername()).append(" and ").append(player2.getUsername()).append("\n\n");

        int round = 0;
        while (round < 100 && player1.hasCards() && player2.hasCards()) {
            round++;
            battleLog.append("Round ").append(round).append(":\n");

            Card card1 = player1.drawRandomCard();
            Card card2 = player2.drawRandomCard();

            battleLog.append(player1.getUsername()).append(" plays ").append(card1.getName()).append(" (Damage: ").append(card1.getDamage()).append(")\n");
            battleLog.append(player2.getUsername()).append(" plays ").append(card2.getName()).append(" (Damage: ").append(card2.getDamage()).append(")\n");

            // Calculate damage with element effectiveness
            double damage1 = calculateDamage(card1, card2);
            double damage2 = calculateDamage(card2, card1);

            // Apply special rules
            boolean specialRuleApplied = applySpecialRules(card1, card2);

            if (!specialRuleApplied) {
                if (damage1 > damage2) {
                    battleLog.append(player1.getUsername()).append(" wins the round!\n\n");
                    player1.addCard(card2);
                    player2.removeCard(card2);
                } else if (damage2 > damage1) {
                    battleLog.append(player2.getUsername()).append(" wins the round!\n\n");
                    player2.addCard(card1);
                    player1.removeCard(card1);
                } else {
                    battleLog.append("It's a draw!\n\n");
                }
            }
        }

        // Determine the winner
        String result;
        if (player1.hasCards() && !player2.hasCards()) {
            result = player1.getUsername() + " wins the battle!";
            updateElo(player1.getUsername(), player2.getUsername(), true);
        } else if (!player1.hasCards() && player2.hasCards()) {
            result = player2.getUsername() + " wins the battle!";
            updateElo(player2.getUsername(), player1.getUsername(), true);
        } else {
            result = "The battle ended in a draw!";
            updateElo(player1.getUsername(), player2.getUsername(), false);
        }

        battleLog.append(result);

        // Send the battle log to both players
        player1.sendBattleResult(battleLog.toString());
        player2.sendBattleResult(battleLog.toString());
    }

    private double calculateDamage(Card attacker, Card defender) {
        double damage = attacker.getDamage();

        // Wenn mindestens eine Karte eine SpellCard ist, berücksichtige Element-Effektivität
        if (attacker instanceof SpellCard || defender instanceof SpellCard) {
            String attackerElement = getElementType(attacker);
            String defenderElement = getElementType(defender);
            double multiplier = getEffectivenessMultiplier(attackerElement, defenderElement);
            damage *= multiplier;
        }

        return damage;
    }

    private String getElementType(Card card) {
        if (card instanceof MonsterCard) {
            return ((MonsterCard) card).getElementType();
        } else if (card instanceof SpellCard) {
            return ((SpellCard) card).getElementType();
        }
        return "normal";
    }

    private double getEffectivenessMultiplier(String attackerElement, String defenderElement) {
        if (attackerElement.equals("water") && defenderElement.equals("fire")) {
            return 2.0;
        } else if (attackerElement.equals("fire") && defenderElement.equals("water")) {
            return 0.5;
        } else if (attackerElement.equals("fire") && defenderElement.equals("normal")) {
            return 2.0;
        } else if (attackerElement.equals("normal") && defenderElement.equals("fire")) {
            return 0.5;
        } else if (attackerElement.equals("normal") && defenderElement.equals("water")) {
            return 2.0;
        } else if (attackerElement.equals("water") && defenderElement.equals("normal")) {
            return 0.5;
        } else {
            return 1.0;
        }
    }

    private boolean applySpecialRules(Card card1, Card card2) {
        String name1 = card1.getName().toLowerCase();
        String name2 = card2.getName().toLowerCase();

        // Goblins sind zu ängstlich, um gegen Drachen zu kämpfen
        if (name1.contains("goblin") && name2.contains("dragon")) {
            battleLog.append("Goblins are too afraid of Dragons to attack.\n");
            battleLog.append(player2.getUsername()).append(" wins the round!\n\n");
            player2.addCard(card1);
            player1.removeCard(card1);
            return true;
        } else if (name2.contains("goblin") && name1.contains("dragon")) {
            battleLog.append("Goblins are too afraid of Dragons to attack.\n");
            battleLog.append(player1.getUsername()).append(" wins the round!\n\n");
            player1.addCard(card2);
            player2.removeCard(card2);
            return true;
        }

        // Wizzards können Orks kontrollieren
        if (name1.contains("wizzard") && name2.contains("ork")) {
            battleLog.append("Wizzards can control Orks.\n");
            battleLog.append(player1.getUsername()).append(" wins the round!\n\n");
            player1.addCard(card2);
            player2.removeCard(card2);
            return true;
        } else if (name2.contains("wizzard") && name1.contains("ork")) {
            battleLog.append("Wizzards can control Orks.\n");
            battleLog.append(player2.getUsername()).append(" wins the round!\n\n");
            player2.addCard(card1);
            player1.removeCard(card1);
            return true;
        }

        // Knights ertrinken, wenn sie von WaterSpells angegriffen werden
        if (name1.contains("waterspell") && name2.contains("knight")) {
            battleLog.append("Knights drown when attacked by WaterSpells.\n");
            battleLog.append(player1.getUsername()).append(" wins the round!\n\n");
            player1.addCard(card2);
            player2.removeCard(card2);
            return true;
        } else if (name2.contains("waterspell") && name1.contains("knight")) {
            battleLog.append("Knights drown when attacked by WaterSpells.\n");
            battleLog.append(player2.getUsername()).append(" wins the round!\n\n");
            player2.addCard(card1);
            player1.removeCard(card1);
            return true;
        }

        // Der Kraken ist immun gegen Zauber
        if (card1 instanceof SpellCard && name2.contains("kraken")) {
            battleLog.append("The Kraken is immune against spells.\n");
            battleLog.append(player2.getUsername()).append(" wins the round!\n\n");
            player2.addCard(card1);
            player1.removeCard(card1);
            return true;
        } else if (card2 instanceof SpellCard && name1.contains("kraken")) {
            battleLog.append("The Kraken is immune against spells.\n");
            battleLog.append(player1.getUsername()).append(" wins the round!\n\n");
            player1.addCard(card2);
            player2.removeCard(card2);
            return true;
        }

        // FireElves können Drachen ausweichen
        if (name1.contains("fireelf") && name2.contains("dragon")) {
            battleLog.append("FireElves can evade Dragons.\n");
            battleLog.append("It's a draw!\n\n");
            return true;
        } else if (name2.contains("fireelf") && name1.contains("dragon")) {
            battleLog.append("FireElves can evade Dragons.\n");
            battleLog.append("It's a draw!\n\n");
            return true;
        }

        return false;
    }

    private void updateElo(String winner, String loser, boolean isWin) {
        if (isWin) {
            UserDatabase.updateUserElo(winner, 3);
            UserDatabase.updateUserElo(loser, -5);
        }
        // Anzahl der gespielten Spiele aktualisieren
        UserDatabase.incrementGamesPlayed(winner);
        UserDatabase.incrementGamesPlayed(loser);
    }
}
