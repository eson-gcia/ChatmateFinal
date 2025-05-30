package ru.startandroid.develop.chatting.Fragments;

import static android.app.Activity.RESULT_OK;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ru.startandroid.develop.chatting.R;
import ru.startandroid.develop.chatting.model.User;

public class ProfileFragment extends Fragment {

    private ShapeableImageView image_profile;
    private TextView username;
    private DatabaseReference reference;
    private FirebaseUser fuser;
    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 20;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        image_profile = view.findViewById(R.id.profile_image);
        username = view.findViewById(R.id.username);

        fuser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());

        loadUserProfile();

        image_profile.setOnClickListener(v -> openImagePicker());

        username.setOnLongClickListener(v -> {
            showEditUsernameDialog();
            return true;
        });

        return view;
    }

    private void showEditUsernameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_username, null);
        builder.setView(dialogView);

        TextView currentUsername = dialogView.findViewById(R.id.current_username);
        TextInputEditText newUsername = dialogView.findViewById(R.id.new_username);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        String currentUsernameText = username.getText().toString();
        currentUsername.setText(currentUsernameText);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String updatedUsername = newUsername.getText().toString().trim();

            if (updatedUsername.isEmpty()) {
                newUsername.setError("Username cannot be empty");
                return;
            }

            if (updatedUsername.equals(currentUsernameText)) {
                newUsername.setError("Username is the same as current");
                return;
            }

            if (updatedUsername.length() < MIN_USERNAME_LENGTH) {
                newUsername.setError("Username must be at least " + MIN_USERNAME_LENGTH + " characters");
                return;
            }

            if (updatedUsername.length() > MAX_USERNAME_LENGTH) {
                newUsername.setError("Username cannot exceed " + MAX_USERNAME_LENGTH + " characters");
                return;
            }

            if (!isValidUsernameFormat(updatedUsername)) {
                newUsername.setError("Only letters, numbers and underscores allowed");
                return;
            }

            checkUsernameAvailability(updatedUsername, dialog);
        });
    }

    private boolean isValidUsernameFormat(String username) {
        return username.matches("^[a-zA-Z0-9_]+$");
    }

    private void checkUsernameAvailability(String newUsername, AlertDialog dialog) {
        showProgress(true);

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        usersRef.orderByChild("search").equalTo(newUsername.toLowerCase())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        showProgress(false);

                        if (snapshot.exists()) {
                            // Username already exists
                            Toast.makeText(getContext(),
                                    "Username already taken",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Username is available
                            updateUsername(newUsername);
                            dialog.dismiss();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showProgress(false);
                        Toast.makeText(getContext(),
                                "Error checking username availability",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUsername(String newUsername) {
        showProgress(true);

        HashMap<String, Object> map = new HashMap<>();
        map.put("username", newUsername);
        map.put("search", newUsername.toLowerCase());

        reference.updateChildren(map)
                .addOnCompleteListener(task -> {
                    showProgress(false);

                    if (task.isSuccessful()) {
                        username.setText(newUsername);
                        Toast.makeText(getContext(),
                                "Username updated successfully",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                "Failed to update username: " + task.getException(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void loadUserProfile() {
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    username.setText(user.getUsername());
                    if ("default".equals(user.getImageURL())) {
                        image_profile.setImageResource(R.mipmap.ic_launcher);
                    } else {
                        Glide.with(requireContext()).load(user.getImageURL()).into(image_profile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ProfileFragment", "Database error", error.toException());
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            if (imageUri != null) {
                uploadImageToCloudinary();
            }
        }
    }

    private void uploadImageToCloudinary() {
        if (imageUri == null) return;

        MediaManager.get().upload(imageUri)
                .option("public_id", "user_" + fuser.getUid() + "_" + System.currentTimeMillis())
                .option("folder", "user_profiles")
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
                        if (imageUrl != null) {
                            updateProfileImage(imageUrl);
                        } else {
                            Toast.makeText(getContext(),
                                    "Failed to get image URL",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(getContext(),
                                "Upload failed: " + error.getDescription(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {

                    }

                })
                .dispatch();
    }

    private void updateProfileImage(String imageUrl) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("imageURL", imageUrl);

        reference.updateChildren(map)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Glide.with(requireContext())
                                .load(imageUrl)
                                .into(image_profile);
                    }
                });
    }

    private void showProgress(boolean show) {
    }

    private void updateProgress(int percent) {
    }
}