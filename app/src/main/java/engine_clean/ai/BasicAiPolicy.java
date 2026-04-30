package engine_clean.ai;


import java.util.List;

import engine_clean.model.Card;
import engine_clean.model.Rank;
import engine_clean.model.Suit;
import carmel.shubeli.euchre.GameController;

public class BasicAiPolicy {

    // thresholds you can tune
    private static final int ORDER_R1_THRESHOLD = 16; // "good enough" hand in upcard suit 16
    private static final int ORDER_R2_THRESHOLD = 18; // round2 usually needs a bit more confidence 18

    public enum OrderDecisionType { PASS, ORDER_UP_R1, ORDER_UP_R2 }
    public static class OrderDecision {
        public final OrderDecisionType type;
        public final Suit suit; // only for ORDER_UP_R2
        private OrderDecision(OrderDecisionType type, Suit suit) { this.type = type; this.suit = suit; }
        public static OrderDecision pass() { return new OrderDecision(OrderDecisionType.PASS, null); }
        public static OrderDecision orderR1() { return new OrderDecision(OrderDecisionType.ORDER_UP_R1, null); }
        public static OrderDecision orderR2(Suit suit) { return new OrderDecision(OrderDecisionType.ORDER_UP_R2, suit); }
    }

    // -------- ORDERING --------

    public OrderDecision decideOrdering(GameController c, int aiIndex) {
        switch (c.getPhase()) {
            case ORDERING_TRUMP_ROUND1:
                return decideRound1(c, aiIndex);
            case ORDERING_TRUMP_ROUND2:
                return decideRound2(c, aiIndex);
            default:
                return OrderDecision.pass();
        }
    }

    private OrderDecision decideRound1(GameController c, int aiIndex) {
        Suit trump = c.getUpCard().getSuit();
        int score = scoreHandAsTrump(c.getHandForPlayer(aiIndex), trump);

        // tiny bias: if I'm dealer's partner (team 0 with human, etc.), still only heuristic.
        if (score >= ORDER_R1_THRESHOLD) return OrderDecision.orderR1();
        return OrderDecision.pass();
    }

    private OrderDecision decideRound2(GameController c, int aiIndex) {
        Suit forbidden = c.getUpCard().getSuit();

        Suit bestSuit = null;
        int bestScore = Integer.MIN_VALUE;

        for (Suit s : Suit.values()) {
            if (s == forbidden) continue;
            int score = scoreHandAsTrump(c.getHandForPlayer(aiIndex), s);
            if (score > bestScore) {
                bestScore = score;
                bestSuit = s;
            }
        }

        if (bestSuit != null && bestScore >= ORDER_R2_THRESHOLD) return OrderDecision.orderR2(bestSuit);
        return OrderDecision.pass();
    }

    // -------- DISCARD --------

    // assumes dealer currently has 6 cards and upCard is in hand; controller already prevents discarding upCard object
    public int chooseDiscardIndex(GameController c, int dealerIndex) {
        Suit trump = c.getTrumpSuit();
        List<Card> hand = c.getHandForPlayer(dealerIndex);

        int worstIdx = -1;
        int worstScore = Integer.MAX_VALUE;

        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);

            // avoid discarding the upcard object (controller guard uses identity; still keep safe here)
            if (card == c.getUpCard()) continue;

