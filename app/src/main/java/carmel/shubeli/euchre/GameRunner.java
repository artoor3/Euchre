package carmel.shubeli.euchre;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.List;

import engine_clean.ai.BasicAiPolicy;
import engine_clean.core.GamePhase;

/**
 * GameRunner responsibilities:
 * - Drive the game forward ONLY when it is an AI turn and UI is ready.
 * - Never spin / tight-loop.
 * - Never cancel a scheduled AI action just because a new pump happened.
 * - Schedule next pump ONLY when:
 *    (1) AI action completed (engine state changed), or
 *    (2) UI became ready after being busy.
 * Contract:
 * - Activity/UI should call requestPump("reason") after ANY user action or render/animation end.
 * - animateCard(...) MUST call onEnd when animation ends.
 * - isUiBusy() should be true while any animation/overlay/lock is active.
 */
public class GameRunner {

    private static final String TAG = "EUCHRE_DEBUG";

    public interface Callbacks {
        void setLocked(boolean locked, String message);
        void renderNow();
        void animateCard(View from, View to, Runnable onEnd);
        View getPlayerAnchor(int playerIndex);
        View getTrickSlot(int playerIndex);
        View getDeckPile();
        void showAction(String message);

        default boolean isUiBusy() {
            return false;
        }
    }

    private final GameController controller;
    private final Callbacks cb;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final BasicAiPolicy aiPolicy = new BasicAiPolicy();
    // --- scheduling guards ---
    private boolean pumpScheduled = false;
    private boolean aiPending = false;     // AI decision/action is scheduled or in-flight (including animation)
    private int aiToken = 0;               // cancels stale AI tasks ONLY when we explicitly want to cancel
    private long lastPumpAtMs = 0L;

    // Backoff for UI-busy retries (prevents spam if UI stays busy)
    private int uiBusyStreak = 0;

    // Tunables
    private static final int PUMP_DELAY_MS = 60;          // small settle delay
    private static final int AI_THINK_MS = 250;           // "thinking" delay
    private static final int UI_BUSY_BASE_MS = 120;       // retry base
    private static final int UI_BUSY_MAX_MS = 800;        // retry cap
    private static final int PUMP_THROTTLE_MIN_MS = 30;   // avoid accidental rapid-fire

    public GameRunner(GameController controller, Callbacks cb) {
        this.controller = controller;
        this.cb = cb;
    }

    /** Call this after any user action, and after any render/animation end (safe to call often). */
    public void requestPump(String reason) {
        if (pumpScheduled) {
            Log.d(TAG, "RUNNER.requestPump SKIP already scheduled reason=" + reason);
            return;
        }

        // Throttle super-fast bursts
        long now = android.os.SystemClock.uptimeMillis();
        long dt = now - lastPumpAtMs;
        int delay = PUMP_DELAY_MS;
        if (dt < PUMP_THROTTLE_MIN_MS) delay += (int) (PUMP_THROTTLE_MIN_MS - dt);

        pumpScheduled = true;
        main.postDelayed(() -> {
            pumpScheduled = false;
            lastPumpAtMs = android.os.SystemClock.uptimeMillis();
            pump(reason);
        }, delay);
    }

    // ---------------- core loop ----------------

    private void pump(String reason) {
        Snap before = snap();
        boolean uiBusy = cb.isUiBusy();
        Log.d(TAG, "RUNNER.pump ENTER reason=" + reason
                + " BEFORE " + before
                + " uiBusy=" + uiBusy
                + " aiPending=" + aiPending);
        // 1) GAME_OVER: פשוט לשחרר
        if (before.phase == GamePhase.GAME_OVER) {
            unlockAndRender();
            Log.d(TAG, "RUNNER.pump STOP: game_over");
            return;
        }
        // 2) SCORING: פה לא "סתם לעצור" - פה עושים איסוף קלפים ואז render
        // כדי לא להפעיל את זה 100 פעם, צריך guard.
        if (before.phase == GamePhase.SCORING) {
            aiPending = false;
            unlockAndRender();
            Log.d(TAG, "RUNNER.pump STOP: scoring");
            return;
        }
        // 3) תור אדם: לשחרר UI ולצאת
        if (before.humanTurn) {
            unlockAndRender();
            Log.d(TAG, "RUNNER.pump STOP: waiting for human");
            return;
        }

        // 4) UI busy: retry עם backoff, בלי ספין
        if (uiBusy) {
            scheduleUiBusyRetry();
            return;
        }
        uiBusyStreak = 0;
        // 5) אם כבר יש AI scheduled / אנימציה בריצה שמחוברת לפעולה => לא לעשות כלום
        // כי finishAiAction() אמור לקרוא requestPump.
        if (aiPending) {
            Log.d(TAG, "RUNNER.pump STOP: aiPending (waiting for scheduled AI/animation)");
            return;
        }
        // 6) תור AI + UI פנוי + אין pending => schedule פעולה אחת בלבד
        scheduleAiStep(before);
    }

    private void scheduleUiBusyRetry() {
        uiBusyStreak++;
        int delay = Math.min(UI_BUSY_MAX_MS, UI_BUSY_BASE_MS + (uiBusyStreak - 1) * 80);
        Log.d(TAG, "RUNNER.pump UI busy -> retry in " + delay + "ms (streak=" + uiBusyStreak + ")");
        requestPumpDelayed(delay);
    }

