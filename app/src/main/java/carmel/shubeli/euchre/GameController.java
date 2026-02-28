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
    public int getCallerTeam() {
        int caller = engine.getTrumpCaller();
        if (caller < 0) return 0;
        return engine.getPlayers().get(caller).getTeamId();
    }
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
        // snapshot BEFORE scoring
        int[] before = engine.getTeamScores();
        int beforeUs = before[0], beforeThem = before[1];

        int tricksUs = engine.getTricksTeam0();
        int tricksThem = engine.getTricksTeam1();
        Suit trump = engine.getTrumpSuit();

        // do scoring + start next round
        engine.scoreHandAndStartNextRound();

        // snapshot AFTER scoring
        int[] after = engine.getTeamScores();
        int afterUs = after[0], afterThem = after[1];

        int gainedUs = afterUs - beforeUs;
        int gainedThem = afterThem - beforeThem;

        lastHandSummary = new HandSummary(
                tricksUs, tricksThem,
                gainedUs, gainedThem,
                afterUs, afterThem,
                trump
        );

        handCounter++;
    }
    public int getDealerIndex() {
        return engine.getDealerIndex();
    }
    public int[] getTeamScores() {
        return engine.getTeamScores();
    }

    public int getTricksTeam0() {
        return engine.getTeamTricks()[0];
    }

    public int getTricksTeam1() {
        return engine.getTeamTricks()[1];
    }

    public Suit getTrumpSuit() {
        return engine.getTrumpSuit();
    }
    // ---------- Legal indexes (UI help only) ----------
    public List<Integer> computeLegalIndexes(int playerIndex) {//מחשב איזה קלפים הם חוקיים לשחק
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
    public static class HandSummary {
        public final int tricksUs;
        public final int tricksThem;
        public final int gainedUs;
        public final int gainedThem;
        public final int totalUs;
        public final int totalThem;
        public final Suit trump;

        public HandSummary(int tricksUs, int tricksThem, int gainedUs, int gainedThem,
                           int totalUs, int totalThem, Suit trump) {
            this.tricksUs = tricksUs;
            this.tricksThem = tricksThem;
            this.gainedUs = gainedUs;
            this.gainedThem = gainedThem;
            this.totalUs = totalUs;
            this.totalThem = totalThem;
            this.trump = trump;
        }
    }

    private HandSummary lastHandSummary = null;

    public HandSummary consumeLastHandSummary() {
        HandSummary h = lastHandSummary;
        lastHandSummary = null;
        return h;
    }
}