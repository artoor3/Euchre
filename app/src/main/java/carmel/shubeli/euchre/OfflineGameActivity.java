package carmel.shubeli.euchre;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import carmel.shubeli.euchre.game.GameEngine;
import carmel.shubeli.euchre.game.Player;
import carmel.shubeli.euchre.game.Card;


public class OfflineGameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_game);

        GameEngine engine = new GameEngine();
        engine.startNewRound();

        for (Player p : engine.getPlayers()) {
            for (Card c : p.getHand()) {
                Log.d("EUCHRE", "Player " + p.getId() + ": " + c);
            }
        }
    }
}