    private void requestPumpDelayed(int delayMs) {
        if (pumpScheduled) {
            Log.d(TAG, "RUNNER.requestPumpDelayed SKIP already scheduled reason=" + "ui_busy_retry");
            return;
        }
        pumpScheduled = true;
        main.postDelayed(() -> {
            pumpScheduled = false;
            lastPumpAtMs = android.os.SystemClock.uptimeMillis();
            pump("ui_busy_retry");
        }, delayMs);
    }

    private void scheduleAiStep(Snap s) {
        final int ai = s.turn;
        final int token = ++aiToken;
        aiPending = true;

        cb.setLocked(true, "Player " + ai + " is thinking…");
        cb.renderNow();

        main.postDelayed(() -> {
            if (token != aiToken) return; // explicitly cancelled
            doAiStep(token, ai);
        }, AI_THINK_MS);
    }

    private void doAiStep(int token, int ai) {
        // If UI became busy, wait and retry later (but keep aiPending true to prevent rescheduling)
        if (cb.isUiBusy()) {
            Log.d(TAG, "RUNNER.doAiStep UI became busy -> retry later");

            // נשאיר aiPending=true כדי שלא יתוזמן AI חדש
            cb.setLocked(false, null);
            cb.renderNow();

            main.postDelayed(() -> {
                if (token != aiToken) return;
                // ננסה שוב את אותו AI
                doAiStep(token, ai);
            }, 120);

            return;
        }
        GamePhase phase = controller.getPhase();

        // Safety stops
        if (phase == GamePhase.SCORING || phase == GamePhase.GAME_OVER || controller.isHumanTurn()) {
            aiPending = false;
            unlockAndRender();
            Log.d(TAG, "RUNNER.doAiStep STOP: terminal/human");
            return;
        }

        // Execute exactly ONE action depending on phase.
        switch (phase) {

            case ORDERING_TRUMP_ROUND1:
            case ORDERING_TRUMP_ROUND2: {
                BasicAiPolicy.OrderDecision d = aiPolicy.decideOrdering(controller, ai);

                switch (d.type) {
                    case PASS:
                        controller.pass();
                        cb.showAction("P" + ai + " passed");
                        finishAiAction(token, "ordering_pass");
                        return;

                    case ORDER_UP_R1:
                        controller.orderUp(controller.getUpCard().getSuit());
                        cb.showAction("P" + ai + " ordered up");
                        finishAiAction(token, "ordering_r1");
                        return;

                    case ORDER_UP_R2:
                        controller.orderUp(d.suit);
                        finishAiAction(token, "ordering_r2");
                        return;

                    default:
                        // Defensive: if AI returns something unexpected, stop cleanly.
                        Log.w(TAG, "RUNNER.ordering: unexpected decision " + d.type);
                        finishAiAction(token, "ordering_unknown");
                        return;
                }
            }

            case DISCARDING: {
                int dealer = controller.getDealerIndex();
                Log.d(TAG, "DISCARDING ai=" + ai
                        + " dealer=" + controller.getDealerIndex()
                        + " isDealerTurnToDiscard=" + controller.isDealerTurnToDiscard());

                // If it's not dealer turn to discard, stop AI pending and wait for correct state.
                if (!controller.isDealerTurnToDiscard()) {
                    Log.d(TAG, "RUNNER.discard: not dealer discard turn -> wait");
                    aiPending = false;
                    unlockAndRender();
                    requestPump("discard_wait_state");
                    return;
                }

                int pick = aiPolicy.chooseDiscardIndex(controller, dealer);
                View from = cb.getPlayerAnchor(dealer);
                View to = cb.getDeckPile();

                cb.animateCard(from, to, () -> {
                    if (token != aiToken) return;
                    controller.discard(pick);
                    finishAiAction(token, "discard_done");
                });
                // Note: keep aiPending until animation ends and finishAiAction runs.
                return;
            }

            case PLAYING_TRICK: {
                List<Integer> legal = controller.getLegalCardIndexesForPlayer(ai);
                if (legal == null || legal.isEmpty()) {
                    Log.w(TAG, "RUNNER.play: legal list empty for P" + ai + " in PLAYING_TRICK");
                    // Don't spin; drop aiPending and wait. This indicates engine inconsistency.
                    aiPending = false;
                    unlockAndRender();
                    return;
                }

                int pick = aiPolicy.choosePlayIndex(controller, ai);

                View from = cb.getPlayerAnchor(ai);
                View to = cb.getTrickSlot(ai);

                cb.animateCard(from, to, () -> {
                    if (token != aiToken) return;
                    controller.playCardAsPlayer(ai, pick);
                    finishAiAction(token, "play_done");
                });
                return;
            }

            default:
                // Unknown/idle phases: stop, unlock, no loops.
                Log.w(TAG, "RUNNER.doAiStep: no handler for phase=" + phase);
                aiPending = false;
                unlockAndRender();
        }
    }

    private void finishAiAction(int token, String reason) {
        if (token != aiToken) return;
        aiPending = false;
        unlockAndRender();
        // Continue only by scheduling next pump AFTER real engine action happened.
        requestPump(reason);
    }



    private void unlockAndRender() {
        cb.setLocked(false, null);
        cb.renderNow();
    }

    // ---------------- debugging snapshots ----------------

    private static class Snap {
        final GamePhase phase;
        final int turn;
        final boolean humanTurn;

        Snap(GamePhase p, int t, boolean h) {
            phase = p;
            turn = t;
            humanTurn = h;
        }

        @NonNull
        @Override public String toString() {
            return "phase=" + phase + " turn=P" + turn + " humanTurn=" + humanTurn;
        }
    }

    private Snap snap() {
        return new Snap(
                controller.getPhase(),
                controller.getCurrentPlayerIndex(),
                controller.isHumanTurn()
        );
    }
}