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

            double damage1 = calculateDamage(card1, card2, player1);
            double damage2 = calculateDamage(card2, card1, player2);

            boolean specialRuleApplied = applySpecialRules(card1, card2);
            if (!specialRuleApplied) {
                if (damage1 > damage2) {
                    battleLog.append(player1.getUsername()).append(" wins the round!\n\n");
                    // Remove from loser first, then add to winner to avoid duplicates
                    player2.removeCard(card2);
                    player1.addCard(card2);
                } else if (damage2 > damage1) {
                    battleLog.append(player2.getUsername()).append(" wins the round!\n\n");
                    player1.removeCard(card1);
                    player2.addCard(card1);
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

        player1.sendBattleResult(battleLog.toString());
        player2.sendBattleResult(battleLog.toString());

        synchronized (player1.getBattleLock()) {
            player1.getBattleLock().notifyAll();
        }
        synchronized (player2.getBattleLock()) {
            player2.getBattleLock().notifyAll();
        }
    }

    private double calculateDamage(Card attacker, Card defender, Player attackingPlayer) {
        double damage = attacker.getDamage();

        // Check booster
        if (attackingPlayer.isBoosterCard(attacker)) {
            damage *= 2;
            battleLog.append(attackingPlayer.getUsername()).append("'s booster card effect activates! Damage doubled.\n");
            attackingPlayer.markBoosterUsed();
        }

        // Element effectiveness if spell involved
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

    public double getEffectivenessMultiplier(String attackerElement, String defenderElement) {
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

    /**
     * Apply special rules. If a rule applies, the card transfer happens here. Ensure remove first, then add.
     */
    public boolean applySpecialRules(Card card1, Card card2) {
        String name1 = card1.getName().toLowerCase();
        String name2 = card2.getName().toLowerCase();

        // Goblins and Dragons
        if (name1.contains("goblin") && name2.contains("dragon")) {
            battleLog.append("Goblins are too afraid of Dragons to attack.\n");
            battleLog.append(player2.getUsername()).append(" wins the round!\n\n");
            player1.removeCard(card1);
            player2.addCard(card1);
            return true;
        } else if (name2.contains("goblin") && name1.contains("dragon")) {
            battleLog.append("Goblins are too afraid of Dragons to attack.\n");
            battleLog.append(player1.getUsername()).append(" wins the round!\n\n");
            player2.removeCard(card2);
            player1.addCard(card2);
            return true;
        }

        // Wizzard and Orks
        if (name1.contains("wizzard") && name2.contains("ork")) {
            battleLog.append("Wizzard can control Orks.\n");
            battleLog.append(player1.getUsername()).append(" wins the round!\n\n");
            player2.removeCard(card2);
            player1.addCard(card2);
            return true;
        } else if (name2.contains("wizzard") && name1.contains("ork")) {
            battleLog.append("Wizzard can control Orks.\n");
            battleLog.append(player2.getUsername()).append(" wins the round!\n\n");
            player1.removeCard(card1);
            player2.addCard(card1);
            return true;
        }

        // Knights and WaterSpells
        if (name1.contains("waterspell") && name2.contains("knight")) {
            battleLog.append("Knights drown when attacked by WaterSpells.\n");
            battleLog.append(player1.getUsername()).append(" wins the round!\n\n");
            player2.removeCard(card2);
            player1.addCard(card2);
            return true;
        } else if (name2.contains("waterspell") && name1.contains("knight")) {
            battleLog.append("Knights drown when attacked by WaterSpells.\n");
            battleLog.append(player2.getUsername()).append(" wins the round!\n\n");
            player1.removeCard(card1);
            player2.addCard(card1);
            return true;
        }

        // Kraken immune to spells
        if ((card1 instanceof SpellCard) && name2.contains("kraken")) {
            battleLog.append("The Kraken is immune against spells.\n");
            battleLog.append(player2.getUsername()).append(" wins the round!\n\n");
            player1.removeCard(card1);
            player2.addCard(card1);
            return true;
        } else if ((card2 instanceof SpellCard) && name1.contains("kraken")) {
            battleLog.append("The Kraken is immune against spells.\n");
            battleLog.append(player1.getUsername()).append(" wins the round!\n\n");
            player2.removeCard(card2);
            player1.addCard(card2);
            return true;
        }

        // FireElves evade Dragons
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
        UserDatabase.incrementGamesPlayed(winner);
        UserDatabase.incrementGamesPlayed(loser);
    }
}
