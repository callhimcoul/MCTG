package at.technikum_wien;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BattleHandler {
    private static final BlockingQueue<Player> queue = new LinkedBlockingQueue<>();

    public static void enqueuePlayer(Player player) {
        synchronized (queue) {
            queue.add(player);
            if (queue.size() >= 2) {
                Player player1 = queue.poll();
                Player player2 = queue.poll();
                if (player1 != null && player2 != null) {
                    Battle battle = new Battle(player1, player2);
                    new Thread(() -> {
                        try {
                            battle.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            } else {
                // Player must wait until an opponent is available
                // This can be implemented with a wait/notify mechanism if needed
            }
        }
    }


    private static void initiateBattle() {
        Player player1 = queue.poll();
        Player player2 = queue.poll();

        if (player1 != null && player2 != null) {
            Battle battle = new Battle(player1, player2);
            new Thread(() -> {
                try {
                    battle.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
