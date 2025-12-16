package carmel.shubeli.euchre;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Button btnStart = findViewById(R.id.btnStart);

        btnStart.setOnClickListener(v ->
                startActivity(new Intent(StartActivity.this, LoginActivity.class))
        );
    }
}
