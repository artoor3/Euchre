package engine_clean.core;

import java.util.ArrayList;
import java.util.List;

import engine_clean.model.Player;
import engine_clean.model.Suit;
import engine_clean.model.Card;
import java.util.Collections;
import engine_clean.model.Rank;
public class EuchreEngine {
    private Deck deck;
    private Card upCard; //הקלף שנהפך בתחילת הסיבוב
    private final List<Player> players = new ArrayList<>();

    private GamePhase phase = GamePhase.NOT_STARTED;

    private Suit trumpSuit = null; // הסוג של הקלף בחזק

    private int dealerIndex = 0;
    private int currentPlayerIndex = 0;

    private final int[] teamScores = new int[]{0, 0};
    private int passesInRow = 0;
    private int trumpCaller = -1;
    private Card[] currentTrick = new Card[4];
    private Suit ledSuit = null;
    private int cardsPlayedInTrick = 0;
    private int leadPlayerIndex = -1;
    private int tricksTeam0 = 0;
    private int tricksTeam1 = 0;
    private int tricksPlayedThisHand = 0;
    private int[] teamTricks = new int[]{0, 0};


    public int getTricksTeam0() { return tricksTeam0; }
    public int getTricksTeam1() { return tricksTeam1; }
    public int getTricksPlayedThisHand() { return tricksPlayedThisHand; }
    public Suit getLedSuit() { return ledSuit; }
    public int getCardsPlayedInTrick() { return cardsPlayedInTrick; }
    public int getTrumpCaller() { return trumpCaller; }

    public GamePhase getPhase() {
        return phase;
    }

