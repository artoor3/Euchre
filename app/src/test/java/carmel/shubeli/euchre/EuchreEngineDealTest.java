package carmel.shubeli.euchre;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import engine_clean.model.Suit;
import engine_clean.model.Rank;
import java.util.List;
import engine_clean.core.EuchreEngine;
import engine_clean.core.GamePhase;
import engine_clean.model.Card;
import engine_clean.model.Player;


import java.util.List;
public class EuchreEngineDealTest {

    @Test
    public void startNewRound_deals5Each_andUpCardUnique() {
        // assertEquals("I WANT THIS TO FAIL", 123, 456); // להשאיר כבוי עכשיו

        EuchreEngine engine = new EuchreEngine();
        engine.startNewRound();

        assertEquals(GamePhase.ORDERING_TRUMP_ROUND1, engine.getPhase());
        assertNotNull(engine.getUpCard());

        Set<String> seen = new HashSet<>();

        for (Player p : engine.getPlayers()) {
            assertEquals(5, p.getHand().size());
            for (Card c : p.getHand()) {
                String key = c.getRank() + "-" + c.getSuit();
                assertTrue("Duplicate card in hands: " + key, seen.add(key));
            }
        }

        String upKey = engine.getUpCard().getRank() + "-" + engine.getUpCard().getSuit();
        assertTrue("UpCard duplicates a card in hands: " + upKey, seen.add(upKey));

        assertEquals(21, seen.size());
    }
    @Test
    public void startNewRound_firstTurnIsLeftOfDealer() {
        EuchreEngine engine = new EuchreEngine();
        engine.startNewRound();

        int dealer = engine.getDealerIndex();
        int expected = (dealer + 1) % 4;

        assertEquals(expected, engine.getCurrentPlayerIndex());
    }
    @Test
    public void trumpRound1_allPass_movesToRound2_andResetsTurn() {
        EuchreEngine engine = new EuchreEngine();
        engine.startNewRound();

        int dealer = engine.getDealerIndex();
        int expectedFirst = (dealer + 1) % 4;
        assertEquals(expectedFirst, engine.getCurrentPlayerIndex());

        engine.pass();
        engine.pass();
        engine.pass();
        engine.pass();

        assertEquals(GamePhase.ORDERING_TRUMP_ROUND2, engine.getPhase());
        assertEquals(expectedFirst, engine.getCurrentPlayerIndex());
    }
    @Test
    public void trumpRound2_allPass_redeals() {
        EuchreEngine engine = new EuchreEngine();
        engine.startNewRound();

        Card firstUp = engine.getUpCard();

        // move to round 2
        engine.pass(); engine.pass(); engine.pass(); engine.pass();
        assertEquals(GamePhase.ORDERING_TRUMP_ROUND2, engine.getPhase());

        // all pass in round 2 => redeal
        engine.pass(); engine.pass(); engine.pass(); engine.pass();

        assertEquals(GamePhase.ORDERING_TRUMP_ROUND1, engine.getPhase());
        assertNotNull(engine.getUpCard());

        // very likely different; not guaranteed, so we don't assert inequality
    }
    @Test
    public void orderUp_round1_setsTrump_andDealerHas6_andPhaseDiscarding() {
        EuchreEngine engine = new EuchreEngine();
        engine.startNewRound();

        int dealer = engine.getDealerIndex();
        int caller = engine.getCurrentPlayerIndex(); // left of dealer at start

        engine.orderUp(Suit.HEARTS); // suit ignored in round 1

        assertEquals(engine.getUpCard().getSuit(), engine.getTrumpSuit());
        assertEquals(caller, engine.getTrumpCaller());
        assertEquals(GamePhase.DISCARDING, engine.getPhase());
        assertEquals(dealer, engine.getCurrentPlayerIndex());

        assertEquals(6, engine.getPlayers().get(dealer).getHand().size());
    }
    @Test
    public void discard_afterPickup_returnsTo5_andStartsPlaying() {
        EuchreEngine engine = new EuchreEngine();
        engine.startNewRound();

        int dealer = engine.getDealerIndex();
        int expectedLead = (dealer + 1) % 4;

        engine.orderUp(Suit.SPADES);
        engine.discard(0);

        assertEquals(5, engine.getPlayers().get(dealer).getHand().size());
        assertEquals(GamePhase.PLAYING_TRICK, engine.getPhase());
        assertEquals(expectedLead, engine.getCurrentPlayerIndex());
    }

