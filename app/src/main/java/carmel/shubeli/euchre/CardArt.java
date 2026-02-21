package carmel.shubeli.euchre;

import android.content.Context;

import engine_clean.model.Card;
import engine_clean.model.Rank;
import engine_clean.model.Suit;

public class CardArt {

    public static int resIdForCard(Context ctx, Card c) {
        if (c == null) return R.drawable.c_back;

        String name = "c_" + rankCode(c.getRank()) + suitCode(c.getSuit());
        int id = ctx.getResources().getIdentifier(name, "drawable", ctx.getPackageName());
        return (id != 0) ? id : R.drawable.c_back; // fallback
    }

    private static String rankCode(Rank r) {
        switch (r) {
            case ACE: return "a";
            case KING: return "k";
            case QUEEN: return "q";
            case JACK: return "j";
            case TEN: return "10"; // חשוב! אם אצלך שמרת כ-t אז תגיד לי
            case NINE: return "9";
            default: return "x";
        }
    }

    private static String suitCode(Suit s) {
        switch (s) {
            case HEARTS: return "h";
            case DIAMONDS: return "d";
            case CLUBS: return "c";
            case SPADES: return "s";
            default: return "x";
        }
    }
}