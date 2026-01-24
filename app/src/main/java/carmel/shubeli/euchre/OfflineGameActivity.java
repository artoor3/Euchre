package carmel.shubeli.euchre;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import carmel.shubeli.euchre.game.Card;
import carmel.shubeli.euchre.game.GameEngine;
import carmel.shubeli.euchre.game.Player;
import carmel.shubeli.euchre.game.Suit;
import android.os.Handler;
import android.os.Looper;

public class OfflineGameActivity extends AppCompatActivity {
    enum TrumpRound {
        ORDER_UP,
        CHOOSE_SUIT
    }

    private TrumpRound trumpRound = TrumpRound.ORDER_UP;
    private int passesInRow = 0;
    private Suit forbiddenSuit; // turned card suit

    private TextView trickP0, trickP1, trickP2, trickP3;
    private Button btnNextTrick;
    private boolean waitingForNextTrick = false;

    public enum GamePhase {
        TRUMP_SELECTION_ROUND_1,   // ORDER UP / PASS
        TRUMP_SELECTION_ROUND_2,   // CHOOSE SUIT / PASS
        DISCARD,
        PLAYING
    }

    private final Handler uiHandler = new Handler(Looper.getMainLooper());


    private GameEngine engine;
    private GamePhase phase = GamePhase.TRUMP_SELECTION_ROUND_1;


    private Button btnOrderUp, btnPass;
    private TextView tvInfo;

    // Trick / turn state
    private Card[] currentTrick = new Card[4];
    private int leadPlayer;               // who leads THIS trick
    private int currentPlayer;            // whose turn right now
    private Suit ledSuit = null;          // "effective" led suit (null until first card played)
    private int cardsPlayedInTrick = 0;
    private int tricksPlayed = 0;

    // Scoring
    private int tricksTeamUs = 0;         // players 0 & 2
    private int tricksTeamThem = 0;       // players 1 & 3
    private int scoreUs = 0;
    private int scoreThem = 0;
    TextView tvScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1️⃣ Inflate layout FIRST
        setContentView(R.layout.activity_offline_game);

        // 2️⃣ Bind UI views
        tvInfo = findViewById(R.id.tvCenterInfo);
        btnOrderUp = findViewById(R.id.btnOrderUp);
        btnPass = findViewById(R.id.btnPass);
        btnNextTrick = findViewById(R.id.btnNextTrick);
        tvScore = findViewById(R.id.tvScore);

        trickP0 = findViewById(R.id.trickP0);
        trickP1 = findViewById(R.id.trickP1);
        trickP2 = findViewById(R.id.trickP2);
        trickP3 = findViewById(R.id.trickP3);

        // 3️⃣ Init game engine and start first round
        engine = new GameEngine();
        engine.startNewRound();
        trumpRound = TrumpRound.ORDER_UP;
        passesInRow = 0;
        forbiddenSuit = engine.getTrumpCard().getSuit();

        phase = GamePhase.TRUMP_SELECTION_ROUND_1;
        waitingForNextTrick = false;

        showTrumpPrompt();
        renderMyHand();
        renderTrickArea();

        // =========================
        // ORDER UP button
        // =========================
        btnOrderUp.setOnClickListener(v -> {
            if (phase != GamePhase.TRUMP_SELECTION_ROUND_1 || trumpRound != TrumpRound.ORDER_UP) {
                return;
            }
            engine.orderUpTrump(0);
            engine.dealerPickupTrump();
            hideTrumpButtons();

            passesInRow = 0;
            phase = GamePhase.DISCARD;

            tvInfo.setText("You ordered up. Dealer picks up.\nPick a card to discard.");
            renderMyHand();
        });

        // =========================
        // PASS button (let AI decide or move to next phase)
        // =========================
        btnPass.setOnClickListener(v -> {
            if (phase != GamePhase.TRUMP_SELECTION_ROUND_1) return;

            passesInRow++;
            hideTrumpButtons();

            // All 4 players passed → move to phase 2 (choose suit)
            if (passesInRow == 4) {
                trumpRound = TrumpRound.CHOOSE_SUIT;
                passesInRow = 0;
                currentPlayer = (engine.getDealerIndex() + 1) % 4;
                tvInfo.setText("All passed. Choose trump suit.");
                advanceTrumpSelectionTurn();
                return;
            }

            // Otherwise advance to next player in ordering round
            advanceTrumpSelectionTurn();
        });