    @Test
    public void playCard_firstCard_setsLedSuit() {
        EuchreEngine engine = new EuchreEngine();
        engine.startNewRound();
        engine.orderUp(engine.getUpCard().getSuit());
        engine.discard(0);

        int leader = engine.getCurrentPlayerIndex();
        engine.playCard(leader, 0);

        assertNotNull(engine.getLedSuit());
        assertEquals(1, engine.getCardsPlayedInTrick());
    }
    @Test(expected = IllegalArgumentException.class)
    public void playCard_mustFollowSuit_ifHasLedSuit() {
        EuchreEngine e = new EuchreEngine();

        e._testSetPhase(GamePhase.PLAYING_TRICK);
        e._testSetTrumpSuit(Suit.SPADES);
        e._testResetTrick();

        // Player 0 leads HEARTS
        e._testSetTurn(0);
        e._testSetHand(0, List.of(
                new Card(Suit.HEARTS, Rank.NINE),
                new Card(Suit.CLUBS, Rank.ACE),
                new Card(Suit.DIAMONDS, Rank.TEN),
                new Card(Suit.SPADES, Rank.KING),
                new Card(Suit.CLUBS, Rank.QUEEN)
        ));
        e.playCard(0, 0); // lead = HEARTS

        // Player 1 has a HEARTS card but tries to play NOT HEARTS => illegal
        e._testSetHand(1, List.of(
                new Card(Suit.HEARTS, Rank.ACE),     // has led suit
                new Card(Suit.CLUBS, Rank.KING),     // illegal choice if played
                new Card(Suit.SPADES, Rank.NINE),
                new Card(Suit.CLUBS, Rank.TEN),
                new Card(Suit.DIAMONDS, Rank.QUEEN)
        ));

        // turn advanced to player 1 automatically
        e.playCard(1, 1); // tries K♣ while having A♥ => must throw
    }
    @Test
    public void playCard_allowsOffSuit_ifNoLedSuitInHand() {
        EuchreEngine e = new EuchreEngine();

        e._testSetPhase(GamePhase.PLAYING_TRICK);
        e._testSetTrumpSuit(Suit.SPADES);
        e._testResetTrick();

        // Player 0 leads HEARTS
        e._testSetTurn(0);
        e._testSetHand(0, List.of(
                new Card(Suit.HEARTS, Rank.NINE),
                new Card(Suit.CLUBS, Rank.ACE),
                new Card(Suit.DIAMONDS, Rank.TEN),
                new Card(Suit.SPADES, Rank.KING),
                new Card(Suit.CLUBS, Rank.QUEEN)
        ));
        e.playCard(0, 0); // ledSuit = HEARTS

        // Player 1 has NO HEARTS at all
        e._testSetHand(1, List.of(
                new Card(Suit.CLUBS, Rank.ACE),
                new Card(Suit.CLUBS, Rank.KING),
                new Card(Suit.SPADES, Rank.NINE),
                new Card(Suit.CLUBS, Rank.TEN),
                new Card(Suit.DIAMONDS, Rank.QUEEN)
        ));

        // Should NOT throw (allowed to play off-suit)
        e.playCard(1, 0);

        // sanity checks
        assertEquals(2, e.getCardsPlayedInTrick());
        assertEquals(Suit.HEARTS, e.getLedSuit());
    }
    @Test
    public void trick_rightBower_wins() {
        EuchreEngine e = new EuchreEngine();

        e._testSetPhase(GamePhase.PLAYING_TRICK);
        e._testSetTrumpSuit(Suit.SPADES);
        e._testResetTrick();
        e._testSetTurn(0);

        // Player 0 leads Ace of Spades (trump)
        e._testSetHand(0, List.of(
                new Card(Suit.SPADES, Rank.ACE),
                new Card(Suit.HEARTS, Rank.NINE),
                new Card(Suit.CLUBS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.NINE),
                new Card(Suit.HEARTS, Rank.TEN)
        ));
        e.playCard(0, 0);

        // Player 1 plays Right Bower (J♠)
        e._testSetHand(1, List.of(
                new Card(Suit.SPADES, Rank.JACK),
                new Card(Suit.HEARTS, Rank.NINE),
                new Card(Suit.CLUBS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.NINE),
                new Card(Suit.HEARTS, Rank.TEN)
        ));
        e.playCard(1, 0);

        // Player 2
        e._testSetHand(2, List.of(
                new Card(Suit.SPADES, Rank.NINE),
                new Card(Suit.HEARTS, Rank.NINE),
                new Card(Suit.CLUBS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.NINE),
                new Card(Suit.HEARTS, Rank.TEN)
        ));
        e.playCard(2, 0);

        // Player 3
        e._testSetHand(3, List.of(
                new Card(Suit.HEARTS, Rank.KING),
                new Card(Suit.HEARTS, Rank.NINE),
                new Card(Suit.CLUBS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.NINE),
                new Card(Suit.HEARTS, Rank.TEN)
        ));
        e.playCard(3, 0);

        // Player 1 should win
        assertEquals(1, e.getCurrentPlayerIndex());
    }
    @Test
    public void trick_leftBower_beats_trumpAce() {
        EuchreEngine e = new EuchreEngine();

        e._testSetPhase(GamePhase.PLAYING_TRICK);
        e._testSetTrumpSuit(Suit.SPADES);
        e._testResetTrick();
        e._testSetTurn(0);

        // Player 0 leads trump Ace (A♠)
        e._testSetHand(0, List.of(
                new Card(Suit.SPADES, Rank.ACE),
                new Card(Suit.HEARTS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.NINE),
                new Card(Suit.HEARTS, Rank.TEN),
                new Card(Suit.DIAMONDS, Rank.TEN)
        ));
        e.playCard(0, 0);

        // Player 1 plays Left Bower (J♣ counts as spades) -> should win
        e._testSetHand(1, List.of(
                new Card(Suit.CLUBS, Rank.JACK),
                new Card(Suit.HEARTS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.NINE),
                new Card(Suit.HEARTS, Rank.TEN),
                new Card(Suit.DIAMONDS, Rank.TEN)
        ));
        e.playCard(1, 0);

        // Player 2 anything
        e._testSetHand(2, List.of(
                new Card(Suit.SPADES, Rank.NINE),
                new Card(Suit.HEARTS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.NINE),
                new Card(Suit.HEARTS, Rank.TEN),
                new Card(Suit.DIAMONDS, Rank.TEN)
        ));
        e.playCard(2, 0);

        // Player 3 anything
        e._testSetHand(3, List.of(
                new Card(Suit.HEARTS, Rank.KING),
                new Card(Suit.HEARTS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.NINE),
                new Card(Suit.HEARTS, Rank.TEN),
                new Card(Suit.DIAMONDS, Rank.TEN)
        ));
        e.playCard(3, 0);

        assertEquals(1, e.getCurrentPlayerIndex());
    }
    @Test
    public void trick_ledSuit_highest_wins_over_offSuit() {
        EuchreEngine e = new EuchreEngine();

        e._testSetPhase(GamePhase.PLAYING_TRICK);
        e._testSetTrumpSuit(Suit.SPADES);
        e._testResetTrick();
        e._testSetTurn(0);

        // Lead HEARTS: 10♥
        e._testSetHand(0, List.of(
                new Card(Suit.HEARTS, Rank.TEN),
                new Card(Suit.CLUBS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.NINE),
                new Card(Suit.CLUBS, Rank.TEN),
                new Card(Suit.DIAMONDS, Rank.TEN)
        ));
        e.playCard(0, 0);

        // Player 1 off-suit Ace (A♦) - should NOT win
        // ensure no hearts in hand to avoid follow-suit exception
        e._testSetHand(1, List.of(
                new Card(Suit.DIAMONDS, Rank.ACE),
                new Card(Suit.CLUBS, Rank.KING),
                new Card(Suit.SPADES, Rank.NINE),
                new Card(Suit.CLUBS, Rank.TEN),
                new Card(Suit.DIAMONDS, Rank.KING)
        ));
        e.playCard(1, 0);

        // Player 2 follows ledSuit with Ace of Hearts -> should win
        e._testSetHand(2, List.of(
                new Card(Suit.HEARTS, Rank.ACE),
                new Card(Suit.CLUBS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.NINE),
                new Card(Suit.CLUBS, Rank.TEN),
                new Card(Suit.DIAMONDS, Rank.TEN)
        ));
        e.playCard(2, 0);

        // Player 3 follows ledSuit with Nine of Hearts
        e._testSetHand(3, List.of(
                new Card(Suit.HEARTS, Rank.NINE),
                new Card(Suit.CLUBS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.NINE),
                new Card(Suit.CLUBS, Rank.TEN),
                new Card(Suit.DIAMONDS, Rank.TEN)
        ));
        e.playCard(3, 0);

        assertEquals(2, e.getCurrentPlayerIndex());
    }
    @Test
    public void hand_ends_after_5_tricks() {
        EuchreEngine e = new EuchreEngine();

        e._testSetPhase(GamePhase.PLAYING_TRICK);
        e._testSetTrumpSuit(Suit.SPADES);

        for (int t = 0; t < 5; t++) {
            e._testResetTrick();
            e._testSetTurn(0);

            // Player 0 leads trump Ace
            e._testSetHand(0, List.of(
                    new Card(Suit.SPADES, Rank.ACE),
                    new Card(Suit.HEARTS, Rank.NINE),
                    new Card(Suit.CLUBS, Rank.NINE),
                    new Card(Suit.DIAMONDS, Rank.NINE),
                    new Card(Suit.HEARTS, Rank.TEN)
            ));
            e.playCard(0, 0);

            // Player 1 plays Right Bower (J♠) => team 1 should win trick
            e._testSetHand(1, List.of(
                    new Card(Suit.SPADES, Rank.JACK),
                    new Card(Suit.HEARTS, Rank.NINE),
                    new Card(Suit.CLUBS, Rank.NINE),
                    new Card(Suit.DIAMONDS, Rank.NINE),
                    new Card(Suit.HEARTS, Rank.TEN)
            ));
            e.playCard(1, 0);

            // Player 2 anything
            e._testSetHand(2, List.of(
                    new Card(Suit.SPADES, Rank.NINE),
                    new Card(Suit.HEARTS, Rank.NINE),
                    new Card(Suit.CLUBS, Rank.NINE),
                    new Card(Suit.DIAMONDS, Rank.NINE),
                    new Card(Suit.HEARTS, Rank.TEN)
            ));
            e.playCard(2, 0);

            // Player 3 anything
            e._testSetHand(3, List.of(
                    new Card(Suit.HEARTS, Rank.KING),
                    new Card(Suit.HEARTS, Rank.NINE),
                    new Card(Suit.CLUBS, Rank.NINE),
                    new Card(Suit.DIAMONDS, Rank.NINE),
                    new Card(Suit.HEARTS, Rank.TEN)
            ));
            e.playCard(3, 0);

            // אחרי כל טריק winner מוביל, אבל אנחנו מאפסים ל-0 בתחילת הלולאה בכוונה
            // כדי שיהיה טסט קל שמוכיח רק "סופרים ומסיימים יד".
        }

        assertEquals(5, e.getTricksPlayedThisHand());
        assertEquals(GamePhase.SCORING, e.getPhase());

        assertEquals(5, e.getTricksTeam0() + e.getTricksTeam1());
        assertEquals(0, e.getTricksTeam0());
        assertEquals(5, e.getTricksTeam1());
    }
    private Suit firstNonTrumpSuit(Suit trump) {
        for (Suit s : Suit.values()) {
            if (s != trump) return s;
        }
        throw new IllegalStateException("No non-trump suit?");
    }

