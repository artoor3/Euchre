package carmel.shubeli.euchre.game;

import java.util.ArrayList;
import java.util.List;

public class Player {

    private final int id;
    private final List<Card> hand = new ArrayList<>();

    public Player(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public void clearHand() {
        hand.clear();
    }
}
