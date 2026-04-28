package carmel.shubeli.euchre;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {

    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String[]> galleryLauncher;

    private ImageView imageProfile;
    private EditText etPlayerName;
    private Button btnChooseImage;
    private Button btnTakePhoto;
    private Button btnSaveProfile;

    private Uri selectedImageUri;
    private PrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        prefsManager = new PrefsManager(this);

        imageProfile = findViewById(R.id.imageProfile);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        etPlayerName = findViewById(R.id.etPlayerName);

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;

                        final int takeFlags =
                                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

                        try {
                            getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }

                        imageProfile.setImageURI(uri);
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        imageProfile.setImageBitmap(bitmap);

                        String savedPath = saveBitmapToInternalStorage(bitmap);
                        if (savedPath != null) {
                            prefsManager.setAvatarCameraPath(savedPath);
                            prefsManager.setAvatarUri(null);
                        }
                    }
                }
        );

        loadProfile();

        btnChooseImage.setOnClickListener(v ->
                galleryLauncher.launch(new String[]{"image/*"}));

        btnTakePhoto.setOnClickListener(v ->
                cameraLauncher.launch(null));

        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {
        etPlayerName.setText(prefsManager.getPlayerName());

        String cameraPath = prefsManager.getAvatarCameraPath();
        if (cameraPath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(cameraPath);
            if (bitmap != null) {
                imageProfile.setImageBitmap(bitmap);
                return;
            }
        }

        String uri = prefsManager.getAvatarUri();
        if (uri != null) {
            try {
                imageProfile.setImageURI(Uri.parse(uri));
            } catch (SecurityException e) {
                e.printStackTrace();
                prefsManager.setAvatarUri(null);
                Toast.makeText(this, "Could not load saved image. Please choose it again.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                prefsManager.setAvatarUri(null);
            }
        }
    }

    private void saveProfile() {
        String name = etPlayerName.getText().toString().trim();

        if (name.isEmpty()) {
            name = "Player";
        }

        prefsManager.setPlayerName(name);

        if (selectedImageUri != null) {
            prefsManager.setAvatarUri(selectedImageUri.toString());
            prefsManager.setAvatarCameraPath(null);
        }

        Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
    }

    private String saveBitmapToInternalStorage(Bitmap bitmap) {
        String uid = "guest";
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        File file = new File(getFilesDir(), "profile_camera_" + uid + ".jpg");

        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}