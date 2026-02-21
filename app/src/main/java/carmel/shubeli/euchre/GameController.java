package carmel.shubeli.euchre;

import java.util.ArrayList;
import java.util.List;

import engine_clean.core.EuchreEngine;
import engine_clean.core.GamePhase;
import engine_clean.model.Card;
import engine_clean.model.Suit;

public class GameController {

    private final EuchreEngine engine;
    private static final int HUMAN_INDEX = 0;
    private int handCounter = 0;

    public int getHandNumberMarker() { return handCounter; }

    public GameController() {
        engine = new EuchreEngine();
        engine.startNewRound();
    }

    // ---------- Read-only state ----------
    public GamePhase getPhase() { return engine.getPhase(); }

    public boolean isHumanTurn() { return engine.getCurrentPlayerIndex() == HUMAN_INDEX; }

    public int getCurrentPlayerIndex() { return engine.getCurrentPlayerIndex(); }

    public Card[] getCurrentTrick() { return engine.getCurrentTrick(); }

    public List<Card> getHumanHand() { return engine.getHand(HUMAN_INDEX); }

    public int getHandSize(int playerIndex) { return engine.getHand(playerIndex).size(); }

    public Card getUpCard() { return engine.getUpCard(); }

    public List<Suit> getSelectableTrumpSuitsRound2() {
        List<Suit> suits = new ArrayList<>();
        Suit forbidden = engine.getUpCard().getSuit();
        for (Suit s : Suit.values()) {
            if (s != forbidden) suits.add(s);
        }
        return suits;
    }

    public List<Integer> getLegalCardIndexesForHuman() {
        return computeLegalIndexes(HUMAN_INDEX);
    }

    // ---------- Actions ----------
    public void pass() { engine.pass(); }

    public void orderUp(Suit suit) { engine.orderUp(suit); }

    public void discard(int cardIndex) { engine.discard(cardIndex); }

    public void playHumanCard(int cardIndex) { engine.playCard(HUMAN_INDEX, cardIndex); }

    // לצורך AUTO (שחקנים אחרים)
    public void playCardAsPlayer(int playerIndex, int cardIndex) { engine.playCard(playerIndex, cardIndex); }

    public void continueAfterScoring() {
        engine.scoreHandAndStartNextRound();
        handCounter++;
    }
    // ---------- Legal indexes (UI help only) ----------
    public List<Integer> computeLegalIndexes(int playerIndex) {
        List<Integer> legal = new ArrayList<>();

        if (engine.getPhase() != GamePhase.PLAYING_TRICK) return legal;
        if (engine.getCurrentPlayerIndex() != playerIndex) return legal;

        List<Card> hand = engine.getHand(playerIndex);

        if (engine.getCardsPlayedInTrick() == 0) {
            for (int i = 0; i < hand.size(); i++) legal.add(i);
            return legal;
        }

        boolean hasLedSuit = false;
        for (Card c : hand) {
            if (c.getEffectiveSuit(engine.getTrumpSuit()) == engine.getLedSuit()) {
                hasLedSuit = true;
                break;
            }
        }

        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.get(i);
            if (!hasLedSuit) legal.add(i);
            else if (c.getEffectiveSuit(engine.getTrumpSuit()) == engine.getLedSuit()) legal.add(i);
        }

        return legal;
    }
}