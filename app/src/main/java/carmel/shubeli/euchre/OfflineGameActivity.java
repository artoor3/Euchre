package carmel.shubeli.euchre;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import carmel.shubeli.euchre.game.GameEngine;
import carmel.shubeli.euchre.game.Player;
import carmel.shubeli.euchre.game.Card;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


public class OfflineGameActivity extends AppCompatActivity {
    private GameEngine engine;
    private Card[] currentTrick = new Card[4];
    private int leadPlayer;
    private int tricksTeamUs = 0;    // Players 0 & 2
    private int tricksTeamThem = 0;  // Players 1 & 3
    private int tricksPlayed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_game);

        engine = new GameEngine();
        engine.startNewRound();
        leadPlayer = engine.getCurrentPlayer();

        TextView tvInfo = findViewById(R.id.tvCenterInfo);
        tvInfo.setText("Your turn");

        renderMyHand();
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
        if (tricksPlayed >= 5) {
            return;
        }

        // Player 0 plays
        currentTrick[0] = engine.playCard(0, cardIndex);

        TextView tvInfo = findViewById(R.id.tvCenterInfo);
        tvInfo.setText("You played: " + currentTrick[0]);

        // AI players play automatically
        for (int i = 1; i < 4; i++) {
            currentTrick[i] = engine.playCard(i, 0);
        }

        // Determine winner
        int winner = engine.determineTrickWinner(currentTrick, leadPlayer);
        tricksPlayed++;

        if (winner == 0 || winner == 2) {
            tricksTeamUs++;
        } else {
            tricksTeamThem++;
        }
        if (tricksPlayed == 5) {
             tvInfo = findViewById(R.id.tvCenterInfo);
            LinearLayout handContainer = findViewById(R.id.handContainer);

            String result;
            if (tricksTeamUs > tricksTeamThem) {
                result = "ROUND OVER — YOU WIN (" +
                        tricksTeamUs + " to " + tricksTeamThem + ")";
            } else {
                result = "ROUND OVER — YOU LOSE (" +
                        tricksTeamUs + " to " + tricksTeamThem + ")";
            }

            tvInfo.setText(result);

            // 🔒 LOCK UI
            handContainer.removeAllViews();

            return; // stop further logic
        }


        tvInfo.setText(
                "You played: " + currentTrick[0] +
                        "\nWinner of trick: Player " + winner
        );

        // Prepare next trick
        leadPlayer = winner;
        engine.setCurrentPlayer(winner);

        // Clear trick
        currentTrick = new Card[4];

        // Re-render your hand
        renderMyHand();
    }

}