    public Suit getTrumpSuit() {
        return trumpSuit;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public int[] getTeamScores() {
        return teamScores;
    }
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
    public int[] getTeamTricks() {
        teamTricks[0] = tricksTeam0;
        teamTricks[1] = tricksTeam1;
        return teamTricks;
    }
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public int getDealerIndex() {
        return dealerIndex;
    }
    public Card getUpCard() {
        return upCard;
    }
    public List<Card> getHand(int playerIndex) {
        return Collections.unmodifiableList(players.get(playerIndex).getHand());
    }

    public Card[] getCurrentTrick() {
        return currentTrick.clone();
    }

    public EuchreEngine() {
        initializePlayers();
    }

    private void initializePlayers() {
        // Player 0 - Human
        players.add(new Player(0, 0, true));

        // Player 1 - AI
        players.add(new Player(1, 1, false));

        // Player 2 - AI (partner of player 0)
        players.add(new Player(2, 0, false));

        // Player 3 - AI
        players.add(new Player(3, 1, false));
    }


    public void startNewRound() {
        tricksTeam0 = 0;
        tricksTeam1 = 0;
        tricksPlayedThisHand = 0;
        passesInRow = 0;
        trumpSuit = null;

        if (phase != GamePhase.NOT_STARTED &&
                phase != GamePhase.SCORING) {
            throw new IllegalStateException("Cannot start new round in phase: " + phase);
        }

        phase = GamePhase.DEALING;

        clearHands();

        deck = new Deck();
        deck.shuffle();

        dealCards();

        upCard = deck.draw();

        phase = GamePhase.ORDERING_TRUMP_ROUND1;

        currentPlayerIndex = (dealerIndex + 1) % 4;
    }
    private void clearHands() {
        for (Player p : players) {
            p.clearHand();
        }
    }

    public void pass() {
        if (phase != GamePhase.ORDERING_TRUMP_ROUND1 &&
                phase != GamePhase.ORDERING_TRUMP_ROUND2) {
            throw new IllegalStateException("Pass not allowed in phase: " + phase);
        }

        passesInRow++;

        if (phase == GamePhase.ORDERING_TRUMP_ROUND1) {
            if (passesInRow == 4) {
                // move to round 2
                phase = GamePhase.ORDERING_TRUMP_ROUND2;
                passesInRow = 0;
                currentPlayerIndex = (dealerIndex + 1) % 4; // reset to left of dealer
            }
            else {
                advancePlayer();
            }
            return;
        }

        // phase == ORDERING_TRUMP_ROUND2
        if (passesInRow == 4) {
            // redeal
            phase = GamePhase.SCORING; // so startNewRound is allowed by your guard
            startNewRound();
        } else {
            advancePlayer();
        }
    }


    public void orderUp(Suit suit) {
        if (phase != GamePhase.ORDERING_TRUMP_ROUND1 &&
                phase != GamePhase.ORDERING_TRUMP_ROUND2) {
            throw new IllegalStateException("Cannot order up in phase: " + phase);
        }

        if (phase == GamePhase.ORDERING_TRUMP_ROUND1) {
            trumpSuit = upCard.getSuit();
        }
        else {
            // ROUND2: cannot choose the upCard suit
            if (suit == upCard.getSuit()) {
                throw new IllegalArgumentException("Cannot choose forbidden suit in round 2: " + suit);
            }
            trumpSuit = suit;
        }

        trumpCaller = currentPlayerIndex;
        passesInRow = 0;

        // Dealer picks up only if round 1
        if (phase == GamePhase.ORDERING_TRUMP_ROUND1) {
            players.get(dealerIndex).addCard(upCard); // dealer now has 6
            phase = GamePhase.DISCARDING;
            currentPlayerIndex = dealerIndex; // dealer must discard
        } else {
            // round 2: no pickup, go directly to playing trick later
            phase = GamePhase.PLAYING_TRICK; // נעדכן אחרי שנבנה טריקים, אבל זה placeholder
            currentPlayerIndex = (dealerIndex + 1) % 4; // lead starts left of dealer
        }
    }


    private void advancePlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % 4;
    }
    private void dealCards() {

        for (int i = 0; i < 5; i++) {
            for (Player player : players) {
                player.addCard(deck.draw());
            }
        }
    }
    public void discard(int cardIndex) {
        if (phase != GamePhase.DISCARDING) {
            throw new IllegalStateException("Discard not allowed in phase: " + phase);
        }
        if (currentPlayerIndex != dealerIndex) {
            throw new IllegalStateException("Only dealer can discard");
        }

        Player dealer = players.get(dealerIndex);
        if (dealer.getHand().size() != 6) {
            throw new IllegalStateException("Dealer must have 6 cards before discard");
        }
        Card chosen = dealer.getHand().get(cardIndex);
        if (chosen == upCard) {
            throw new IllegalArgumentException("Dealer cannot discard the picked-up upCard");
        }

        dealer.getHand().remove(cardIndex);

        // After discard, start playing (first lead is left of dealer)
        phase = GamePhase.PLAYING_TRICK;
        currentPlayerIndex = (dealerIndex + 1) % 4;
    }
    public Card playCard(int playerIndex, int cardIndex) {
        if (phase != GamePhase.PLAYING_TRICK) {
            throw new IllegalStateException("Play not allowed in phase: " + phase);
        }
        if (playerIndex != currentPlayerIndex) {
            throw new IllegalStateException("Not this player's turn");
        }

        Player p = players.get(playerIndex);
        Card chosen = p.getHand().get(cardIndex);

        // אם זה הקלף הראשון בטריק
        if (cardsPlayedInTrick == 0) {
            leadPlayerIndex = playerIndex;
            ledSuit = chosen.getEffectiveSuit(trumpSuit); // חשוב: effective suit
        }
        else {
            // חייב לעקוב אחרי ledSuit אם אפשר
            boolean hasLedSuit = false;
            for (Card c : p.getHand()) {               // בודק האם יש לשחקן קלף מהסדרה המובילה
                if (c.getEffectiveSuit(trumpSuit) == ledSuit) {
                    hasLedSuit = true;
                    break;
                }
            }
            if (hasLedSuit && chosen.getEffectiveSuit(trumpSuit) != ledSuit) {
                throw new IllegalArgumentException("Must follow suit: " + ledSuit);
            }
        }

        // בפועל לשחק
        Card played = p.getHand().remove(cardIndex);
        currentTrick[playerIndex] = played;
        cardsPlayedInTrick++;

        if (cardsPlayedInTrick == 4) {
            int winner = determineTrickWinner();

            // ✅ מי הקבוצה של המנצח?
            int winnerTeam = players.get(winner).getTeamId(); // אם אין לך getTeamId תגיד לי איך קוראים לזה

            if (winnerTeam == 0) tricksTeam0++;
            else tricksTeam1++;

            tricksPlayedThisHand++;

            resetTrickState();
            currentPlayerIndex = winner; // winner leads next trick

            // ✅ אחרי 5 טריקים - סוף יד
            if (tricksPlayedThisHand == 5) {
                phase = GamePhase.SCORING;
            }

            return played;
        }

        // התור עובר הלאה (עדיין בלי הכרעת טריק)
        currentPlayerIndex = (currentPlayerIndex + 1) % 4;

        return played;
    }
    private int nonTrumpValue(Rank r) {
        if (r == Rank.ACE) return 6;
        if (r == Rank.KING) return 5;
        if (r == Rank.QUEEN) return 4;
        if (r == Rank.JACK) return 3;
        if (r == Rank.TEN) return 2;
        if (r == Rank.NINE) return 1;
        return 0;
    }

