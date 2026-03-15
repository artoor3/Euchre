package carmel.shubeli.euchre;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import android.widget.FrameLayout;
import engine_clean.core.GamePhase;
import engine_clean.model.Card;
import engine_clean.model.Suit;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
public class GameActivityClean extends AppCompatActivity {
    private static final String TAG = "GameActivityClean";
    private GameController controller;
    private ImageView trickP0, trickP1, trickP2, trickP3;
    private ImageView p1Back, p2Back, p3Back;
    private TextView p1Count, p2Count, p3Count;
    private TextView tvActionBanner;
    private TextView tvCaller, tvTrump, tvFinalCaller, tvfinalTrump;
    private View dealerChip;
    private View rootLayout;
    private LinearLayout handContainer;
    private boolean orderUpAnimRunning = false;
    private Button btnPass, btnOrderUp;
    private Button btnTrumpH, btnTrumpD, btnTrumpC, btnTrumpS;
    private ImageView deckPile;
    private final boolean collectingTrickRunning = false;
    private ImageView flyingCard;
    private ImageView upCardView;
    private boolean dealingAnimationRunning = false;
    private TextView tvScoreUs, tvScoreThem;
    private boolean scoringOverlayShown = false;
    private View scoringOverlay;
    private TextView tvScoringTitle, tvTricks, tvPoints, tvTotalScore;
    private Button btnContinueOverlay;
    private int lastSeenHandNumber = -1; // כדי לדעת שמתחיל סיבוב חדש
    private GameRunner runner;
    private boolean uiLocked = false;
    private View thinkingOverlay;
    private TextView tvThinking;
    private final Handler uiH = new Handler(Looper.getMainLooper());
    private int actionGen = 0;
    private static final long DEAL_START_DELAY_MS = 400;  // לפני שמתחילים את הקלפים לעוף
    private static final long DEAL_END_DELAY_MS = 700;  // אחרי שהחלוקה נגמרת לפני שמתחילים לשחק
    private FrameLayout gameOverOverlay;
    private TextView tvGameOverTitle;
    private TextView tvGameOverResult;
    private TextView tvGameOverReason;
    private Button btnNewGame;
    private boolean gameResultSaved = false;
    private ImageView imgPlayerAvatar;
    private TextView tvPlayerNameInGame;
    private Button btnMainMenu;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_clean);
        controller = new GameController();

        bindViews();
        loadPlayerProfileUi();
        bindClicks();
        setupRunner();
        rootLayout.post(() -> moveDealerChipTo(controller.getDealerIndex(), false));

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
        btnContinueOverlay = findViewById(R.id.btnContinueOverlay);
        btnTrumpH = findViewById(R.id.btnTrumpH);
        btnTrumpD = findViewById(R.id.btnTrumpD);
        btnTrumpC = findViewById(R.id.btnTrumpC);
        btnTrumpS = findViewById(R.id.btnTrumpS);
        flyingCard = findViewById(R.id.flyingCard);
        deckPile = findViewById(R.id.deckPile);
        upCardView = findViewById(R.id.upCardView);
        tvScoreUs = findViewById(R.id.tvScoreUs);
        tvScoreThem = findViewById(R.id.tvScoreThem);

        scoringOverlay = findViewById(R.id.scoringOverlay);
        tvScoringTitle = findViewById(R.id.tvScoringTitle);
        tvTrump = findViewById(R.id.tvTrump);
        tvTricks = findViewById(R.id.tvTricks);
        tvPoints = findViewById(R.id.tvPoints);
        tvTotalScore = findViewById(R.id.tvTotalScore);
        thinkingOverlay = findViewById(R.id.thinkingOverlay);
        tvThinking = findViewById(R.id.tvThinking);
        tvActionBanner = findViewById(R.id.tvActionBanner);
        tvCaller = findViewById(R.id.tvCaller);
        tvTrump = findViewById(R.id.tvTrump);

        tvFinalCaller = findViewById(R.id.tvFinalCaller);
        tvfinalTrump = findViewById(R.id.tvfinalTrump);
        rootLayout = findViewById(R.id.rootLayout);
        dealerChip = findViewById(R.id.dealerChip);
        gameOverOverlay = findViewById(R.id.gameOverOverlay);
        tvGameOverTitle = findViewById(R.id.tvGameOverTitle);
        tvGameOverResult = findViewById(R.id.tvGameOverResult);
        tvGameOverReason = findViewById(R.id.tvGameOverReason);
        btnNewGame = findViewById(R.id.btnNewGame);
        btnMainMenu = findViewById(R.id.btnMainMenu);
        imgPlayerAvatar = findViewById(R.id.imgPlayerAvatar);
        tvPlayerNameInGame = findViewById(R.id.tvPlayerNameInGame);
    }

    private void bindClicks() {

        btnPass.setOnClickListener(v -> {
            safeRun(() -> controller.pass());
            render();
            runner.requestPump("user_passes");
            renderHand();
        });
        btnOrderUp.setOnClickListener(v -> {
            if (orderUpAnimRunning) return;
            if (controller.getPhase() != GamePhase.ORDERING_TRUMP_ROUND1) return;

            orderUpRound1WithAnimation();
        });

        btnContinueOverlay.setOnClickListener(v -> {
            GamePhase phase = controller.getPhase();

            if (phase != GamePhase.SCORING) return;

            scoringOverlay.setVisibility(View.GONE);
            disableAllActions(false);

            safeRun(() -> controller.continueAfterScoring());
            dealerChip.setVisibility(View.VISIBLE);

            render();
            int[] scores = controller.getTeamScores(); // {team0, team1}
            if(scores[0] < 10 && scores[1] < 10) {
                runDealAnimation();
            }
            runner.requestPump("user_continue");
        });

        btnTrumpH.setOnClickListener(v -> {
            safeRun(() -> controller.orderUp(Suit.HEARTS));
            render();
        });
        btnTrumpD.setOnClickListener(v -> {
            safeRun(() -> controller.orderUp(Suit.DIAMONDS));
            render();
        });
        btnTrumpC.setOnClickListener(v -> {
            safeRun(() -> controller.orderUp(Suit.CLUBS));
            render();
        });
        btnTrumpS.setOnClickListener(v -> {
            safeRun(() -> controller.orderUp(Suit.SPADES));
            render();
        });
        btnNewGame.setOnClickListener(v -> {
            gameOverOverlay.setVisibility(View.GONE);
            gameResultSaved = false;
            Intent intent = new Intent(GameActivityClean.this, GameActivityClean.class);
            finish();
            startActivity(intent);        });
        btnMainMenu.setOnClickListener(v -> {
            Intent intent = new Intent(GameActivityClean.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    @SuppressLint("SetTextI18n")
    private void render() {
        GamePhase phase = controller.getPhase();
        if (maybeRunDealAnimation()) return;
        if (dealingAnimationRunning) return;
        if (phase != GamePhase.PLAYING_TRICK) {
            controller.clearLastCompletedTrick();
        }
        renderScoreBoard();
        // buttons off by default
        btnPass.setVisibility(View.GONE);
        btnOrderUp.setVisibility(View.GONE);
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
        } else if (phase == GamePhase.SCORING) {
            if (!scoringOverlayShown) {
                scoringOverlayShown = true;
                showScoringOverlay();
            }
        } else {
            scoringOverlayShown = false;
            scoringOverlay.setVisibility(View.GONE);
        }
        if (phase == GamePhase.GAME_OVER) {
            Log.d(TAG, "render(): reached GAME_OVER");
            showGameOverOverlay();
        }
        renderUpCard();
        if (controller.getUpCard() != null) {
            upCardView.setImageResource(
                    CardArt.resIdForCard(this, controller.getUpCard())
            );
        }
    }

    private void renderTrick() {
        if (collectingTrickRunning) return;
        Card[] trick = controller.getTrickForUi();
        setTrickSlot(trickP0, trick[0]);
        setTrickSlot(trickP1, trick[1]);
        setTrickSlot(trickP2, trick[2]);
        setTrickSlot(trickP3, trick[3]);
    }

    private void setTrickSlot(ImageView slot, Card c) {
        if (c == null) {
            slot.setTag(null);
            slot.setImageDrawable(null);
            slot.setAlpha(0f);
            slot.setVisibility(View.INVISIBLE);
        } else {
            slot.setTag(c);
            slot.setAlpha(1f);
            slot.setVisibility(View.VISIBLE);
            slot.setImageResource(CardArt.resIdForCard(this, c));
        }
    }

    private void renderHand() {
        handContainer.removeAllViews();

        List<Card> hand = controller.getHumanHand();
        List<Integer> legalPlay = controller.getLegalCardIndexesForHuman();

        // דיסקארד חוקי רק אם אנחנו באמת במצב הזה
        List<Integer> legalDiscard = controller.isHumanDealerAndMustDiscard()
                ? controller.getLegalDiscardIndexesForDealer()
                : java.util.Collections.emptyList();

        for (int i = 0; i < hand.size(); i++) {
            final int index = i; // ✅ חובה

            Card card = hand.get(i);

            LinearLayout cardTile = new LinearLayout(this);
            cardTile.setOrientation(LinearLayout.VERTICAL);
            cardTile.setPadding(dp(6), 0, dp(6), 0);

            ImageView img = new ImageView(this);
            img.setLayoutParams(new LinearLayout.LayoutParams(dp(70), dp(100)));
            img.setImageResource(CardArt.resIdForCard(this, card));
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // (אופציונלי) label לדיבוג
            TextView label = new TextView(this);
            label.setText(card.toString());
            label.setTextColor(0xFFFFFFFF);
            label.setTextSize(12);

            boolean clickablePlay = !uiLocked
                    && controller.isHumanTurn()
                    && controller.getPhase() == GamePhase.PLAYING_TRICK
                    && legalPlay.contains(index);

            boolean clickableDiscard = !uiLocked
                    && controller.isHumanDealerAndMustDiscard()
                    && legalDiscard.contains(index);

            boolean clickable = clickablePlay || clickableDiscard;
            img.setAlpha(clickable ? 1.0f : 0.35f);

            img.setOnClickListener(v -> {
                if (!clickable) return;

                if (clickableDiscard) {
                    // ✅ לשמור על ה"טון": קלף אמיתי טס מהמקום שנלחץ
                    animateDiscardToDeck(index, img); // הפונקציה שלך כבר עושה render בסוף
                    // אבל כדי שה-AI ימשיך אחרי הדיסקארד:
                    // נשים kick קטן בסוף האנימציה (תראה תיקון קטן למטה)
                    return;
                }

                // play
                safeRun(() -> controller.playHumanCard(index));
                render();
                runner.requestPump("render pump");
            });

            cardTile.addView(img);
            // אם בא לך בלי דיבוג — תמחק את שתי השורות האלו:
            // cardTile.addView(label);

            handContainer.addView(cardTile);
        }
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

    private boolean maybeRunDealAnimation() {
        // תנאי ריצה: בתחילת סיבוב (ORDERING), ורק פעם אחת
        if (dealingAnimationRunning) return false;

        GamePhase phase = controller.getPhase();
        if (phase != GamePhase.ORDERING_TRUMP_ROUND1) return false; // בתחילת סיבוב

        // נשתמש במספר "יד" פנימי: tricksPlayedThisHand == 0 בתחילת סיבוב
        // אם אין לך getter לזה ב-controller, תגיד לי ואוסיף לך.
        int handNo = controller.getHandNumberMarker(); // נסביר עוד רגע

        if (handNo == lastSeenHandNumber) return false;
        lastSeenHandNumber = handNo;
        renderScoreBoard();
        runDealAnimation();
        return true;
    }

    @SuppressLint("SetTextI18n")
    private void runDealAnimation() {
        View root = findViewById(R.id.rootLayout);
        if (root == null) return;
        tvCaller.setText("Caller: - " );
        root.post(() -> {
            int[] start = centerInRoot(deckPile);
            if (start == null) {
                // אם עדיין אין layout מוכן, ננסה שוב עוד רגע
                root.postDelayed(this::runDealAnimation, 60);
                return;
            }

            dealingAnimationRunning = true;

            btnPass.setVisibility(View.GONE);
            btnOrderUp.setVisibility(View.GONE);

            buildHandPlaceholders();

            flyingCard.setVisibility(View.VISIBLE);
            flyingCard.setX(start[0]);
            flyingCard.setY(start[1]);
            flyingCard.setAlpha(1f);
            flyingCard.setScaleX(1f);
            flyingCard.setScaleY(1f);

            uiH.postDelayed(() -> animateDealStep(0, 0), DEAL_START_DELAY_MS);
        });
    }

    private void animateDealStep(int round, int player) {
        moveDealerChipTo(controller.getDealerIndex(), true);
        if (round == 5) {
            flyingCard.setVisibility(View.GONE);
            dealingAnimationRunning = false;

            render(); // מצב יציב אחרי חלוקה

            uiH.postDelayed(() -> {
                        render();
                        applyUiLocked(false, null); // ✅ לשחרר רק פה
                        runner.requestPump("deal pump");
                    },
                    DEAL_END_DELAY_MS);

            return;
        }

        View target = getDealTargetView(player, round);

        int[] start = centerInRoot(deckPile);       // ✅ תמיד מתחילים מה-deck ב-root coords
        int[] end = centerInRoot(target);         // ✅ יעד ב-root coords
        if (target == null) {
            View root = findViewById(R.id.rootLayout);
            if (root != null) {
                root.postDelayed(() -> animateDealStep(round, player), 60);
            }
            return;
        }

        flyingCard.setImageResource(R.drawable.c_back);

        flyingCard.animate().cancel();
        assert start != null;
        flyingCard.setX(start[0]);
        flyingCard.setY(start[1]);

        assert end != null;
        flyingCard.animate()
                .x(end[0])
                .y(end[1])
                .setDuration(220)
                .withEndAction(() -> {

                    if (player == 0) {
                        try {
                            Card c = controller.getHumanHand().get(round);
                            ((ImageView) target).setImageResource(CardArt.resIdForCard(this, c));
                            target.setAlpha(1f);
                        } catch (Exception ignored) {
                        }
                    } else {
                        target.setAlpha(1f);
                    }

                    int nextPlayer = (player + 1) % 4;
                    int nextRound = round + (nextPlayer == 0 ? 1 : 0);

                    animateDealStep(nextRound, nextPlayer);
                })
                .start();
    }

    private void buildHandPlaceholders() {
        handContainer.removeAllViews();
        for (int i = 0; i < 5; i++) {
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
            return wrapper.getChildAt(0);
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
            assert start != null;
            flyingCard.setX(start[0]);
            flyingCard.setY(start[1]);

            flyingCard.setVisibility(View.VISIBLE);

            // רק תנועה — בלי flip
            assert end != null;
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
                        orderUpAnimRunning = false;
                        applyUiLocked(false, null);
                        render();
                        runner.requestPump("user_orderup_done");

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

        // להכין flyingCard בלי קפיצות
        flyingCard.animate().cancel();
        flyingCard.setVisibility(View.INVISIBLE);
        flyingCard.setAlpha(1f);
        flyingCard.setRotationY(0f);

        flyingCard.setImageResource(CardArt.resIdForCard(this, chosen));
        assert start != null;
        flyingCard.setX(start[0]);
        flyingCard.setY(start[1]);

        flyingCard.setVisibility(View.VISIBLE);

        assert end != null;
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
                    render();
                    runner.requestPump("user_discard");
                })
                .start();
    }

    private void animateCardFromTo(View from, View to, int cardDrawableRes, Runnable onEnd) {
        if (dealingAnimationRunning) {
            if (onEnd != null) onEnd.run();
            return;
        }

        View root = findViewById(R.id.rootLayout);
        if (root == null) {
            if (onEnd != null) onEnd.run();
            return;
        }

        root.post(() -> {
            int[] start = centerInRoot(from);
            int[] end = centerInRoot(to);

            // אם אחד המיקומים לא תקין - לא לעשות "שיגור לפינה"
            if (start == null || end == null) {
                if (onEnd != null) onEnd.run();
                return;
            }

            dealingAnimationRunning = true;

            flyingCard.animate().cancel();
            flyingCard.clearAnimation();

            flyingCard.setImageResource(cardDrawableRes);
            flyingCard.setAlpha(1f);
            flyingCard.setRotation(0f);
            flyingCard.setRotationX(0f);
            flyingCard.setRotationY(0f);

            flyingCard.setX(start[0]);
            flyingCard.setY(start[1]);
            flyingCard.setVisibility(View.VISIBLE);
            flyingCard.bringToFront();

            flyingCard.animate()
                    .x(end[0])
                    .y(end[1])
                    .setDuration(260)
                    .withEndAction(() -> {
                        flyingCard.animate().cancel();
                        flyingCard.setVisibility(View.GONE);
                        dealingAnimationRunning = false;

                        if (onEnd != null) onEnd.run();
                    })
                    .start();
        });
    }

    private int[] posInRoot(View v) {
        View rootView = findViewById(R.id.rootLayout);
        if (v == null || rootView == null) return null;
        if (v.getWidth() == 0 || v.getHeight() == 0) return null;

        int[] root = new int[2];
        int[] loc = new int[2];

        rootView.getLocationInWindow(root);
        v.getLocationInWindow(loc);

        return new int[]{loc[0] - root[0], loc[1] - root[1]};
    }
    private int[] centerInRoot(View v) {
        View root = findViewById(R.id.rootLayout);
        if (v == null || root == null) return null;

        if (v.getWidth() == 0 || v.getHeight() == 0) return null;
        if (root.getWidth() == 0 || root.getHeight() == 0) return null;

        int[] vLoc = new int[2];
        int[] rootLoc = new int[2];

        v.getLocationInWindow(vLoc);
        root.getLocationInWindow(rootLoc);

        int flyingW = flyingCard.getWidth() > 0 ? flyingCard.getWidth() : dp(70);
        int flyingH = flyingCard.getHeight() > 0 ? flyingCard.getHeight() : dp(100);

        int cx = (vLoc[0] - rootLoc[0]) + v.getWidth() / 2 - flyingW / 2;
        int cy = (vLoc[1] - rootLoc[1]) + v.getHeight() / 2 - flyingH / 2;

        return new int[]{cx, cy};
    }

    @SuppressLint("SetTextI18n")
    private void renderScoreBoard() {
        int[] scores = controller.getTeamScores(); // {team0, team1}
        Suit trump = controller.getTrumpSuit();
        String trumpText = (trump == null) ? "-" : trump.toString(); // או symbol

        tvTrump.setText("Trump: " + trumpText);
        tvScoreUs.setText("Your team: " + scores[0]);
        tvScoreThem.setText("Other team: " + scores[1]);
    }

    @SuppressLint("SetTextI18n")
    private void showScoringOverlay() {
        scoringOverlay.setVisibility(View.VISIBLE);
        dealerChip.setVisibility(View.GONE);
        // נתונים מהמנוע/קונטרולר
        int usTricks = controller.getTricksTeam0();
        int themTricks = controller.getTricksTeam1();
        Suit trump = controller.getTrumpSuit();

        // חישוב "כמה נקודות יקבלו" (לפי הכללים שלך)
        int[] gain = computePointsThisHand(usTricks, themTricks);
        int gainedUs = gain[0];
        int gainedThem = gain[1];
        int[] total = controller.getTeamScores();
        int totalUsAfter = total[0] + gainedUs;     // ✅ חישוב לתצוגה בלבד
        int totalThemAfter = total[1] + gainedThem; // ✅ חישוב לתצוגה בלבד
        renderTrumpAndCaller();
        tvScoringTitle.setText("Hand Summary");
        tvTrump.setText("Trump: " + (trump == null ? "-" : trump.toString()));
        tvTricks.setText("Tricks - Us: " + usTricks + "  Them: " + themTricks);
        tvPoints.setText("Points this hand - Us: " + gainedUs + "  Them: " + gainedThem);
        tvTotalScore.setText("Total - Us: " + totalUsAfter + "  Them: " + totalThemAfter);

        // בזמן overlay רק Continue פעיל
        disableAllActions(true);
    }

    private void disableAllActions(boolean disabled) {
        btnPass.setEnabled(!disabled);
        btnOrderUp.setEnabled(!disabled);
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
            if (callerTeam == 0) gainedUs += 1;
            else gainedThem += 1;
        } else if (callerTricks == 5) {
            if (callerTeam == 0) gainedUs += 2;
            else gainedThem += 2;
        } else {
            // euchred
            if (otherTeam == 0) gainedUs += 2;
            else gainedThem += 2;
        }

        return new int[]{gainedUs, gainedThem};
    }

    private void setupRunner() {
        runner = new GameRunner(controller, new GameRunner.Callbacks() {

            @Override
            public void setLocked(boolean locked, String message) {
                applyUiLocked(locked, message);
            }

            @Override
            public void renderNow() {
                render();
                renderTrumpAndCaller();
                moveDealerChipTo(controller.getDealerIndex(), false);
            }

            @Override
            public void animateCard(View from, View to, Runnable onEnd) {
                // AI אנימציה תמיד עם גב קלף
                animateCardFromTo(from, to, R.drawable.c_back, onEnd);
            }

            @Override
            public View getPlayerAnchor(int playerIndex) {
                return getAnchorForPlayer(playerIndex);
            }

            @Override
            public View getTrickSlot(int playerIndex) {
                return getTrickSlotForPlayer(playerIndex);
            }

            @Override
            public View getDeckPile() {
                return deckPile;
            }

            @Override
            public void showAction(String message) {
                showActionMessage(message);
            }

            public boolean isUiBusy() {
                return dealingAnimationRunning || collectingTrickRunning;
            }

        });
    }

    private void applyUiLocked(boolean locked, String message) {
        uiLocked = locked;

        if (locked) {
            thinkingOverlay.setVisibility(View.VISIBLE);
            tvThinking.setText(message == null ? "Thinking…" : message);
            disableAllActions(true);
        } else {
            thinkingOverlay.setVisibility(View.GONE);
            disableAllActions(false);
        }
    }

    private View getAnchorForPlayer(int playerIndex) {
        if (playerIndex == 1) return p1Back;
        if (playerIndex == 2) return p2Back;
        if (playerIndex == 3) return p3Back;
        return p1Back; // fallback
    }

    private View getTrickSlotForPlayer(int playerIndex) {
        if (playerIndex == 0) return trickP0;
        if (playerIndex == 1) return trickP1;
        if (playerIndex == 2) return trickP2;
        if (playerIndex == 3) return trickP3;
        return trickP0;
    }

    private void showActionMessage(String msg) {
        int token = ++actionGen;
        tvActionBanner.setText(msg);
        tvActionBanner.setVisibility(View.VISIBLE);

        uiH.postDelayed(() -> {
            if (token != actionGen) return;
            tvActionBanner.setVisibility(View.GONE);
        }, 1600);
    }
    private String playerName(int idx) {
        // מינימום כדי שיתקמפל. תשנה למה שמתאים אצלך.
        if (idx == 0) return "You"; // אם 0 זה האדם
        return "P" + idx;
    }

    private String suitToSymbol(engine_clean.model.Suit s) {
        if (s == null) return "-";
        switch (s) {
            case HEARTS:
                return "♥";
            case DIAMONDS:
                return "♦";
            case CLUBS:
                return "♣";
            case SPADES:
                return "♠";
            default:
                return s.toString();
        }
    }

    private void renderTrumpAndCaller() {
        // צריך שה-controller/engine יחשפו את שני הערכים האלה:
        // controller.getTrumpSuit()
        // controller.getTrumpCaller()

        engine_clean.model.Suit trump = controller.getTrumpSuit();
        int caller = controller.getTrumpCaller(); // -1 אם לא נקבע עדיין

        String trumpText = "Trump: " + suitToSymbol(trump);
        String callerText = "Caller: " + (caller < 0 ? "-" : playerName(caller));

        // Scoreboard (למעלה)
        if (tvCaller != null) tvCaller.setText(callerText);

        // Scoring overlay
        if (tvTrump != null) tvTrump.setText(trumpText);
        if (tvfinalTrump != null) tvfinalTrump.setText(trumpText);

        if (tvFinalCaller != null) tvFinalCaller.setText(callerText);
    }

    private View getDealerAnchorView(int playerIndex) {
        switch (playerIndex) {
            case 0:
                return findViewById(R.id.handScroll); // או משהו אצלך שמייצג P0
            case 1:
                return findViewById(R.id.p1Box);
            case 2:
                return findViewById(R.id.p2Box);
            case 3:
                return findViewById(R.id.p3Box);
            default:
                return findViewById(R.id.scoreBoard);
        }
    }

    private void moveDealerChipTo(int dealerIndex, boolean animate) {
        View anchor = getDealerAnchorView(dealerIndex);
        if (anchor == null) return;

        // לוודא שיש מידות
        if (anchor.getWidth() == 0 || dealerChip.getWidth() == 0) {
            rootLayout.post(() -> moveDealerChipTo(dealerIndex, animate));
            return;
        }

        int[] c = centerInRoot(anchor);

        assert c != null;
        float targetX = c[0] - dealerChip.getWidth() / 2f;
        float targetY = c[1] - dealerChip.getHeight() / 2f;

        if (!animate) {
            dealerChip.setX(targetX);
            dealerChip.setY(targetY);
            dealerChip.setAlpha(1f);
            dealerChip.setScaleX(1f);
            dealerChip.setScaleY(1f);
            return;
        }

        dealerChip.animate().cancel();
        dealerChip.animate()
                .x(targetX)
                .y(targetY)
                .setDuration(300)
                .start();
    }
    @SuppressLint("SetTextI18n")
    private void showGameOverOverlay() {
        dealerChip.setVisibility(View.GONE);
        Log.d(TAG, "showGameOverOverlay(): called");

        int[] scores = controller.getTeamScores();
        int us = scores[0];
        int them = scores[1];

        boolean weWon = us > them;
        Log.d(TAG, "showGameOverOverlay(): us=" + us + ", them=" + them + ", weWon=" + weWon + ", gameResultSaved=" + gameResultSaved);

        if (!gameResultSaved) {
            saveMatchToFirebase(weWon, us, them);
            gameResultSaved = true;
        }
        else {
            Log.d(TAG, "showGameOverOverlay(): save skipped because gameResultSaved=true");
        }

        tvGameOverTitle.setText("GAME OVER");
        tvGameOverResult.setText(weWon ? "YOU WON" : "YOU LOST");
        tvGameOverReason.setText("Final score: Us " + us + " - Them " + them);

        gameOverOverlay.setVisibility(View.VISIBLE);
    }
    private void saveMatchToFirebase(boolean won, int usScore, int themScore) {
        Log.d(TAG, "saveMatchToFirebase(): called with won=" + won + ", usScore=" + usScore + ", themScore=" + themScore);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "saveMatchToFirebase(): currentUser is NULL");
            return;
        }

        String uid = user.getUid();
        Log.d(TAG, "saveMatchToFirebase(): user uid=" + uid);

        PrefsManager prefs = new PrefsManager(this);
        String playerName = prefs.getPlayerName();
        String result = won ? "WIN" : "LOSS";

        Log.d(TAG, "saveMatchToFirebase(): playerName=" + playerName + ", result=" + result);

        MatchResult match = new MatchResult(
                playerName,
                result,
                usScore,
                themScore,
                System.currentTimeMillis()
        );

        String key = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("matches")
                .push()
                .getKey();

        Log.d(TAG, "saveMatchToFirebase(): generated key=" + key);

        if (key == null) {
            Log.e(TAG, "saveMatchToFirebase(): key is null");
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("matches")
                .child(key)
                .setValue(match)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "saveMatchToFirebase(): SUCCESS");
                    Toast.makeText(this, "Match saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveMatchToFirebase(): FAILED -> " + e.getMessage(), e);
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    private void loadPlayerProfileUi() {
        PrefsManager prefs = new PrefsManager(this);

        String playerName = prefs.getPlayerName();
        tvPlayerNameInGame.setText(playerName);

        String cameraPath = prefs.getAvatarCameraPath();
        if (cameraPath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(cameraPath);
            if (bitmap != null) {
                imgPlayerAvatar.setImageBitmap(bitmap);
                return;
            }
        }

        String uri = prefs.getAvatarUri();
        if (uri != null) {
            try {
                imgPlayerAvatar.setImageURI(Uri.parse(uri));
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        imgPlayerAvatar.setImageResource(R.mipmap.ic_launcher);
    }
}