    private void rigTrickSoWinnerWinsWithRightBower(EuchreEngine e, int winnerIndex) {
        Suit trump = e.getTrumpSuit();
        Suit offSuit = firstNonTrumpSuit(trump);

        int leader = e.getCurrentPlayerIndex();

        // Leader מוביל off-suit (A) => ledSuit = offSuit
        // Winner ישים Right Bower (J of trump) ובכוונה לא תהיה לו offSuit ביד => מותר לו לא לעקוב
        for (int p = 0; p < 4; p++) {
            if (p == leader) {
                e._testSetHand(p, List.of(
                        new Card(offSuit, Rank.ACE),
                        new Card(trump, Rank.NINE),
                        new Card(trump, Rank.TEN),
                        new Card(trump, Rank.QUEEN),
                        new Card(trump, Rank.KING)
                ));
            } else if (p == winnerIndex) {
                e._testSetHand(p, List.of(
                        new Card(trump, Rank.JACK), // Right Bower
                        new Card(trump, Rank.NINE),
                        new Card(trump, Rank.TEN),
                        new Card(trump, Rank.QUEEN),
                        new Card(trump, Rank.KING)
                ));
            } else {
                // יש להם offSuit כדי לעקוב, אבל הם חלשים
                e._testSetHand(p, List.of(
                        new Card(offSuit, Rank.NINE),
                        new Card(trump, Rank.NINE),
                        new Card(trump, Rank.TEN),
                        new Card(trump, Rank.QUEEN),
                        new Card(trump, Rank.KING)
                ));
            }
        }

        // לשחק 4 קלפים, תמיד index 0 בכל יד
        for (int i = 0; i < 4; i++) {
            int p = e.getCurrentPlayerIndex();
            e.playCard(p, 0);
        }

        // אחרי הטריק, winner אמור להוביל
        assertEquals(winnerIndex, e.getCurrentPlayerIndex());
    }
    @Test
    public void scoring_callerTeamWins3_gets1Point() {
        EuchreEngine e = new EuchreEngine();
        e.startNewRound();

        int dealerBefore = e.getDealerIndex();
        int caller = e.getCurrentPlayerIndex(); // left of dealer בתחילת Round1

        // Order up אמיתי
        e.orderUp(Suit.HEARTS); // מתעלם מהפרמטר ב-Round1
        e.discard(0);

        int callerTeam = e.getPlayers().get(caller).getTeamId();
        int otherTeam = (callerTeam == 0) ? 1 : 0;

        // נגרום ל-callerTeam לנצח 3 טריקים, וליריבה 2
        int winnerCaller = (callerTeam == 0) ? 0 : 1; // שחקן בצוות של caller
        int winnerOther  = (otherTeam == 0) ? 0 : 1;

        rigTrickSoWinnerWinsWithRightBower(e, winnerCaller);
        rigTrickSoWinnerWinsWithRightBower(e, winnerCaller);
        rigTrickSoWinnerWinsWithRightBower(e, winnerCaller);

        rigTrickSoWinnerWinsWithRightBower(e, winnerOther);
        rigTrickSoWinnerWinsWithRightBower(e, winnerOther);

        assertEquals(5, e.getTricksPlayedThisHand());
        assertEquals(GamePhase.SCORING, e.getPhase());

        int scoreCallerBefore = e.getTeamScores()[callerTeam];
        int scoreOtherBefore  = e.getTeamScores()[otherTeam];

        e.scoreHandAndStartNextRound();

        assertEquals(scoreCallerBefore + 1, e.getTeamScores()[callerTeam]);
        assertEquals(scoreOtherBefore, e.getTeamScores()[otherTeam]);

        // dealer rotate
        assertEquals((dealerBefore + 1) % 4, e.getDealerIndex());

        // התחיל סיבוב חדש
        assertEquals(GamePhase.ORDERING_TRUMP_ROUND1, e.getPhase());
    }
    @Test
    public void scoring_callerTeamEuchred_otherGets2Points() {
        EuchreEngine e = new EuchreEngine();
        e.startNewRound();

        int dealerBefore = e.getDealerIndex();
        int caller = e.getCurrentPlayerIndex();

        e.orderUp(Suit.HEARTS);
        e.discard(0);

        int callerTeam = e.getPlayers().get(caller).getTeamId();
        int otherTeam = (callerTeam == 0) ? 1 : 0;

        int winnerCaller = (callerTeam == 0) ? 0 : 1;
        int winnerOther  = (otherTeam == 0) ? 0 : 1;

        // callerTeam רק 2 טריקים, היריבה 3 => caller euchred
        rigTrickSoWinnerWinsWithRightBower(e, winnerCaller);
        rigTrickSoWinnerWinsWithRightBower(e, winnerCaller);

        rigTrickSoWinnerWinsWithRightBower(e, winnerOther);
        rigTrickSoWinnerWinsWithRightBower(e, winnerOther);
        rigTrickSoWinnerWinsWithRightBower(e, winnerOther);

        assertEquals(GamePhase.SCORING, e.getPhase());

        int scoreCallerBefore = e.getTeamScores()[callerTeam];
        int scoreOtherBefore  = e.getTeamScores()[otherTeam];

        e.scoreHandAndStartNextRound();

        assertEquals(scoreCallerBefore, e.getTeamScores()[callerTeam]);
        assertEquals(scoreOtherBefore + 2, e.getTeamScores()[otherTeam]);

        assertEquals((dealerBefore + 1) % 4, e.getDealerIndex());
        assertEquals(GamePhase.ORDERING_TRUMP_ROUND1, e.getPhase());
    }
    @Test(expected = IllegalStateException.class)
    public void cannotPlayCardInScoringPhase() {
        EuchreEngine e = new EuchreEngine();
        e._testSetPhase(GamePhase.SCORING);
        e.playCard(0, 0);
    }
    @Test(expected = IllegalStateException.class)
    public void cannotScoreOutsideScoringPhase() {
        EuchreEngine e = new EuchreEngine();
        e.startNewRound();
        e.scoreHandAndStartNextRound();
    }
    private int playerOnTeam(int teamId) {
        return (teamId == 0) ? 0 : 1; // מספיק לבחור 0 לצוות 0 ו-1 לצוות 1
    }

