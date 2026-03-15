package carmel.shubeli.euchre;

public class MatchResult {

    public String playerName;
    public String result;
    public int usScore;
    public int themScore;
    public long timestamp;
    public MatchResult(String playerName, String result, int usScore, int themScore, long timestamp) {
        this.playerName = playerName;
        this.result = result;
        this.usScore = usScore;
        this.themScore = themScore;
        this.timestamp = timestamp;
    }
}