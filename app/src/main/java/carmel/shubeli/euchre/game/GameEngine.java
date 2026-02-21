package carmel.shubeli.euchre.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameEngine {
    private Player[] players;
    private int dealerIndex = -1;
    private Card trumpCard;
    private Suit trumpSuit;
    private int trumpCaller;
    private int[] teamScores = new int[2];
    private boolean trumpChosen = false;
    private GamePhase phase;
    private int currentPlayer;

    public enum GamePhase {
        DEALING,
        CHOOSE_TRUMP,
        DISCARD,
        PLAY_TRICK,
        ROUND_END
    }

    public GameEngine() {
        // Initialize players (assuming Player has a no-argument constructor)
        players = new Player[4];
        for (int i = 0; i < 4; i++) {
            players[i] = new Player(i);
        }
        // Team scores start at 0 (already default for int[])
        teamScores[0] = 0;
        teamScores[1] = 0;
    }

    /** Start a new round: shuffle and deal cards, choose (or rotate) dealer, and set the turned-up trump card. */
    public void startNewRound() {
        // Determine dealer (random for first round, then rotate for subsequent rounds)
        if (dealerIndex == -1) {
            dealerIndex = new Random().nextInt(4);  // initial dealer
        } else {
            dealerIndex = (dealerIndex + 1) % 4;    // rotate dealer
        }

        // Clear hands from previous round
        for (Player p : players) {
            p.getHand().clear();
        }

        // Build Euchre deck (9, 10, J, Q, K, A of each suit)
        List<Card> deck = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                // Assume Card constructor takes Rank and Suit
                deck.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(deck, new Random());

        int[] dealPattern = {2, 3};

        for (int round = 0; round < 2; round++) {
            for (int i = 1; i <= 4; i++) {
                int player = (dealerIndex + i) % 4;
                for (int c = 0; c < dealPattern[round]; c++) {
                    players[player].getHand().add(deck.remove(0));
                }
            }
        }


        // Turn up the next card as the potential trump card
        trumpCard = deck.remove(0);
        trumpSuit = null;
        trumpCaller = -1;
        trumpChosen = false;

        phase = GamePhase.CHOOSE_TRUMP;
        currentPlayer = (dealerIndex + 1) % 4; // השחקן שמשמאל לדילר
    }

    /** Get the array of players. */
    public Player[] getPlayers() {
        return players;
    }

    /** Get the index of the current dealer (0-3). */
    public int getDealerIndex() {
        return dealerIndex;
    }

    /** Get the turned-up trump Card for the current round. */
    public Card getTrumpCard() {
        return trumpCard;
    }

    /** Get the Suit chosen as trump for this round (null if not decided yet). */
    public Suit getTrumpSuit() {
        return trumpSuit;
    }
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public GamePhase getPhase() {
        return phase;
    }
    public void advanceTurn() {
        currentPlayer = (currentPlayer + 1) % 4;
    }

    /** Get the index of the player who called trump this round. */
    public int getTrumpCaller() {
        return trumpCaller;
    }

    /** Declare the turned-up card's suit as trump (first round of bidding). */
    public void orderUpTrump(int playerIndex) {
        trumpSuit = trumpCard.getSuit();
        trumpCaller = playerIndex;
    }

    /** Have the dealer pick up the trump card into their hand (after someone orders up trump). */
    public void dealerPickupTrump() {
        // Dealer adds the turned-up trump card to their hand (hand now has 6 cards)
        players[dealerIndex].getHand().add(trumpCard);
        // The dealer will discard one card via discardCard() to return to 5 cards
    }

    /** Discard a card from a player's hand (used when the dealer discards after picking up trump). */
    public void discardCard(int playerIndex, int cardIndex) {
        players[playerIndex].getHand().remove(cardIndex);
    }

    /** Play a card from a player's hand (remove it from hand and return the Card played). */
    public Card playCard(int playerIndex, int cardIndex) {
        return players[playerIndex].getHand().remove(cardIndex);
    }

    /** Determine the effective suit of a card, considering trump (Left Bower counts as trump suit). */
    public Suit getEffectiveSuit(Card card) {
        Suit cardSuit = card.getSuit();
        if (trumpSuit == null) {
            // If trump not decided yet, effective suit is the card's own suit
            return cardSuit;
        }
        // Left Bower check: if card is Jack and same color as trump but not trump suit, treat as trump suit
        if (card.getRank() == Rank.JACK && cardSuit != trumpSuit && isSameColor(cardSuit, trumpSuit)) {
            return trumpSuit;
        }
        return cardSuit;
    }

    /** Helper to check if two suits are the same color (hearts/diamonds are red, spades/clubs are black). */
    private boolean isSameColor(Suit s1, Suit s2) {
        if ((s1 == Suit.HEARTS || s1 == Suit.DIAMONDS) && (s2 == Suit.HEARTS || s2 == Suit.DIAMONDS)) {
            return true;
        }
        if ((s1 == Suit.CLUBS || s1 == Suit.SPADES) && (s2 == Suit.CLUBS || s2 == Suit.SPADES)) {
            return true;
        }
        return false;
    }

    /** Determine the winner of a completed trick.
     * @param trick      Array of 4 Cards (indexed by player 0-3) representing the trick.
     * @param leadPlayer Index of the player who led this trick.
     * @return the index of the winning player. */
    public int determineTrickWinner(Card[] trick, int leadPlayer) {
        Suit ledSuitEff = getEffectiveSuit(trick[leadPlayer]);
        int winningPlayer = leadPlayer;
        Card winningCard = trick[leadPlayer];

        // Evaluate each of the other players' cards against the current winner
        for (int p = 0; p < 4; p++) {
            if (p == leadPlayer) continue;
            Card card = trick[p];
            if (card == null) continue;

            Suit cardEffSuit = getEffectiveSuit(card);
            boolean winningIsTrump = getEffectiveSuit(winningCard) == trumpSuit;
            boolean cardIsTrump = cardEffSuit == trumpSuit;

            if (cardIsTrump && !winningIsTrump) {
                // New card is trump, current winner is not trump -> trump wins
                winningCard = card;
                winningPlayer = p;
            } else if (cardIsTrump && winningIsTrump) {
                // Both cards are trump -> compare ranks with bowers considered
                winningPlayer = compareTrumpCards(winningCard, winningPlayer, card, p);
                winningCard = trick[winningPlayer];
            } else if (!cardIsTrump && !winningIsTrump) {
                // Neither card is trump -> must follow led suit to win
                if (cardEffSuit == ledSuitEff && getEffectiveSuit(winningCard) == ledSuitEff) {
                    // Both follow led suit: higher rank wins
                    if (card.getRank().ordinal() > winningCard.getRank().ordinal()) {
                        winningCard = card;
                        winningPlayer = p;
                    }
                } else if (cardEffSuit == ledSuitEff && getEffectiveSuit(winningCard) != ledSuitEff) {
                    // New card follows suit, current winning card did not follow -> new card wins
                    winningCard = card;
                    winningPlayer = p;
                }
                // Otherwise, either both did not follow suit or current winner followed and new didn't -> no change
            }
            // If new card is not trump and current winner is trump, no change (trump still winning)
        }
        return winningPlayer;
    }

    /** Helper to compare two trump cards (including bowers). Returns the winner's player index. */
    private int compareTrumpCards(Card cardA, int playerA, Card cardB, int playerB) {
        // Identify Right Bower (Jack of trump suit)
        boolean aRightBower = (cardA.getRank() == Rank.JACK && cardA.getSuit() == trumpSuit);
        boolean bRightBower = (cardB.getRank() == Rank.JACK && cardB.getSuit() == trumpSuit);
        if (aRightBower && !bRightBower) return playerA;
        if (bRightBower && !aRightBower) return playerB;

        // Identify Left Bower (Jack of the other suit of same color)
        boolean aLeftBower = (cardA.getRank() == Rank.JACK && cardA.getSuit() != trumpSuit && isSameColor(cardA.getSuit(), trumpSuit));
        boolean bLeftBower = (cardB.getRank() == Rank.JACK && cardB.getSuit() != trumpSuit && isSameColor(cardB.getSuit(), trumpSuit));
        if (aLeftBower && !bLeftBower) return playerA;
        if (bLeftBower && !aLeftBower) return playerB;

        // If neither card is a bower (or both are same bower, which cannot happen), compare rank ordinals
        return (cardA.getRank().ordinal() > cardB.getRank().ordinal()) ? playerA : playerB;
    }

    /** Get the current game score for a team (team 0 = Players 0 & 2, team 1 = Players 1 & 3). */
    public int getGameScore(int team) {
        if (team == 0 || team == 1) {
            return teamScores[team];
        }
        return 0;
    }
    public void setTrumpSuit(Suit suit) {
        this.trumpSuit = suit;
    }

    public void setTrumpChosen(boolean chosen) {
        this.trumpChosen = chosen;
    }

    public void setTrumpCaller(int caller) {
        this.trumpCaller = caller;
    }

}
