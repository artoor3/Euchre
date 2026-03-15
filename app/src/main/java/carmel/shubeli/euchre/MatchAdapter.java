package carmel.shubeli.euchre;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.MatchViewHolder> {

    private final List<MatchResult> matchList;

    public MatchAdapter(List<MatchResult> matchList) {
        this.matchList = matchList;
    }

    public static class MatchViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlayerName, tvResult, tvScore, tvDate;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
            tvResult = itemView.findViewById(R.id.tvResult);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match, parent, false);
        return new MatchViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        MatchResult item = matchList.get(position);

        holder.tvPlayerName.setText(item.playerName);
        holder.tvResult.setText(item.result);
        holder.tvScore.setText("Us " + item.usScore + " - Them " + item.themScore);
        holder.tvDate.setText(DateFormat.getDateTimeInstance().format(new Date(item.timestamp)));
    }

    @Override
    public int getItemCount() {
        return matchList.size();
    }
}