package carmel.shubeli.euchre;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import engine_clean.core.GamePhase;
import engine_clean.model.Card;
import engine_clean.model.Suit;

public class GameActivityClean extends AppCompatActivity {

    private GameController controller;

    private TextView bannerText;

    private ImageView trickP0, trickP1, trickP2, trickP3;
    private ImageView p1Back, p2Back, p3Back;
    private TextView p1Count, p2Count, p3Count;

    private LinearLayout handContainer;

    private Button btnPass, btnOrderUp, btnContinue, btnAuto;
    private Button btnTrumpH, btnTrumpD, btnTrumpC, btnTrumpS;
    private ImageView deckPile;
    private ImageView flyingCard;

    private boolean dealingAnimationRunning = false;
    private int lastSeenHandNumber = -1; // כדי לדעת שמתחיל סיבוב חדש
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_clean);

        controller = new GameController();

        bindViews();
        bindClicks();

        render();
    }

    private void bindViews() {
        bannerText = findViewById(R.id.bannerText);

        trickP0 = findViewById(R.id.trickP0);
        trickP1 = findViewById(R.id.trickP1);
        trickP2 = findViewById(R.id.trickP2);
        trickP3 = findViewById(R.id.trickP3);

        p1Back = findViewById(R.id.p1Back);
        p2Back = findViewById(R.id.p2Back);
        p3Back = findViewById(R.id.p3Back);

        p1Count = findViewById(R.id.p1Count);
        p2Count = findViewById(R.id.p2Count);
        p3Count = findViewById(R.id.p3Count);

        handContainer = findViewById(R.id.handContainer);

        btnPass = findViewById(R.id.btnPass);
        btnOrderUp = findViewById(R.id.btnOrderUp);
        btnContinue = findViewById(R.id.btnContinue);
        btnAuto = findViewById(R.id.btnAuto);
        btnTrumpH = findViewById(R.id.btnTrumpH);
        btnTrumpD = findViewById(R.id.btnTrumpD);
        btnTrumpC = findViewById(R.id.btnTrumpC);
        btnTrumpS = findViewById(R.id.btnTrumpS);
        deckPile = findViewById(R.id.deckPile);
        flyingCard = findViewById(R.id.flyingCard);
    }

    private void bindClicks() {

        btnPass.setOnClickListener(v -> {
            safeRun(() -> controller.pass());
            render();
        });

        btnOrderUp.setOnClickListener(v -> {
            safeRun(() -> {
                Suit suitToOrder;
                if (controller.getPhase() == GamePhase.ORDERING_TRUMP_ROUND1) {
                    suitToOrder = controller.getUpCard().getSuit(); // round1
                } else {
                    suitToOrder = controller.getSelectableTrumpSuitsRound2().get(0); // round2 זמני
                }
                controller.orderUp(suitToOrder);
            });
            render();
        });

        btnContinue.setOnClickListener(v -> {
            safeRun(() -> controller.continueAfterScoring());
            render();
        });

        btnAuto.setOnClickListener(v -> {
            safeRun(this::autoStepUntilHumanOrStop);
            render();
        });
        btnTrumpH.setOnClickListener(v -> { safeRun(() -> controller.orderUp(Suit.HEARTS)); render(); });
        btnTrumpD.setOnClickListener(v -> { safeRun(() -> controller.orderUp(Suit.DIAMONDS)); render(); });
        btnTrumpC.setOnClickListener(v -> { safeRun(() -> controller.orderUp(Suit.CLUBS)); render(); });
        btnTrumpS.setOnClickListener(v -> { safeRun(() -> controller.orderUp(Suit.SPADES)); render(); });
    }

    private void render() {
        // buttons off by default
        btnPass.setVisibility(View.GONE);
        btnOrderUp.setVisibility(View.GONE);
        btnContinue.setVisibility(View.GONE);

        // Banner
        GamePhase phase = controller.getPhase();
        String turnMsg = controller.isHumanTurn() ? "Your turn" : ("Waiting for Player " + controller.getCurrentPlayerIndex());
        bannerText.setText(turnMsg + "  |  " + phase.name());

        // Opponents backs + counts
        p1Back.setImageResource(R.drawable.c_back);
        p2Back.setImageResource(R.drawable.c_back);
        p3Back.setImageResource(R.drawable.c_back);

        p1Count.setText("P1: " + controller.getHandSize(1));
        p2Count.setText("P2: " + controller.getHandSize(2));
        p3Count.setText("P3: " + controller.getHandSize(3));

        // Trick
        renderTrick();

        // Hand
        renderHand();
// hide suit buttons by default
        btnTrumpH.setVisibility(View.GONE);
        btnTrumpD.setVisibility(View.GONE);
        btnTrumpC.setVisibility(View.GONE);
        btnTrumpS.setVisibility(View.GONE);

        if (phase == GamePhase.ORDERING_TRUMP_ROUND1) {
            if (controller.isHumanTurn()) {
                btnPass.setVisibility(View.VISIBLE);
                btnOrderUp.setVisibility(View.VISIBLE); // round1 = upcard suit
            }
        }

        if (phase == GamePhase.ORDERING_TRUMP_ROUND2) {
            if (controller.isHumanTurn()) {
                btnPass.setVisibility(View.VISIBLE);

                // show all suits except forbidden (upcard suit)
                Suit forbidden = controller.getUpCard().getSuit();

                btnTrumpH.setVisibility(forbidden == Suit.HEARTS ? View.GONE : View.VISIBLE);
                btnTrumpD.setVisibility(forbidden == Suit.DIAMONDS ? View.GONE : View.VISIBLE);
                btnTrumpC.setVisibility(forbidden == Suit.CLUBS ? View.GONE : View.VISIBLE);
                btnTrumpS.setVisibility(forbidden == Suit.SPADES ? View.GONE : View.VISIBLE);

                // ב-round2 לא צריך btnOrderUp בכלל
                btnOrderUp.setVisibility(View.GONE);
            }
        }
        // Phase-specific actions
        if (phase == GamePhase.ORDERING_TRUMP_ROUND1 || phase == GamePhase.ORDERING_TRUMP_ROUND2) {
            if (controller.isHumanTurn()) {
                btnPass.setVisibility(View.VISIBLE);
                btnOrderUp.setVisibility(View.VISIBLE);
            }
        } else if (phase == GamePhase.SCORING) {
            btnContinue.setVisibility(View.VISIBLE);
        } else if (phase == GamePhase.GAME_OVER) {
            Toast.makeText(this, "GAME OVER", Toast.LENGTH_LONG).show();
        }
    }

    private void renderTrick() {
        Card[] trick = controller.getCurrentTrick();

        // כרגע אין לנו תמונות קלפים אמיתיות → אם יש קלף נשחק, נציג back (בשלב הבא נשים תמונה אמיתית)
        setTrickSlot(trickP0, trick[0]);
        setTrickSlot(trickP1, trick[1]);
        setTrickSlot(trickP2, trick[2]);
        setTrickSlot(trickP3, trick[3]);
    }

    private void setTrickSlot(ImageView slot, Card c) {
        if (c == null) {
            slot.setAlpha(0.25f);
            slot.setImageResource(R.drawable.c_back);
        } else {
            slot.setAlpha(1.0f);
            slot.setImageResource(CardArt.resIdForCard(this, c));
        }
    }
    private void renderHand() {
        maybeRunDealAnimation();
        handContainer.removeAllViews();

        List<Card> hand = controller.getHumanHand();
        List<Integer> legal = controller.getLegalCardIndexesForHuman();

        for (int i = 0; i < hand.size(); i++) {

            Card card = hand.get(i);

            LinearLayout cardTile = new LinearLayout(this);
            cardTile.setOrientation(LinearLayout.VERTICAL);
            cardTile.setPadding(8, 0, 8, 0);

            ImageView img = new ImageView(this);
            img.setLayoutParams(new LinearLayout.LayoutParams(dp(70), dp(100)));
            img.setImageResource(CardArt.resIdForCard(this, card));
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);

            TextView label = new TextView(this);
            label.setText(card.toString()); // דיבוג: לראות איזה קלף זה באמת
            label.setTextColor(0xFFFFFFFF);
            label.setTextSize(12);

            boolean clickable =
                    controller.isHumanTurn()
                            && (
                            (controller.getPhase() == GamePhase.PLAYING_TRICK && legal.contains(i))
                                    || (controller.getPhase() == GamePhase.DISCARDING) // dealer discards any
                    );
            img.setAlpha(clickable ? 1.0f : 0.35f);

            final int index = i;
            img.setOnClickListener(v -> {
                if (!clickable) return;

                safeRun(() -> {
                    if (controller.getPhase() == GamePhase.DISCARDING) {
                        controller.discard(index);
                    } else {
                        controller.playHumanCard(index);
                    }
                });

                render();
            });

            cardTile.addView(img);
            cardTile.addView(label);
            handContainer.addView(cardTile);
        }
    }

    private void autoStepUntilHumanOrStop() {

        for (int safety = 0; safety < 30; safety++) {

            GamePhase phase = controller.getPhase();

            if (phase == GamePhase.GAME_OVER) return;

            if (controller.isHumanTurn()) return;

            if (phase == GamePhase.SCORING) {
                controller.continueAfterScoring();
                continue;
            }

            if (phase == GamePhase.ORDERING_TRUMP_ROUND1 || phase == GamePhase.ORDERING_TRUMP_ROUND2) {
                controller.pass();
                continue;
            }

            if (phase == GamePhase.DISCARDING) {
                controller.discard(0);
                continue;
            }

            if (phase == GamePhase.PLAYING_TRICK) {
                int current = controller.getCurrentPlayerIndex();
                playFirstLegalForCurrentPlayer(current);
                continue;
            }

            // DEALING/NOT_STARTED - אין לנו פעולה
            return;
        }
    }

    private void playFirstLegalForCurrentPlayer(int playerIndex) {
        List<Integer> legal = controller.computeLegalIndexes(playerIndex);
        if (!legal.isEmpty()) {
            controller.playCardAsPlayer(playerIndex, legal.get(0));
            return;
        }
        throw new IllegalStateException("No legal move found for player " + playerIndex);
    }

    private void safeRun(Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    private void maybeRunDealAnimation() {
        // תנאי ריצה: בתחילת סיבוב (ORDERING), ורק פעם אחת
        if (dealingAnimationRunning) return;

        GamePhase phase = controller.getPhase();
        if (phase != GamePhase.ORDERING_TRUMP_ROUND1) return; // בתחילת סיבוב

        // נשתמש במספר "יד" פנימי: tricksPlayedThisHand == 0 בתחילת סיבוב
        // אם אין לך getter לזה ב-controller, תגיד לי ואוסיף לך.
        int handNo = controller.getHandNumberMarker(); // נסביר עוד רגע

        if (handNo == lastSeenHandNumber) return;
        lastSeenHandNumber = handNo;

        runDealAnimation();
    }

    private void runDealAnimation() {
        dealingAnimationRunning = true;

        // בזמן אנימציה: תסתיר כפתורים כדי לא לבלבל
        btnPass.setVisibility(View.GONE);
        btnOrderUp.setVisibility(View.GONE);
        btnContinue.setVisibility(View.GONE);

        // בנה 5 סלוטים ביד (placeholder backs)
        buildHandPlaceholders(5);

        // קבע מיקום התחלה של flyingCard = מיקום deckPile
        int[] start = new int[2];
        deckPile.getLocationOnScreen(start);

        // נהפוך את flyingCard ל-visible ונמקם אותו
        flyingCard.setVisibility(View.VISIBLE);
        flyingCard.setX(start[0]);
        flyingCard.setY(start[1]);
        flyingCard.setAlpha(1f);
        flyingCard.setScaleX(1f);
        flyingCard.setScaleY(1f);

        // רצף חלוקה: 5 סבבים * 4 שחקנים = 20 "מסירות"
        // הסדר כאן הוא פשוט: P0,P1,P2,P3 כל פעם (כמו במנוע שלך שמחלק בלולאה לפי players)
        animateDealStep(0, 0, start);
    }

    private void animateDealStep(int round, int player, int[] startPos) {
        if (round == 5) {
            // סיום
            flyingCard.setVisibility(View.GONE);
            dealingAnimationRunning = false;

            // עכשיו נצייר יד אמיתית + הכל רגיל
            render();
            return;
        }

        // יעד לאן הקלף "נוחת"
        View target = getDealTargetView(player, round);

        int[] targetPos = new int[2];
        target.getLocationOnScreen(targetPos);

        // כל קלף עף כ-back (אפשר לשפר בהמשך לפליפ)
        flyingCard.setImageResource(R.drawable.c_back);

        // מחזיר את flyingCard להתחלה (deck)
        flyingCard.animate().cancel();
        flyingCard.setX(startPos[0]);
        flyingCard.setY(startPos[1]);

        flyingCard.animate()
                .x(targetPos[0])
                .y(targetPos[1])
                .setDuration(220)
                .withEndAction(() -> {

                    // כשהקלף "נחת":
                    if (player == 0) {
                        // ליד של האדם: נעדכן את הסלוט לתמונה אמיתית של הקלף
                        // round הוא אינדקס 0..4 = הקלף ה־(round)
                        try {
                            Card c = controller.getHumanHand().get(round);
                            ((ImageView) target).setImageResource(CardArt.resIdForCard(this, c));
                            target.setAlpha(1f);
                        } catch (Exception ignored) {}
                    } else {
                        // ליריבים נשאיר back (או רק ספירה)
                        target.setAlpha(1f);
                    }

                    // next player / next round
                    int nextPlayer = (player + 1) % 4;
                    int nextRound = round + (nextPlayer == 0 ? 1 : 0);

                    animateDealStep(nextRound, nextPlayer, startPos);
                })
                .start();
    }

    private void buildHandPlaceholders(int n) {
        handContainer.removeAllViews();
        for (int i = 0; i < n; i++) {
            ImageView slot = new ImageView(this);
            slot.setLayoutParams(new LinearLayout.LayoutParams(dp(70), dp(100)));
            slot.setImageResource(R.drawable.c_back);
            slot.setScaleType(ImageView.ScaleType.FIT_CENTER);
            slot.setAlpha(0.6f);

            LinearLayout wrapper = new LinearLayout(this);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.setPadding(8, 0, 8, 0);
            wrapper.addView(slot);

            handContainer.addView(wrapper);
        }
    }

    private View getDealTargetView(int player, int roundIndex) {
        // roundIndex 0..4
        if (player == 0) {
            // היד שלנו: היעד הוא הסלוט שיצרנו
            LinearLayout wrapper = (LinearLayout) handContainer.getChildAt(roundIndex);
            return (ImageView) wrapper.getChildAt(0);
        }

        // יריבים: ננחית "על הקלף back" שלהם (זה מספיק ויזואלית)
        if (player == 1) return p1Back;
        if (player == 2) return p2Back;
        return p3Back;
    }
}