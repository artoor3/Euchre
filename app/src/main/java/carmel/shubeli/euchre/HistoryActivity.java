package carmel.shubeli.euchre;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private MatchAdapter adapter;
    private final List<MatchResult> matchList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        RecyclerView recyclerViewMatches = findViewById(R.id.recyclerViewMatches);
        recyclerViewMatches.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MatchAdapter(matchList);
        recyclerViewMatches.setAdapter(adapter);

        loadMatches();
    }

    private void loadMatches() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("matches")
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        matchList.clear();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            MatchResult match = ds.getValue(MatchResult.class);
                            if (match != null) {
                                matchList.add(0, match);
                            }
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }
}