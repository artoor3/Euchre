package engine_clean.model;

public class Card {

    private final Suit suit;
    private final Rank rank;

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

    public boolean isRightBower(Suit trump) {
        return rank == Rank.JACK && suit == trump;
    }

    public boolean isLeftBower(Suit trump) {
        return rank == Rank.JACK &&
                suit != trump &&
                suit.sameColor(trump);
    }

    public Suit getEffectiveSuit(Suit trump) {
        if (isLeftBower(trump)) {
            return trump;
        }
        return suit;
    }
}
