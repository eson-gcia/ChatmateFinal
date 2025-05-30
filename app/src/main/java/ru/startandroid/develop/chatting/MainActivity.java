package ru.startandroid.develop.chatting;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ru.startandroid.develop.chatting.Fragments.ChatsFragment;
import ru.startandroid.develop.chatting.Fragments.ProfileFragment;
import ru.startandroid.develop.chatting.Fragments.UsersFragment;
import ru.startandroid.develop.chatting.model.User;

public class MainActivity extends AppCompatActivity {

    private ShapeableImageView profile_image;
    private TextView username;
    private FirebaseUser firebaseUser;
    private DatabaseReference reference;
    private ViewPager2 viewPager2;
    private TabLayout tabLayout;
    private ViewPageAdapter viewPageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("");

        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

            reference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        username.setText(user.getUsername());
                        if ("default".equals(user.getImageURL())) {
                            profile_image.setImageResource(R.mipmap.ic_launcher);
                        } else {
                            Glide.with(MainActivity.this).load(user.getImageURL()).into(profile_image);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }

        tabLayout = findViewById(R.id.tab_layout);
        viewPager2 = findViewById(R.id.viewPager);

        viewPageAdapter = new ViewPageAdapter(this);
        viewPageAdapter.addFragment(new ChatsFragment(), "Chats");
        viewPageAdapter.addFragment(new UsersFragment(), "Users");
        viewPageAdapter.addFragment(new ProfileFragment(), "Profile");

        viewPager2.setAdapter(viewPageAdapter);

        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) ->
                tab.setText(viewPageAdapter.getTitle(position))
        ).attach();
    }

    private void uploadProfileImage(Uri imageUri) {
        MediaManager.get().upload(imageUri)
                .callback(new com.cloudinary.android.callback.UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("url");

                        if (firebaseUser != null) {
                            reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
                            HashMap<String, Object> updates = new HashMap<>();
                            updates.put("imageURL", imageUrl);
                            reference.updateChildren(updates);
                        }
                    }

                    @Override
                    public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                    }

                    @Override
                    public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                    }
                }).dispatch();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            updateStatus("Offline");

            new Handler().postDelayed(() -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainActivity.this, StartActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }, 300);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class ViewPageAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        private final List<Fragment> fragmentList = new ArrayList<>();
        private final List<String> titleList = new ArrayList<>();

        public ViewPageAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        public void addFragment(Fragment fragment, String title) {
            fragmentList.add(fragment);
            titleList.add(title);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getItemCount() {
            return fragmentList.size();
        }

        public String getTitle(int position) {
            return titleList.get(position);
        }
    }

    private void status(String status) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus("Online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateStatus("Offline");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void updateStatus(String status) {
        if (firebaseUser != null) {
            DatabaseReference statusRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(firebaseUser.getUid())
                    .child("status");

            statusRef.setValue(status)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e("StatusUpdate", "Failed to update status: " +
                                    (task.getException() != null ?
                                            task.getException().getMessage() : "unknown error"));
                        }
                    });
        }
    }
}