    private int trumpValue(Rank r) {
        // JACK לא אמור להגיע לפה (כי bowers מטופלים קודם)
        if (r == Rank.ACE) return 6;
        if (r == Rank.KING) return 5;
        if (r == Rank.QUEEN) return 4;
        if (r == Rank.TEN) return 3;
        if (r == Rank.NINE) return 2;
        return 0;
    }
    private int trickStrength(Card c) {
        if (c == null) return -1;

        // ✅ 1) Right bower הכי חזק
        if (c.getRank() == Rank.JACK && c.getSuit() == trumpSuit) return 1000;

        // ✅ 2) Left bower שני
        if (c.getRank() == Rank.JACK
                && c.getSuit() != trumpSuit
                && c.getSuit().sameColor(trumpSuit)) return 900;

        Suit eff = c.getEffectiveSuit(trumpSuit);

        // ✅ 3) טראמפ (לא bower)
        if (eff == trumpSuit) return 800 + trumpValue(c.getRank());

        // ✅ 4) led suit
        if (eff == ledSuit) return 400 + nonTrumpValue(c.getRank());

        // ✅ 5) off-suit
        return nonTrumpValue(c.getRank());
    }
    private int determineTrickWinner() {
        int winner = leadPlayerIndex;
        int best = trickStrength(currentTrick[winner]);

        for (int i = 0; i < 4; i++) {
            Card c = currentTrick[i];
            if (c == null) continue;

            int s = trickStrength(c);
            if (s > best) {
                best = s;
                winner = i;
            }
        }
        return winner;
    }

    private void resetTrickState() {
        for (int i = 0; i < 4; i++) currentTrick[i] = null;
        ledSuit = null;
        cardsPlayedInTrick = 0;
        leadPlayerIndex = -1;
    }
    public void scoreHandAndStartNextRound() {
        if (phase != GamePhase.SCORING) {
            throw new IllegalStateException("Not in scoring phase: " + phase);
        }
        if (trumpCaller == -1) {
            throw new IllegalStateException("No trump caller set");
        }

        int callerTeam = players.get(trumpCaller).getTeamId();
        int callerTricks = (callerTeam == 0) ? tricksTeam0 : tricksTeam1;
        int otherTeam = (callerTeam == 0) ? 1 : 0;

        if (callerTricks >= 3 && callerTricks <= 4) {
            teamScores[callerTeam] += 1;
        } else if (callerTricks == 5) {
            teamScores[callerTeam] += 2;
        } else { // 0-2 => euchred
            teamScores[otherTeam] += 2;
        }
        if (teamScores[0] >= 10 || teamScores[1] >= 10) {
            phase = GamePhase.GAME_OVER;
            return;
        }
        dealerIndex = (dealerIndex + 1) % 4;
        phase = GamePhase.SCORING; // כדי להיות 100% תואם ל-guard שלך
        startNewRound();
    }
   public void _testSetPhase(GamePhase p) { this.phase = p; }
   public void _testSetTrumpSuit(Suit s) { this.trumpSuit = s; }
    public void _testSetTurn(int p) { this.currentPlayerIndex = p; }

   public void _testResetTrick() {
        for (int i = 0; i < 4; i++) currentTrick[i] = null;
        ledSuit = null;
        cardsPlayedInTrick = 0;
        leadPlayerIndex = -1;
    }

 public    void _testSetHand(int playerIndex, java.util.List<Card> hand) {
        players.get(playerIndex).clearHand();
        for (Card c : hand) players.get(playerIndex).addCard(c);
    }
}
