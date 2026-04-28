package carmel.shubeli.euchre;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PrefsManager {

    private static final String PREF_NAME = "euchre_prefs";

    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private String getUidSuffix() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return "_guest";
        }
        return "_" + user.getUid();
    }

    private String keyPlayerName() {
        return "player_name" + getUidSuffix();
    }

    private String keyAvatarUri() {
        return "avatar_uri" + getUidSuffix();
    }

    private String keyAvatarCameraPath() {
        return "avatar_camera_path" + getUidSuffix();
    }

    public void setPlayerName(String name) {
        prefs.edit().putString(keyPlayerName(), name).apply();
    }

    public String getPlayerName() {
        return prefs.getString(keyPlayerName(), "Player");
    }

    public void setAvatarUri(String uri) {
        prefs.edit().putString(keyAvatarUri(), uri).apply();
    }

    public String getAvatarUri() {
        return prefs.getString(keyAvatarUri(), null);
    }

    public void setAvatarCameraPath(String path) {
        prefs.edit().putString(keyAvatarCameraPath(), path).apply();
    }

    public String getAvatarCameraPath() {
        return prefs.getString(keyAvatarCameraPath(), null);
    }
}