            int val = cardStrengthForTrick(card, trump, null); // no led suit, just absolute-ish value
            if (val < worstScore) {
                worstScore = val;
                worstIdx = i;
            }
        }

        // fallback
        if (worstIdx < 0) worstIdx = 0;
        return worstIdx;
    }

    // -------- PLAYING --------

    public int choosePlayIndex(GameController c, int aiIndex) {
        List<Integer> legal = c.getLegalCardIndexesForPlayer(aiIndex);
        if (legal.isEmpty()) return 0;

        Suit trump = c.getTrumpSuit();
        Suit ledSuit = c.getLedSuit();
        Card[] trick = c.getCurrentTrick();

        // if leading (no cards yet) -> choose a "good lead"
        if (c.getCardsPlayedInTrick() == 0) {
            return chooseLeadCardIndex(c.getHandForPlayer(aiIndex), legal, trump);
        }

        // following -> try to win cheaply if possible
        int bestWinIdx = -1;
        int bestWinCost = Integer.MAX_VALUE;

        int bestLoseIdx = -1;
        int bestLoseCost = Integer.MAX_VALUE;

        // compute current best strength on table
        int currentBest = -1;
        for (Card onTable : trick) {
            if (onTable == null) continue;
            int s = cardStrengthForTrick(onTable, trump, ledSuit);
            if (s > currentBest) currentBest = s;
        }

        List<Card> hand = c.getHandForPlayer(aiIndex);

        for (int idx : legal) {
            Card cand = hand.get(idx);
            int s = cardStrengthForTrick(cand, trump, ledSuit);

            // "cost" prefers smaller winning card (win cheaply), or smallest losing dump

            if (s > currentBest) {
                if (s < bestWinCost) {
                    bestWinCost = s;
                    bestWinIdx = idx;
                }
            } else {
                if (s < bestLoseCost) {
                    bestLoseCost = s;
                    bestLoseIdx = idx;
                }
            }
        }

        if (bestWinIdx != -1) return bestWinIdx;
        return (bestLoseIdx != -1) ? bestLoseIdx : legal.get(0);
    }

    private int chooseLeadCardIndex(List<Card> hand, List<Integer> legal, Suit trump) {
        // simple: lead highest non-trump if possible; otherwise lead lowest trump
        int bestNonTrumpIdx = -1;
        int bestNonTrumpStrength = -1;

        int lowestTrumpIdx = -1;
        int lowestTrumpStrength = Integer.MAX_VALUE;

        for (int idx : legal) {
            Card c = hand.get(idx);
            Suit eff = c.getEffectiveSuit(trump);
            int s = cardStrengthForTrick(c, trump, eff); // ledSuit = eff when leading

            if (eff == trump) {
                if (s < lowestTrumpStrength) {
                    lowestTrumpStrength = s;
                    lowestTrumpIdx = idx;
                }
            } else {
                if (s > bestNonTrumpStrength) {
                    bestNonTrumpStrength = s;
                    bestNonTrumpIdx = idx;
                }
            }
        }

        if (bestNonTrumpIdx != -1) return bestNonTrumpIdx;
        if (lowestTrumpIdx != -1) return lowestTrumpIdx;
        return legal.get(0);
    }

    // -------- scoring helper used by ordering --------

    private int scoreHandAsTrump(List<Card> hand, Suit trump) {
        int total = 0;
        for (Card c : hand) total += orderValue(c, trump);
        return total;
    }

    private int orderValue(Card c, Suit trump) {
        // coarse heuristic for ordering strength
        if (c.isRightBower(trump)) return 10;
        if (c.isLeftBower(trump)) return 9;

        Suit eff = c.getEffectiveSuit(trump);
        if (eff == trump) {
            switch (c.getRank()) {
                case ACE: return 8;
                case KING: return 6;
                case QUEEN: return 5;
                case TEN: return 3;
                case NINE: return 2;
                default: return 0; // JACK handled above
            }
        } else {
            switch (c.getRank()) {
                case ACE: return 4;
                case KING: return 3;
                case QUEEN: return 2;
                case JACK: return 2; // non-trump jack is not huge, but not nothing
                case TEN: return 1;
                default: return 0;
            }
        }
    }

    // -------- trick strength compatible with your engine idea --------

    private int cardStrengthForTrick(Card c, Suit trump, Suit ledSuit) {
        if (c == null) return -1;

        // Right bower
        if (c.getRank() == Rank.JACK && c.getSuit() == trump) return 1000;

        // Left bower
        if (c.getRank() == Rank.JACK && c.getSuit() != trump && c.getSuit().sameColor(trump)) return 900;

        Suit eff = c.getEffectiveSuit(trump);

        // trump
        if (eff == trump) return 800 + trumpValue(c.getRank());

        // led suit
        if (ledSuit != null && eff == ledSuit) return 400 + nonTrumpValue(c.getRank());

        // off-suit
        return nonTrumpValue(c.getRank());
    }

    private int nonTrumpValue(Rank r) {
        switch (r) {
            case ACE: return 6;
            case KING: return 5;
            case QUEEN: return 4;
            case JACK: return 3;
            case TEN: return 2;
            case NINE: return 1;
            default: return 0;
        }
    }

    private int trumpValue(Rank r) {
        // bowers already handled
        switch (r) {
            case ACE: return 6;
            case KING: return 5;
            case QUEEN: return 4;
            case TEN: return 3;
            case NINE: return 2;
            default: return 0;
        }
    }
}