    private void playHandGivingTeam0TwoPoints(EuchreEngine e) {
        assertEquals(GamePhase.ORDERING_TRUMP_ROUND1, e.getPhase());

        e.orderUp(Suit.HEARTS);
        e.discard(0);

        int caller = e.getTrumpCaller();
        int callerTeam = e.getPlayers().get(caller).getTeamId();

        int team0Winner = 0; // team 0 player
        int team1Winner = 1; // team 1 player

        if (callerTeam == 0) {
            // callerTeam=0 => תן ל-Team0 March => +2
            for (int i = 0; i < 5; i++) rigTrickSoWinnerWinsWithRightBower(e, team0Winner);
        } else {
            // callerTeam=1 => תגרום ל-caller להיכשל => Team0 יקבל +2
            // כלומר Team1 יקח רק 2 טריקים, Team0 יקח 3
            for (int i = 0; i < 2; i++) rigTrickSoWinnerWinsWithRightBower(e, team1Winner);
            for (int i = 0; i < 3; i++) rigTrickSoWinnerWinsWithRightBower(e, team0Winner);
        }

        assertEquals(GamePhase.SCORING, e.getPhase());
    }
    @Test
    public void gameOver_team0Reaches10() {
        EuchreEngine e = new EuchreEngine();
        e.startNewRound();

        for (int i = 0; i < 5; i++) { // 5 ידיים * 2 נקודות = 10
            playHandGivingTeam0TwoPoints(e);
            e.scoreHandAndStartNextRound();

            if (e.getPhase() == GamePhase.GAME_OVER) break;
        }

        assertEquals(GamePhase.GAME_OVER, e.getPhase());
        assertTrue(e.getTeamScores()[0] >= 10);
    }
}
