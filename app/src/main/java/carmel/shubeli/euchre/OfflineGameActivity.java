package carmel.shubeli.euchre;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import carmel.shubeli.euchre.game.Card;
import carmel.shubeli.euchre.game.GameEngine;
import carmel.shubeli.euchre.game.Player;

public class OfflineGameActivity extends AppCompatActivity {

    private GameEngine engine;
    private Card[] currentTrick = new Card[4];
    private int leadPlayer;

    private int tricksTeamUs = 0;
    private int tricksTeamThem = 0;
    private int tricksPlayed = 0;

    private int scoreUs = 0;
    private int scoreThem = 0;

    private Button btnOrderUp;
    private Button btnPass;
    private TextView tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_game);

        engine = new GameEngine();
        engine.startNewRound();
        leadPlayer = engine.getCurrentPlayer();

        tvInfo = findViewById(R.id.tvCenterInfo);
        btnOrderUp = findViewById(R.id.btnOrderUp);
        btnPass = findViewById(R.id.btnPass);

        tvInfo.setText("Trump card: " + engine.getTrumpCard() + "\nOrder up?");

        btnOrderUp.setOnClickListener(v -> {
            engine.orderUpTrump(0);
            tvInfo.setText("You ordered up. Trump is " + engine.getTrumpSuit());
            hideTrumpButtons();
            renderMyHand();
        });

        btnPass.setOnClickListener(v -> {
            engine.orderUpTrump(1);
            tvInfo.setText("AI ordered up. Trump is " + engine.getTrumpSuit());
            hideTrumpButtons();
            renderMyHand();
        });
    }

    private void renderMyHand() {
        LinearLayout handContainer = findViewById(R.id.handContainer);
        handContainer.removeAllViews();

        Player me = engine.getPlayers()[0];

        for (int i = 0; i < me.getHand().size(); i++) {
            Card card = me.getHand().get(i);
            int cardIndex = i;

            Button btn = new Button(this);
            btn.setText(card.toString());

            btn.setOnClickListener(v -> playMyCard(cardIndex));

            handContainer.addView(btn);
        }
    }

    private void playMyCard(int cardIndex) {
        if (tricksPlayed >= 5) return;

        currentTrick[0] = engine.playCard(0, cardIndex);
        tvInfo.setText("You played: " + currentTrick[0]);

        for (int i = 1; i < 4; i++) {
            currentTrick[i] = engine.playCard(i, 0);
        }

        int winner = engine.determineTrickWinner(currentTrick, leadPlayer);
        tricksPlayed++;

        if (winner == 0 || winner == 2) {
            tricksTeamUs++;
        } else {
            tricksTeamThem++;
        }

        if (tricksPlayed == 5) {
            handleEndOfRound();
            return;
        }

        tvInfo.setText(
                "You played: " + currentTrick[0] +
                        "\nWinner of trick: Player " + winner
        );

        leadPlayer = winner;
        engine.setCurrentPlayer(winner);
        currentTrick = new Card[4];

        renderMyHand();
    }

    private void handleEndOfRound() {
        boolean makersAreUs =
                engine.getTrumpCaller() == 0 ||
                        engine.getTrumpCaller() == 2;

        String result;

        if (makersAreUs) {
            if (tricksTeamUs >= 3 && tricksTeamUs < 5) {
                scoreUs += 1;
                result = "YOU SCORE 1 POINT";
            } else if (tricksTeamUs == 5) {
                scoreUs += 2;
                result = "YOU SCORE 2 POINTS (SWEEP)";
            } else {
                scoreThem += 2;
                result = "YOU WERE EUCHRED! THEY SCORE 2";
            }
        } else {
            if (tricksTeamThem >= 3 && tricksTeamThem < 5) {
                scoreThem += 1;
                result = "THEY SCORE 1 POINT";
            } else if (tricksTeamThem == 5) {
                scoreThem += 2;
                result = "THEY SCORE 2 POINTS (SWEEP)";
            } else {
                scoreUs += 2;
                result = "THEY WERE EUCHRED! YOU SCORE 2";
            }
        }

        tvInfo.setText(
                result +
                        "\n\nSCORE\nUS: " + scoreUs +
                        "\nTHEM: " + scoreThem
        );

        LinearLayout handContainer = findViewById(R.id.handContainer);
        handContainer.removeAllViews();
    }

    private void hideTrumpButtons() {
        btnOrderUp.setVisibility(View.GONE);
        btnPass.setVisibility(View.GONE);
    }
}
