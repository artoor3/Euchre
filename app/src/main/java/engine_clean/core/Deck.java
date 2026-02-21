package engine_clean.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import engine_clean.model.Card;
import engine_clean.model.Rank;
import engine_clean.model.Suit;

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
        if (cards.isEmpty()) {
            throw new IllegalStateException("Deck is empty");
        }
        return cards.remove(0);
    }

    public int size() {
        return cards.size();
    }
}
