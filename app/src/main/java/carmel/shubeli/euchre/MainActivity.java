package carmel.shubeli.euchre;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private Button btnOfflineGame;
    private Button btnProfile;
    private Button btnHistory;
    private Button btnRules;
    private Button btnAbout;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        bindClicks();
    }

    private void bindViews() {
        btnOfflineGame = findViewById(R.id.btnOfflineGame);
        btnProfile = findViewById(R.id.btnProfile);
        btnHistory = findViewById(R.id.btnHistory);
        btnRules = findViewById(R.id.btnRules);
        btnAbout = findViewById(R.id.btnAbout);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void bindClicks() {
        btnOfflineGame.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, GameActivityClean.class)));

        btnRules.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RulesActivity.class)));

        btnAbout.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AboutActivity.class)));

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, HistoryActivity.class)));
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}