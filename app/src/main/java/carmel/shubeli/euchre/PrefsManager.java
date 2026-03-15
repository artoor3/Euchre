package carmel.shubeli.euchre;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {

    private static final String PREF_NAME = "euchre_prefs";
    private static final String KEY_PLAYER_NAME = "player_name";
    private static final String KEY_AVATAR_URI = "avatar_uri";
    private static final String KEY_AVATAR_CAMERA_PATH = "avatar_camera_path";
    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setPlayerName(String name) {
        prefs.edit().putString(KEY_PLAYER_NAME, name).apply();
    }

    public String getPlayerName() {
        return prefs.getString(KEY_PLAYER_NAME, "Player");
    }

    public void setAvatarUri(String uri) {
        prefs.edit().putString(KEY_AVATAR_URI, uri).apply();
    }

    public String getAvatarUri() {
        return prefs.getString(KEY_AVATAR_URI, null);
    }
    public void setAvatarCameraPath(String path) {
        prefs.edit().putString(KEY_AVATAR_CAMERA_PATH, path).apply();
    }

    public String getAvatarCameraPath() {
        return prefs.getString(KEY_AVATAR_CAMERA_PATH, null);
    }
}