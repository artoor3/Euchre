package carmel.shubeli.euchre.game;

public class GameEngine {

    private final Player[] players = new Player[4];
    private Deck deck;

    public GameEngine() {
        for (int i = 0; i < 4; i++) {
            players[i] = new Player(i);
        }
    }

    public void startNewRound() {
        deck = new Deck();
        deck.shuffle();

        for (Player p : players) {
            p.clearHand();
        }

        // Deal 5 cards to each player
        for (int i = 0; i < 5; i++) {
            for (Player p : players) {
                p.addCard(deck.draw());
            }
        }
    }

    public Player[] getPlayers() {
        return players;
    }
}
