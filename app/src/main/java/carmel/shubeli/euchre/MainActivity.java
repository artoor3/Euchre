package carmel.shubeli.euchre;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnRules = findViewById(R.id.btnRules);
        Button btnOfflineGame = findViewById(R.id.btnOfflineGame);
        Button btnAbout = findViewById(R.id.btnAbout);

        btnRules.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RulesActivity.class))
        );

        btnOfflineGame.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, OfflineGameActivity.class))
        );

        btnAbout.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AboutActivity.class))
        );
    }
}
