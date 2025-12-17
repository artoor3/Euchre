package carmel.shubeli.euchre.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {

    private final List<Card> cards = new ArrayList<>();

    public Deck() {
        for (Suit suit : Suit.values()) {
            cards.add(new Card(suit, Rank.NINE));
            cards.add(new Card(suit, Rank.TEN));
            cards.add(new Card(suit, Rank.JACK));
            cards.add(new Card(suit, Rank.QUEEN));
            cards.add(new Card(suit, Rank.KING));
            cards.add(new Card(suit, Rank.ACE));
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card draw() {
        return cards.remove(0);
    }
}
