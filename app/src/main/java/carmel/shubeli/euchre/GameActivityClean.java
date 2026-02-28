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


    private ImageView trickP0, trickP1, trickP2, trickP3;
    private ImageView p1Back, p2Back, p3Back;
    private TextView p1Count, p2Count, p3Count;

    private LinearLayout handContainer;
    private boolean orderUpAnimRunning = false;
    private Button btnPass, btnOrderUp, btnContinue, btnAuto;
    private Button btnTrumpH, btnTrumpD, btnTrumpC, btnTrumpS;
    private ImageView deckPile;
    private ImageView flyingCard;
    private ImageView upCardView;
    private boolean dealingAnimationRunning = false;
    private TextView tvScoreUs, tvScoreThem;
    private boolean scoringOverlayShown = false;
    private View scoringOverlay;
    private TextView tvScoringTitle, tvTrump, tvTricks, tvPoints, tvTotalScore;
    private Button btnContinueOverlay;
    private int[] lastTotalScore = new int[]{0,0};
    private boolean scoringOverlayRunning = false;
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
        btnContinueOverlay = findViewById(R.id.btnContinueOverlay);
        btnAuto = findViewById(R.id.btnAuto);
        btnTrumpH = findViewById(R.id.btnTrumpH);
        btnTrumpD = findViewById(R.id.btnTrumpD);
        btnTrumpC = findViewById(R.id.btnTrumpC);
        btnTrumpS = findViewById(R.id.btnTrumpS);
        deckPile = findViewById(R.id.deckPile);
        flyingCard = findViewById(R.id.flyingCard);
        upCardView = findViewById(R.id.upCardView);
        tvScoreUs = findViewById(R.id.tvScoreUs);
        tvScoreThem = findViewById(R.id.tvScoreThem);

        scoringOverlay = findViewById(R.id.scoringOverlay);
        tvScoringTitle = findViewById(R.id.tvScoringTitle);
        tvTrump = findViewById(R.id.tvTrump);
        tvTricks = findViewById(R.id.tvTricks);
        tvPoints = findViewById(R.id.tvPoints);
        tvTotalScore = findViewById(R.id.tvTotalScore);
    }

    private void bindClicks() {

        btnPass.setOnClickListener(v -> {
            safeRun(() -> controller.pass());
            render();
            renderHand();
        });

        btnOrderUp.setOnClickListener(v -> {
            if (orderUpAnimRunning) return;
            if (controller.getPhase() != GamePhase.ORDERING_TRUMP_ROUND1) return;
            orderUpRound1WithAnimation();
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
            if (controller.getPhase() != GamePhase.SCORING) return;

            safeRun(() -> controller.continueAfterScoring());

            scoringOverlay.setVisibility(View.GONE);
            disableAllActions(false);

            render();
        });
        btnContinueOverlay.setOnClickListener(v -> {
            if (controller.getPhase() != GamePhase.SCORING) return;

            scoringOverlay.setVisibility(View.GONE);
            disableAllActions(false);

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
        GamePhase phase = controller.getPhase();
        renderScoreBoard();
        // buttons off by default
        btnPass.setVisibility(View.GONE);
        btnOrderUp.setVisibility(View.GONE);
        btnContinue.setVisibility(View.GONE);
        upCardView.setVisibility(View.GONE);

        if (phase == GamePhase.ORDERING_TRUMP_ROUND1 && controller.isHumanTurn()) {
            btnPass.setVisibility(View.VISIBLE);
            btnOrderUp.setVisibility(View.VISIBLE);
        }
        renderUpCard();
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
        }
        else if (phase == GamePhase.SCORING) {
            btnContinue.setVisibility(View.VISIBLE);
            if (!scoringOverlayShown) {
                scoringOverlayShown = true;
                showScoringOverlay();
            }
        }
        else {
            scoringOverlayShown = false;
            scoringOverlay.setVisibility(View.GONE);
        }
        if (phase == GamePhase.GAME_OVER) {
            Toast.makeText(this, "GAME OVER", Toast.LENGTH_LONG).show();
        }
        renderUpCard();
        if (controller.getUpCard() != null) {
            upCardView.setImageResource(
                    CardArt.resIdForCard(this, controller.getUpCard())
            );
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
                        animateDiscardToDeck(index, img);
                    }
                    else {
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
    private void renderUpCard() {
        GamePhase p = controller.getPhase();

        if (p == GamePhase.ORDERING_TRUMP_ROUND1) {
            upCardView.setVisibility(View.VISIBLE);
            upCardView.setImageResource(CardArt.resIdForCard(this, controller.getUpCard()));
            return;
        }

        upCardView.setVisibility(View.GONE);
    }
    private View getDealerTargetViewForPickup() {
        int dealer = controller.getDealerIndex();

        // אנחנו מנחיתים על גב הקלפים של הדילר ב-UI
        if (dealer == 0) {
            // היד שלך: ננחית "באמצע" של היד (או בסלוט האחרון של placeholder אם יש)
            if (handContainer.getChildCount() > 0) {
                int idx = Math.max(0, handContainer.getChildCount() - 1);
                LinearLayout wrapper = (LinearLayout) handContainer.getChildAt(idx);
                return wrapper.getChildAt(0); // ImageView של הקלף
            }
            return handContainer; // fallback
        }

        if (dealer == 1) return p1Back;
        if (dealer == 2) return p2Back;
        return p3Back;
    }
    private void orderUpRound1WithAnimation() {
        if (controller.getPhase() != GamePhase.ORDERING_TRUMP_ROUND1) return;
        if (dealingAnimationRunning) return;

        dealingAnimationRunning = true;
        btnOrderUp.setEnabled(false);
        btnPass.setEnabled(false);
        btnAuto.setEnabled(false);

        View root = findViewById(R.id.rootLayout);

        root.post(() -> {
            // יעד: הדילר (היכן "נוחת" הקלף)
            View target = getDealerTargetViewForPickup();

            int[] start = posInRoot(upCardView);
            int[] end = posInRoot(target);

            // hide upcard on deck immediately (כדי שלא יישאר על הערימה בזמן טיסה)
            upCardView.setVisibility(View.GONE);

            // הכנת flyingCard לפני שמראים אותו (כדי שלא יקפוץ מהמרכז)
            flyingCard.animate().cancel();
            flyingCard.setVisibility(View.INVISIBLE);
            flyingCard.setRotationY(0f);
            flyingCard.setAlpha(1f);

            flyingCard.setImageResource(CardArt.resIdForCard(this, controller.getUpCard()));
            flyingCard.setX(start[0]);
            flyingCard.setY(start[1]);

            flyingCard.setVisibility(View.VISIBLE);

            // רק תנועה — בלי flip
            flyingCard.animate()
                    .x(end[0])
                    .y(end[1])
                    .setDuration(320)
                    .withEndAction(() -> {
                        // לבצע orderUp רק פעם אחת ובטוח רק אם עדיין ב-round1
                        if (controller.getPhase() == GamePhase.ORDERING_TRUMP_ROUND1) {
                            safeRun(() -> controller.orderUp(controller.getUpCard().getSuit()));
                        }

                        flyingCard.animate().cancel();
                        flyingCard.setVisibility(View.GONE);

                        dealingAnimationRunning = false;
                        btnOrderUp.setEnabled(true);
                        btnPass.setEnabled(true);
                        btnAuto.setEnabled(true);

                        render();
                    })
                    .start();
        });
    }
    private void animateDiscardToDeck(int handIndex, ImageView clickedImg) {
        if (dealingAnimationRunning) return;

        // קודם כל מחשבים מיקום התחלה מיד (לפני כל שינוי UI)
        int[] start = centerInRoot(clickedImg);

        // יעד: מרכז ה-deckPile
        int[] end = centerInRoot(deckPile);

        // לוקחים את הקלף לפי אינדקס (היד עוד לא השתנתה בשלב הזה)
        Card chosen = controller.getHumanHand().get(handIndex);

        dealingAnimationRunning = true;
        btnAuto.setEnabled(false);

        // להכין flyingCard בלי קפיצות
        flyingCard.animate().cancel();
        flyingCard.setVisibility(View.INVISIBLE);
        flyingCard.setAlpha(1f);
        flyingCard.setRotationY(0f);

        flyingCard.setImageResource(CardArt.resIdForCard(this, chosen));
        flyingCard.setX(start[0]);
        flyingCard.setY(start[1]);

        flyingCard.setVisibility(View.VISIBLE);

        flyingCard.animate()
                .x(end[0])
                .y(end[1])
                .setDuration(260)
                .withEndAction(() -> {
                    // רק עכשיו מבצעים discard במנוע
                    safeRun(() -> controller.discard(handIndex));

                    flyingCard.animate().cancel();
                    flyingCard.setVisibility(View.GONE);

                    dealingAnimationRunning = false;
                    btnAuto.setEnabled(true);

                    render();
                })
                .start();
    }
    private int[] posInRoot(View v) {
        int[] root = new int[2];
        int[] loc = new int[2];

        View rootView = findViewById(R.id.rootLayout);
        rootView.getLocationOnScreen(root);
        v.getLocationOnScreen(loc);

        return new int[]{ loc[0] - root[0], loc[1] - root[1] };
    }
    private int[] centerInRoot(View v) {
        android.graphics.Rect r = new android.graphics.Rect();
        boolean ok = v.getGlobalVisibleRect(r); // הכי יציב עם scroll

        // אם משום מה לא visible (נדיר), fallback ל-0,0
        if (!ok) return new int[]{0, 0};

        // root layout של המסך
        View root = findViewById(R.id.rootLayout);
        int[] rootLoc = new int[2];
        root.getLocationOnScreen(rootLoc);

        // מרכז ה-View ביחס ל-root
        int cx = (r.left + r.right) / 2 - rootLoc[0];
        int cy = (r.top + r.bottom) / 2 - rootLoc[1];

        // כדי שהקלף יתיישב עם הפינה העליונה-שמאלית של flyingCard
        cx -= dp(70) / 2;
        cy -= dp(100) / 2;

        return new int[]{cx, cy};
    }
    private void renderScoreBoard() {
        int[] scores = controller.getTeamScores(); // {team0, team1}

        // בהנחה Team 0 = השחקן האנושי (Player0)
        tvScoreUs.setText("Your team: " + scores[0]);
        tvScoreThem.setText("Other team: " + scores[1]);
    }
    private void maybeShowScoringOverlay() {
        if (scoringOverlayRunning) return;

        scoringOverlayRunning = true;

        int[] scoresNow = controller.getTeamScores();
        int usGained = scoresNow[0] - lastTotalScore[0];
        int themGained = scoresNow[1] - lastTotalScore[1];

        lastTotalScore[0] = scoresNow[0];
        lastTotalScore[1] = scoresNow[1];

        scoringOverlay.setVisibility(View.VISIBLE);

        tvScoringTitle.setText("Hand Summary");
        tvTrump.setText("Trump: " + controller.getTrumpSuit()); // אם יש getter; אם לא תגיד לי
        tvTricks.setText("Tricks - Us: " + controller.getTricksTeam0() + "  Them: " + controller.getTricksTeam1());
        tvPoints.setText("Points this hand - Us: " + usGained + "  Them: " + themGained);
        tvTotalScore.setText("Total - Us: " + scoresNow[0] + "  Them: " + scoresNow[1]);

        // אחרי 2 שניות ממשיכים אוטומטית
        scoringOverlay.postDelayed(() -> {
            scoringOverlay.setVisibility(View.GONE);
            scoringOverlayRunning = false;

            safeRun(() -> controller.continueAfterScoring()); // זה מה שמתחיל יד חדשה
            render();

        }, 2000);
    }
    private void showScoringOverlay() {
        scoringOverlay.setVisibility(View.VISIBLE);

        // נתונים מהמנוע/קונטרולר
        int usTricks = controller.getTricksTeam0();
        int themTricks = controller.getTricksTeam1();
        Suit trump = controller.getTrumpSuit();

        // חישוב "כמה נקודות יקבלו" (לפי הכללים שלך)
        int[] gain = computePointsThisHand(usTricks, themTricks);
        int gainedUs = gain[0];
        int gainedThem = gain[1];

        int[] total = controller.getTeamScores();
        total[0] += gain[0];
        total[1] += gain[1];

        tvScoringTitle.setText("Hand Summary");
        tvTrump.setText("Trump: " + (trump == null ? "-" : trump.toString()));
        tvTricks.setText("Tricks - Us: " + usTricks + "  Them: " + themTricks);
        tvPoints.setText("Points this hand - Us: " + gainedUs + "  Them: " + gainedThem);
        tvTotalScore.setText("Total - Us: " + total[0] + "  Them: " + total[1]);

        // בזמן overlay רק Continue פעיל
        disableAllActions(true);
        btnContinue.setEnabled(true);
        btnContinue.setVisibility(View.VISIBLE);
    }
    private void showScoringOverlayIfAny() {
        GameController.HandSummary s = controller.consumeLastHandSummary();
        if (s == null) {
            render();
            return;
        }

        scoringOverlay.setVisibility(View.VISIBLE);


// בזמן overlay: נועל הכל
        disableAllActions(true);

// אבל ה-Continue של ה-overlay נשאר פעיל
        btnContinueOverlay.setEnabled(true);
        btnContinueOverlay.setVisibility(View.VISIBLE);
    }
    private void disableAllActions(boolean disabled) {
        btnAuto.setEnabled(!disabled);
        btnPass.setEnabled(!disabled);
        btnOrderUp.setEnabled(!disabled);
        btnContinue.setEnabled(!disabled);
        btnTrumpH.setEnabled(!disabled);
        btnTrumpD.setEnabled(!disabled);
        btnTrumpC.setEnabled(!disabled);
        btnTrumpS.setEnabled(!disabled);
    }
    private int[] computePointsThisHand(int usTricks, int themTricks) {
        // מניחים: מי שקרא טראמפ יכול להיות us או them.
        // אם אתה רוצה גם להציג "מי קרא", תגיד לי ונוסיף.
        // כרגע רק נציג את הניקוד שבפועל יתקבל לפי תוצאות:
        //  - 3-4 => +1 לקבוצה שקראה
        //  - 5   => +2 לקבוצה שקראה
        //  - 0-2 => +2 ליריבה
        // כדי לחשב נכון צריך לדעת מי callerTeam. נוסיף את זה עכשיו:

        int callerTeam = controller.getCallerTeam(); // נצטרך getter קטן
        int otherTeam = (callerTeam == 0) ? 1 : 0;

        int callerTricks = (callerTeam == 0) ? usTricks : themTricks;

        int gainedUs = 0, gainedThem = 0;

        if (callerTricks >= 3 && callerTricks <= 4) {
            if (callerTeam == 0) gainedUs += 1; else gainedThem += 1;
        } else if (callerTricks == 5) {
            if (callerTeam == 0) gainedUs += 2; else gainedThem += 2;
        } else {
            // euchred
            if (otherTeam == 0) gainedUs += 2; else gainedThem += 2;
        }

        return new int[]{gainedUs, gainedThem};
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
}