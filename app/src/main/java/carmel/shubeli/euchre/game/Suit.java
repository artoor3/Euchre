package carmel.shubeli.euchre.game;

public enum Suit {
    HEARTS(true),
    DIAMONDS(true),
    CLUBS(false),
    SPADES(false);

    private final boolean red;

    Suit(boolean red) {
        this.red = red;
    }

    public boolean isRed() {
        return red;
    }
}
