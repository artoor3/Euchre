package carmel.shubeli.euchre.game;

public class GameEngine {

    private final Player[] players = new Player[4];
    private Deck deck;
    private int dealerIndex = -1;
    private Suit trumpSuit;
    private Card trumpCard;
    private int currentPlayer;

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

        // Set dealer
        if (dealerIndex == -1) {
            dealerIndex = (int) (Math.random() * 4); // first dealer random
        } else {
            dealerIndex = (dealerIndex + 1) % 4; // rotate dealer
        }

        // Deal cards starting left of dealer
        int current = (dealerIndex + 1) % 4;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                players[current].addCard(deck.draw());
                current = (current + 1) % 4;
            }
        }

        // Flip trump card
        trumpCard = deck.draw();
        trumpSuit = trumpCard.getSuit();
        // First trick starts left of dealer
        currentPlayer = (dealerIndex + 1) % 4;

    }

    public Player[] getPlayers() {
        return players;
    }
    public int getDealerIndex() {
        return dealerIndex;
    }

    public Suit getTrumpSuit() {
        return trumpSuit;
    }

    public Card getTrumpCard() {
        return trumpCard;
    }
    public boolean isRightBower(Card card) {
        return card.getRank() == Rank.JACK &&
                card.getSuit() == trumpSuit;
    }

    public boolean isLeftBower(Card card) {
        return card.getRank() == Rank.JACK &&
                card.getSuit().isRed() == trumpSuit.isRed() &&
                card.getSuit() != trumpSuit;
    }

    public boolean isTrump(Card card) {
        return card.getSuit() == trumpSuit || isLeftBower(card);
    }
    public boolean beats(Card a, Card b, Suit leadSuit) {

        // Right Bower beats everything
        if (isRightBower(a)) return !isRightBower(b);
        if (isRightBower(b)) return false;

        // Left Bower beats everything except Right Bower
        if (isLeftBower(a)) return !isLeftBower(b);
        if (isLeftBower(b)) return false;

        boolean aTrump = isTrump(a);
        boolean bTrump = isTrump(b);

        // Trump beats non-trump
        if (aTrump && !bTrump) return true;
        if (!aTrump && bTrump) return false;

        // Both trump → compare rank
        if (aTrump && bTrump) {
            return a.getRank().ordinal() > b.getRank().ordinal();
        }

        // Follow suit beats non-follow suit
        boolean aFollow = a.getSuit() == leadSuit;
        boolean bFollow = b.getSuit() == leadSuit;

        if (aFollow && !bFollow) return true;
        if (!aFollow && bFollow) return false;

        // Same suit → compare rank
        if (a.getSuit() == b.getSuit()) {
            return a.getRank().ordinal() > b.getRank().ordinal();
        }

        return false;
    }
    public int determineTrickWinner(Card[] trickCards, int leadPlayer) {

        int winningPlayer = leadPlayer;
        Card winningCard = trickCards[leadPlayer];
        Suit leadSuit = winningCard.getSuit();

        for (int i = 1; i < 4; i++) {
            int currentPlayer = (leadPlayer + i) % 4;
            Card currentCard = trickCards[currentPlayer];

            if (beats(currentCard, winningCard, leadSuit)) {
                winningCard = currentCard;
                winningPlayer = currentPlayer;
            }
        }

        return winningPlayer;
    }
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(int player) {
        currentPlayer = player;
    }
    public Card playCard(int playerIndex, int cardIndex) {
        Player player = players[playerIndex];
        Card played = player.getHand().remove(cardIndex);
        return played;
    }

}
