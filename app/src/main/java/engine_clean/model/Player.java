package engine_clean.model;

import java.util.ArrayList;
import java.util.List;

public class Player {

    private final int id;
    private final int teamId;
    private final boolean isHuman;

    private final List<Card> hand = new ArrayList<>();

    public Player(int id, int teamId, boolean isHuman) {
        this.id = id;
        this.teamId = teamId;
        this.isHuman = isHuman;
    }

    public int getId() {
        return id;
    }

    public int getTeamId() {
        return teamId;
    }

    public boolean isHuman() {
        return isHuman;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public void removeCard(Card card) {
        hand.remove(card);
    }

    public void clearHand() {
        hand.clear();
    }
}
