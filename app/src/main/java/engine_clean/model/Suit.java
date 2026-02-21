package engine_clean.model;

public enum Suit {
    HEARTS,
    DIAMONDS,
    CLUBS,
    SPADES;

    public boolean sameColor(Suit other) {
        if ((this == HEARTS || this == DIAMONDS) &&
                (other == HEARTS || other == DIAMONDS)) return true;

        if ((this == CLUBS || this == SPADES) &&
                (other == CLUBS || other == SPADES)) return true;

        return false;
    }
}
