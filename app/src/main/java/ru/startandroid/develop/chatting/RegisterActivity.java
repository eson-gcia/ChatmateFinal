package ru.startandroid.develop.chatting;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 20;

    TextInputEditText username, email, password;
    Button btn_register;
    FirebaseAuth auth;
    DatabaseReference reference;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Register");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        username = findViewById(R.id.username);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btn_register = findViewById(R.id.btn_register);
        auth = FirebaseAuth.getInstance();

        btn_register.setOnClickListener(view -> {
            String txt_username = Objects.requireNonNull(username.getText()).toString().trim();
            String txt_email = Objects.requireNonNull(email.getText()).toString().trim();
            String txt_password = Objects.requireNonNull(password.getText()).toString();

            if (TextUtils.isEmpty(txt_username)) {
                username.setError("Username is required");
                return;
            }

            if (TextUtils.isEmpty(txt_email)) {
                email.setError("Email is required");
                return;
            }

            if (TextUtils.isEmpty(txt_password)) {
                password.setError("Password is required");
                return;
            }

            if (txt_username.length() < MIN_USERNAME_LENGTH) {
                username.setError("Username must be at least " + MIN_USERNAME_LENGTH + " characters");
                return;
            }

            if (txt_username.length() > MAX_USERNAME_LENGTH) {
                username.setError("Username cannot exceed " + MAX_USERNAME_LENGTH + " characters");
                return;
            }

            if (!isValidUsernameFormat(txt_username)) {
                username.setError("Only letters, numbers and underscores allowed");
                return;
            }

            if (txt_password.length() < 6) {
                password.setError("Password should be at least 6 characters");
                return;
            }

            checkUsernameAvailability(txt_username, txt_email, txt_password);
        });
    }

    private boolean isValidUsernameFormat(String username) {
        return username.matches("^[a-zA-Z0-9_]+$");
    }

    private void checkUsernameAvailability(String username, String email, String password) {
        showProgress(true);

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        Query query = usersRef.orderByChild("search").equalTo(username.toLowerCase());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showProgress(false);
                if (snapshot.exists()) {
                    RegisterActivity.this.username.setError("Username already taken");
                    Toast.makeText(RegisterActivity.this, "Username is not available", Toast.LENGTH_SHORT).show();
                } else {
                    register(username, email, password);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showProgress(false);
                Log.e("FirebaseError", "onCancelled: " + error.getMessage());
                Toast.makeText(RegisterActivity.this, "Error checking username availability: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void register(String username, String email, String password) {
        showProgress(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        String userid = firebaseUser.getUid();
                        uploadDefaultProfileImage(username, userid);
                    } else {
                        showProgress(false);
                        Toast.makeText(RegisterActivity.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showProgress(boolean show) {
        if (show) {
        } else {
        }
    }

    private void uploadDefaultProfileImage(String username, String userId) {
        int defaultImageRes = R.mipmap.ic_launcher;
        String defaultImageUrl = "https://res.cloudinary.com/dhkm0ntgu/image/upload/v1746936903/sample.jpg";

        MediaManager.get().upload(defaultImageRes)
                .option("public_id", "default_profile_" + userId)
                .option("folder", "default_profiles")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {

                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {

                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveUserDataToFirebase(userId, username, imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        saveUserDataToFirebase(userId, username, defaultImageUrl);
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {

                    }

                })
                .dispatch();
    }

    private void uploadProfileImage(Uri imageUri, String username, String userId, String defaultImageUrl) {
        if (imageUri != null) {
            MediaManager.get().upload(imageUri)
                    .option("upload_preset", "unsigned_preset")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String cloudinaryUrl = resultData.get("secure_url").toString();
                            saveUserDataToFirebase(userId, username, cloudinaryUrl);
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Toast.makeText(RegisterActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                        }
                    }).dispatch();
        } else {
            saveUserDataToFirebase(userId, username, defaultImageUrl);
        }
    }

    private void saveUserDataToFirebase(String userId, String username, String imageUrl) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", userId);
        hashMap.put("username", username);
        hashMap.put("imageURL", imageUrl);
        hashMap.put("status", "offline");
        hashMap.put("search", username.toLowerCase());

        reference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    FirebaseMessaging.getInstance().getToken().addOnCompleteListener(tokenTask -> {
                        if (tokenTask.isSuccessful()) {
                            String token = tokenTask.getResult();
                            reference.child("Tokens").setValue(token);
                        }
                    });

                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }
}