        // =========================
        // NEXT TRICK button
        // =========================
        btnNextTrick.setOnClickListener(v -> {
            btnNextTrick.setVisibility(View.GONE);
            waitingForNextTrick = false;

            // Clear displayed trick
            currentTrick = new Card[4];
            renderTrickArea();

            // Next trick: reset state and start with trick winner as leader
            resetTrick();
            currentPlayer = leadPlayer;
            advanceTurn();
        });
    }

    // -------------------------
    // Rendering & Player Actions
    // -------------------------

    private void renderMyHand() {
        LinearLayout handContainer = findViewById(R.id.handContainer);
        handContainer.removeAllViews();

        Player me = engine.getPlayers()[0];
        List<Card> hand = me.getHand();

        for (int i = 0; i < hand.size(); i++) {
            final int cardIndex = i;
            final Card card = hand.get(i);

            Button btn = new Button(this);
            btn.setText(card.toString());

            boolean enabled = false;
            if (phase == GamePhase.DISCARD) {
                enabled = true;  // during discard phase, allow clicking any card to discard
            } else if (phase == GamePhase.PLAYING && currentPlayer == 0) {
                enabled = isCardLegalForMe(card);
            }
            btn.setEnabled(enabled);

            btn.setOnClickListener(v -> {
                if (phase == GamePhase.DISCARD) {
                    // Dealer (you) discards the selected card
                    engine.discardCard(0, cardIndex);
                    phase = GamePhase.PLAYING;
                    tvInfo.setText("Trump is " + engine.getTrumpSuit() + "\nDiscard done.");
                    startPlayingHand();
                    return;
                }
                if (phase == GamePhase.PLAYING && currentPlayer == 0) {
                    if (!isCardLegalForMe(card)) return;  // safety check
                    playCardForPlayer(0, cardIndex);
                }
            });

            handContainer.addView(btn);
        }
    }

    /**
     * Check if a given card in my hand is legal to play (follows suit if possible).
     */
    private boolean isCardLegalForMe(Card card) {
        Player me = engine.getPlayers()[0];
        if (ledSuit == null) return true;  // if no suit led yet, any card can be played

        // If I have any card of the led suit, I must follow that suit
        boolean iHaveLedSuit = hasEffectiveSuit(me, ledSuit);
        if (!iHaveLedSuit) return true;
        return engine.getEffectiveSuit(card) == ledSuit;
    }

    /**
     * Determine if the player has at least one card of the given suit (taking trump left bower into account).
     */
    private boolean hasEffectiveSuit(Player player, Suit suit) {
        for (Card c : player.getHand()) {
            if (engine.getEffectiveSuit(c) == suit) return true;
        }
        return false;
    }

    // -------------------------
    // Progression through hand (tricks)
    // -------------------------

    private void startPlayingHand() {
        phase = GamePhase.PLAYING;
        // First trick lead goes to the player left of dealer
        leadPlayer = (engine.getDealerIndex() + 1) % 4;
        resetTrick();

        tvInfo.setText("Trump is " + engine.getTrumpSuit() + "\nLead: Player " + leadPlayer);
        currentPlayer = leadPlayer;
        advanceTurn();
    }

    private void resetTrick() {
        ledSuit = null;
        cardsPlayedInTrick = 0;
        currentTrick = new Card[4];
    }

    private void advanceTurn() {
        if (waitingForNextTrick) return;
        if (phase != GamePhase.PLAYING) return;

        if (currentPlayer == 0) {
            // Human player's turn -> wait for input (enable relevant cards in hand)
            renderMyHand();
        } else {
            // AI player's turn -> simulate play
            aiPlay(currentPlayer);
        }
    }

    private void playCardForPlayer(int playerIndex, int cardIndex) {
        if (phase != GamePhase.PLAYING) return;

        Card played = engine.playCard(playerIndex, cardIndex);

        if (ledSuit == null) {
            ledSuit = engine.getEffectiveSuit(played);
        }

        currentTrick[playerIndex] = played;
        cardsPlayedInTrick++;

        renderTrickArea();

        tvInfo.setText("Player " + playerIndex + " played " + played);

        uiHandler.postDelayed(() -> {
            if (cardsPlayedInTrick == 4) {
                finishTrick();
            } else {
                currentPlayer = (currentPlayer + 1) % 4;
                playNextTurn();
            }
        }, 800);
    }

    private void aiPlay(int aiIndex) {
        // Choose a card index for AI to play
        int idx = chooseAiCardIndex(aiIndex);
        playCardForPlayer(aiIndex, idx);
    }

    /**
     * Very simple AI strategy: follow led suit if possible; otherwise play the first card.
     */
    private int chooseAiCardIndex(int aiIndex) {
        Player ai = engine.getPlayers()[aiIndex];
        if (ledSuit == null) {
            // AI is leading the trick: play first card
            return 0;
        }
        // If AI has a card of the led suit, play the first such card
        boolean hasLed = hasEffectiveSuit(ai, ledSuit);
        if (hasLed) {
            for (int i = 0; i < ai.getHand().size(); i++) {
                if (engine.getEffectiveSuit(ai.getHand().get(i)) == ledSuit) {
                    return i;
                }
            }
        }
        // Otherwise, no card of led suit - play first card (could be trump or anything)
        return 0;
    }

    private void finishTrick() {
        int winner = engine.determineTrickWinner(currentTrick, leadPlayer);

        tricksPlayed++;

        if (winner == 0 || winner == 2) {
            tricksTeamUs++;
        } else {
            tricksTeamThem++;
        }

        tvInfo.setText("Trick winner: Player " + winner);

        if (tricksPlayed == 5) {
            handleEndOfRound();
            return;
        }

        leadPlayer = winner;

        btnNextTrick.setVisibility(View.VISIBLE);
    }

    private void handleEndOfRound() {
        boolean makersAreUs = (engine.getTrumpCaller() == 0 || engine.getTrumpCaller() == 2);
        String result;
        if (makersAreUs) {
            if (tricksTeamUs >= 3 && tricksTeamUs < 5) {
                scoreUs += 1;
                result = "US +1";
            } else if (tricksTeamUs == 5) {
                scoreUs += 2;
                result = "US +2 (SWEEP)";
            } else {
                scoreThem += 2;
                result = "EUCHRED! THEM +2";
            }
        } else {
            if (tricksTeamThem >= 3 && tricksTeamThem < 5) {
                scoreThem += 1;
                result = "THEM +1";
            } else if (tricksTeamThem == 5) {
                scoreThem += 2;
                result = "THEM +2 (SWEEP)";
            } else {
                scoreUs += 2;
                result = "THEY EUCHRED! US +2";
            }
        }
        tvInfo.setText(result + "\n\nSCORE\nUS: " + scoreUs + "\nTHEM: " + scoreThem);

        // Clear hand UI at round end
        LinearLayout handContainer = findViewById(R.id.handContainer);
        handContainer.removeAllViews();
        tvInfo.append("\n\nללחוץ כדי להתחיל סיבוב חדש");
        btnNextTrick.setVisibility(View.VISIBLE);
        btnNextTrick.setOnClickListener(v -> {
            // אפס מצב משחק
            tricksTeamUs = 0;
            tricksTeamThem = 0;
            tricksPlayed = 0;

            engine.startNewRound();
            renderScore();

            phase = GamePhase.TRUMP_SELECTION_ROUND_1;;
            trumpRound = TrumpRound.ORDER_UP;
            passesInRow = 0;
            forbiddenSuit = engine.getTrumpCard().getSuit();

            renderMyHand();
            showTrumpPrompt();
            btnNextTrick.setVisibility(View.GONE);
            advanceTrumpSelectionTurn();

        });

    }

    /**
     * Simple AI decision for trump selection (first round only): order up if AI has 2+ cards of the turned suit.
     */
    private void aiDiscard(int dealerIdx) {
        Player dealer = engine.getPlayers()[dealerIdx];
        // Simplest strategy: discard the last card in dealer's hand
        int discardIndex = dealer.getHand().size() - 1;
        if (discardIndex >= 0) {
            engine.discardCard(dealerIdx, discardIndex);
        }
    }

    private void hideTrumpButtons() {
        btnOrderUp.setVisibility(View.GONE);
        btnPass.setVisibility(View.GONE);
    }

    private void showTrumpPrompt() {
        tvInfo.setText(
                "You: Player 0\n" +
                        "Dealer: Player " + engine.getDealerIndex() + "\n" +
                        "Trump card: " + engine.getTrumpCard() + "\n" +
                        "Order up?"
        );
    }
    private void showRoundIntro() {
        Card up = engine.getTrumpCard();
        int dealer = engine.getDealerIndex();

        tvInfo.setText(
                "New round\n" +
                        "Dealer: Player " + dealer + "\n" +
                        "Up card: " + up
        );

        // השהיה לפני שמתחילים לבחור טרמפ
        uiHandler.postDelayed(this::startTrumpSelection, 1500);
    }

    private void renderTrickArea() {
        trickP0.setText("YOU: " + cardToText(currentTrick[0]));
        trickP1.setText("P1: " + cardToText(currentTrick[1]));
        trickP2.setText("P2: " + cardToText(currentTrick[2]));
        trickP3.setText("P3: " + cardToText(currentTrick[3]));
    }


    private String cardToText(Card c) {
        if (c == null) return "-";
        return c.getRank() + " of " + c.getSuit();
    }


    private void advanceTrumpSelectionTurn() {
        // phase guard
        if (phase != GamePhase.TRUMP_SELECTION_ROUND_1) return;

        // אם אנחנו בסיבוב שני - מציגים בחירת סדרה / AI בחירת סדרה
        if (trumpRound == TrumpRound.CHOOSE_SUIT) {
            if (currentPlayer == 0) {
                showSuitSelection();
            } else {
                aiChooseTrump(); // נוסיף עוד רגע
            }
            return;
        }

        // סיבוב ראשון: ORDER_UP / PASS
        if (currentPlayer == 0) {
            tvInfo.setText("Your turn: ORDER UP or PASS");
            btnOrderUp.setVisibility(View.VISIBLE);
            btnPass.setVisibility(View.VISIBLE);
        } else {
            aiTrumpDecision();
        }
    }


    private void aiTrumpDecision() {
        if (trumpRound != TrumpRound.ORDER_UP) return;
        int ai = currentPlayer;

        tvInfo.setText("Player " + ai + " is thinking...");

        new Handler().postDelayed(() -> {
            int count = 0;
            Suit suit = engine.getTrumpCard().getSuit();
            for (Card c : engine.getPlayers()[ai].getHand()) {
                if (c.getSuit() == suit) count++;
            }

            if (count >= 2) {
                tvInfo.setText("Player " + ai + " ordered up.");
                tvInfo.append("\nTrump suit: " + engine.getTrumpSuit());
                engine.orderUpTrump(ai);
                engine.dealerPickupTrump();
                hideTrumpButtons();
                passesInRow = 0;
                phase = GamePhase.DISCARD;

                if (engine.getDealerIndex() == 0) {
                    tvInfo.append("\nYou are dealer. Discard a card.");
                    renderMyHand();
                } else {
                    aiDiscard(engine.getDealerIndex());
                    startPlayingHand();
                }
            } else {
                tvInfo.setText("Player " + ai + " passed.");

                passesInRow++;
                new Handler().postDelayed(() -> {
                    if (passesInRow == 4) {
                        trumpRound = TrumpRound.CHOOSE_SUIT;
                        passesInRow = 0;
                        currentPlayer = (engine.getDealerIndex() + 1) % 4;
                        tvInfo.setText("All passed. Choose trump suit.");
                        String emoji = suit == Suit.HEARTS ? "♥" :
                                suit == Suit.SPADES ? "♠" :
                                        suit == Suit.CLUBS ? "♣" : "♦";

                        tvInfo.setText("Player " + ai + " chose " + emoji + " " + suit + " as trump.");

                        advanceTrumpSelectionTurn();
                    } else {
                        advanceTrumpSelectionTurn();
                    }
                }, 800);
            }

        }, 800);
    }


    private void renderScore() {
        tvScore.setText("Team 0: " + engine.getGameScore(0) + " | Team 1: " + engine.getGameScore(1));
    }

    private void showSuitSelection() {
        // מוודאים שזה באמת שלב 2
        if (phase != GamePhase.TRUMP_SELECTION_ROUND_1) return;

        // מסתירים כפתורי סיבוב 1
        btnOrderUp.setVisibility(View.GONE);
        btnPass.setVisibility(View.GONE);

        // מביאים כפתורים + container
        View suitContainer = findViewById(R.id.suitButtonsContainer);
        Button btnSpades = findViewById(R.id.btnSpades);
        Button btnHearts = findViewById(R.id.btnHearts);
        Button btnDiamonds = findViewById(R.id.btnDiamonds);
        Button btnClubs = findViewById(R.id.btnClubs);
        Button btnPass2 = findViewById(R.id.btnPass2);

        suitContainer.setVisibility(View.VISIBLE);

        tvInfo.setText("Round 2: Choose trump (not " + forbiddenSuit + ")");

        // פונקציה פנימית קטנה: להגדיר כפתור של סדרה
        java.util.function.BiConsumer<Button, Suit> setupSuitBtn = (button, suit) -> {
            if (suit == forbiddenSuit) {
                button.setVisibility(View.GONE);
                button.setOnClickListener(null);
                return;
            }

            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(v -> {
                // אתה בחרת טראמפ
                engine.setTrumpSuit(suit);
                engine.setTrumpCaller(0); // אתה (Player 0)

                // מסתירים כפתורי בחירה
                suitContainer.setVisibility(View.GONE);

                // הודעה + מעבר להמשך
                showMsgThen("You chose " + suit + " as trump.", 900, () -> {
                    // בסיבוב 2 אין pickup של קלף — ישר מתחילים לשחק
                    passesInRow = 0;
                    startPlayingHand();
                });
            });
        };

        setupSuitBtn.accept(btnSpades, Suit.SPADES);
        setupSuitBtn.accept(btnHearts, Suit.HEARTS);
        setupSuitBtn.accept(btnDiamonds, Suit.DIAMONDS);
        setupSuitBtn.accept(btnClubs, Suit.CLUBS);

        // PASS בשלב 2
        btnPass2.setVisibility(View.VISIBLE);
        btnPass2.setOnClickListener(v -> {
            passesInRow++;
            suitContainer.setVisibility(View.GONE);

            showMsgThen("You passed.", 900, () -> {
                goToNextTrumpPlayer();
            });
        });
    }
    private void showMsgThen(String msg, int ms, Runnable next) {
        tvInfo.setText(msg);
        uiHandler.postDelayed(next, ms);
    }

    private void goToNextTrumpPlayer() {
        currentPlayer = (currentPlayer + 1) % 4;
        advanceTrumpSelectionTurn();
    }
    private void aiChooseTrump() {
        int ai = currentPlayer;

        showMsgThen("Player " + ai + " is thinking (choose suit)...", 800, () -> {
            Suit chosen = null;
            for (Suit s : Suit.values()) {
                if (s != forbiddenSuit) {
                    chosen = s;
                    break;
                }
            }

            if (chosen == null) {
                passesInRow++;
                showMsgThen("Player " + ai + " passed.", 900, () -> goToNextTrumpPlayer());
                return;
            }

            engine.setTrumpSuit(chosen);
            engine.setTrumpCaller(ai);

            showMsgThen("Player " + ai + " chose " + chosen + " as trump.", 1100, () -> {
                passesInRow = 0;
                startPlayingHand();
            });
        });
    }
    private void startTrick() {
        cardsPlayedInTrick = 0;
        ledSuit = null;
        currentTrick = new Card[4];

        tvInfo.setText("New trick started");
        playNextTurn();
    }
    private void playNextTurn() {
        if (currentPlayer == 0) {
            tvInfo.setText("Your turn");
            renderMyHand();
        } else {
            tvInfo.setText("Player " + currentPlayer + " is thinking...");
            uiHandler.postDelayed(
                    () -> aiPlay(currentPlayer),
                    900
            );
        }
    }
    private void startNextTrick() {
        btnNextTrick.setVisibility(View.GONE);

        currentTrick = new Card[4];
        cardsPlayedInTrick = 0;
        ledSuit = null;

        currentPlayer = leadPlayer;

        tvInfo.setText("Player " + currentPlayer + " leads");

        playNextTurn();
    }
    private void startTrumpSelection() {
        phase = GamePhase.TRUMP_SELECTION_ROUND_1;

        passesInRow = 0;
        forbiddenSuit = engine.getTrumpCard().getSuit();

        currentPlayer = (engine.getDealerIndex() + 1) % 4;

        advanceTrumpSelectionTurn();
    }

}