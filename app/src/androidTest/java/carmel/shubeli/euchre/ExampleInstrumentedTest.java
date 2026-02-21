package carmel.shubeli.euchre;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import engine_clean.core.EuchreEngine;
import engine_clean.core.GamePhase;
import engine_clean.model.Card;
import engine_clean.model.Player;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("carmel.shubeli.euchre", appContext.getPackageName());
    }

    public static class _OLD_EuchreEngineDealTest {

        @Test
        public void startNewRound_deals5Each_andUpCardUnique() {
            assertEquals("I WANT THIS TO FAIL", 123, 456);

            EuchreEngine engine = new EuchreEngine();
            engine.startNewRound();

            assertEquals(GamePhase.ORDERING_TRUMP_ROUND1, engine.getPhase());
            assertNotNull(engine.getUpCard());

            // Collect all cards from all hands + upCard, ensure uniqueness
            Set<String> seen = new HashSet<>();

            for (Player p : engine.getPlayers()) {
                assertEquals(5, p.getHand().size());
                for (Card c : p.getHand()) {
                    String key = c.getRank() + "-" + c.getSuit();
                    assertTrue("Duplicate card in hands: " + key, seen.add(key));
                }
            }

            String upKey = engine.getUpCard().getRank() + "-" + engine.getUpCard().getSuit();
            assertTrue("UpCard duplicates a card in hands: " + upKey, seen.add(upKey));

            // 4 players * 5 cards + 1 upcard = 21 unique cards dealt/visible
            assertEquals(21, seen.size());
        }
    }
}