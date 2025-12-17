package carmel.shubeli.euchre.game;

public class Card {

    private final Suit suit;
    private final Rank rank;

    // 👇 THIS IS THE MISSING PIECE
    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public Suit getSuit() {
        return suit;
    }

    public Rank getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return rank + " of " + suit;
    }
}
