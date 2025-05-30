package ru.startandroid.develop.chatting;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.startandroid.develop.chatting.Adapter.MessageAdapter;
import ru.startandroid.develop.chatting.Fragments.APiService;
import ru.startandroid.develop.chatting.Notifications.*;
import ru.startandroid.develop.chatting.model.Chat;
import ru.startandroid.develop.chatting.model.User;

public class MessageActivity extends AppCompatActivity {

    private String userid;
    private ShapeableImageView profile_image;
    private TextView username;

    FirebaseUser fuser;
    DatabaseReference reference;

    ImageButton btn_send;
    EditText text_send;

    MessageAdapter messageAdapter;
    List<Chat> mChat;
    RecyclerView recyclerView;
    Intent intent;
    ValueEventListener seenListener;

    APiService aPiService;
    boolean notify = false;
    private static final int PICK_MEDIA_REQUEST = 1;
    private Uri fileUri;
    ImageButton btn_img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        btn_send = findViewById(R.id.btn_send);
        text_send = findViewById(R.id.text_send);

        btn_img = findViewById(R.id.btn_img);
        btn_img.setOnClickListener(v -> openFileChooser());

        intent = getIntent();
        userid = intent.getStringExtra("userid");
        fuser = FirebaseAuth.getInstance().getCurrentUser();

        btn_send.setOnClickListener(v -> {
            notify = true;
            String rawMsg = text_send.getText().toString();

            String[] lines = rawMsg.split("\n");

            int end = lines.length;
            while (end > 0 && lines[end - 1].trim().isEmpty()) {
                end--;
            }

            StringBuilder cleaned = new StringBuilder();
            for (int i = 0; i < end; i++) {
                cleaned.append(lines[i]);
                if (i < end - 1) {
                    cleaned.append("\n");
                }
            }

            String msg = cleaned.toString().trim();

            if (!msg.isEmpty()) {
                sendMessage(fuser.getUid(), userid, msg);
                text_send.setText("");
            } else {
                Toast.makeText(MessageActivity.this, "You can't send empty message", Toast.LENGTH_SHORT).show();
            }
        });

        reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    username.setText(user.getUsername());
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
                    readMessages(fuser.getUid(), userid, user.getImageURL());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        seenMessage(userid);
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "image/*",
                "video/*",
                "audio/*",
                "application/pdf",
                "application/epub+zip",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        });
        startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_MEDIA_REQUEST);
    }

    private void sendMessage(String sender, final String receiver, String message) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        String messageId = reference.child("Chats").push().getKey();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("messageId", messageId);
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);
        hashMap.put("isseen", false);
        hashMap.put("reaction", "");
        hashMap.put("timestamp", System.currentTimeMillis());

        String fileName = "";

        if (message.contains("res.cloudinary.com")) {
            if (message.contains("/video/")) {
                hashMap.put("messageType", "video");
                // Extract fileName from URL (optional)
                fileName = message.substring(message.lastIndexOf('/') + 1);
            } else if (message.matches(".*\\.(jpg|jpeg|png|gif)$")) {
                hashMap.put("messageType", "image");
                fileName = message.substring(message.lastIndexOf('/') + 1);
            } else if (message.matches(".*\\.(mp3|wav|m4a)$")) {
                hashMap.put("messageType", "audio");
                fileName = message.substring(message.lastIndexOf('/') + 1);
            } else if (message.matches(".*\\.(pdf|docx|epub)$")) {
                hashMap.put("messageType", "document");
                fileName = message.substring(message.lastIndexOf('/') + 1);
            } else {
                hashMap.put("messageType", "file");
                fileName = message.substring(message.lastIndexOf('/') + 1);
            }
        } else {
            hashMap.put("messageType", "text");
        }

        hashMap.put("fileName", fileName);

        reference.child("Chats").child(messageId).setValue(hashMap);

        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(fuser.getUid()).child(userid);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    chatRef.child("id").setValue(userid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

    }

    private void readMessages(final String myid, final String userid, final String imageurl) {
        mChat = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("Chats");

        messageAdapter = new MessageAdapter(MessageActivity.this, mChat, imageurl);
        recyclerView.setAdapter(messageAdapter);

        reference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                Chat chat = snapshot.getValue(Chat.class);
                if (chat != null &&
                        chat.getSender() != null &&
                        chat.getReceiver() != null &&
                        ((chat.getReceiver().equals(myid) && chat.getSender().equals(userid)) ||
                                (chat.getReceiver().equals(userid) && chat.getSender().equals(myid)))) {

                    boolean exists = false;
                    for (Chat c : mChat) {
                        if (c.getMessageId() != null && c.getMessageId().equals(chat.getMessageId())) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        mChat.add(chat);
                        messageAdapter.notifyItemInserted(mChat.size() - 1);
                        recyclerView.scrollToPosition(mChat.size() - 1);
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                Chat updatedChat = snapshot.getValue(Chat.class);
                if (updatedChat != null) {
                    for (int i = 0; i < mChat.size(); i++) {
                        if (mChat.get(i).getMessageId() != null &&
                                mChat.get(i).getMessageId().equals(updatedChat.getMessageId())) {
                            mChat.set(i, updatedChat);
                            messageAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Chat removedChat = snapshot.getValue(Chat.class);
                if (removedChat != null) {
                    for (int i = 0; i < mChat.size(); i++) {
                        if (mChat.get(i).getMessageId() != null &&
                                mChat.get(i).getMessageId().equals(removedChat.getMessageId())) {
                            mChat.remove(i);
                            messageAdapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MessageActivity.this, "Failed to load messages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMessageOptions(Chat chat) {
        if (chat.getSender().equals(fuser.getUid())) {
            String[] options = {"Edit", "Delete", "React"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select an option")
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0: // Edit
                                showEditDialog(chat);
                                break;
                            case 1: // Delete
                                deleteMessage(chat.getMessageId());
                                break;
                            case 2: // React
                                showReactionOptions(chat);
                                break;
                        }
                    }).show();
        } else {
            showReactionOptions(chat);
        }
    }

    private void showEditDialog(Chat chat) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Message");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(chat.getMessage());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newMessage = input.getText().toString();
            editMessage(chat.getMessageId(), newMessage);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showReactionOptions(Chat chat) {
        String[] emojis = {"👍", "❤️", "😂", "😢", "😡", "❌"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("React with an emoji")
                .setItems(emojis, (dialog, which) -> {
                    String selectedReaction = emojis[which];
                    reactToMessage(chat.getMessageId(), selectedReaction);
                }).show();
    }

    public void editMessage(String messageId, String newMessage) {
        FirebaseDatabase.getInstance().getReference("Chats").child(messageId).child("message").setValue(newMessage);
    }

    public void deleteMessage(String messageId) {
        if (messageId == null || messageId.isEmpty()) return;

        FirebaseDatabase.getInstance().getReference("Chats")
                .child(messageId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MessageActivity.this, "Message deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MessageActivity.this, "Failed to delete message", Toast.LENGTH_SHORT).show();
                    Log.e("MessageActivity", "Delete failed", e);
                });
    }

    public void reactToMessage(String messageId, String reaction) {
        FirebaseDatabase.getInstance().getReference("Chats").child(messageId).child("reaction").setValue(reaction);
    }

    private void seenMessage(final String userid) {
        if (userid == null || fuser == null || fuser.getUid() == null) {
            return;
        }

        reference = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    Chat chat = snapshot1.getValue(Chat.class);
                    if (chat != null &&
                            chat.getReceiver() != null &&
                            chat.getSender() != null &&
                            chat.getReceiver().equals(fuser.getUid()) &&
                            chat.getSender().equals(userid)) {

                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("isseen", true);
                        snapshot1.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Add proper error handling
                Log.e("MessageActivity", "Error updating seen status: " + error.getMessage());
                Toast.makeText(MessageActivity.this, "Error updating message status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void status(String status) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);
        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("Online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        reference.removeEventListener(seenListener);
        status("Offline");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_MEDIA_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
            uploadToCloudinary(fileUri);
        }
    }

    private void uploadToCloudinary(Uri uri) {
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        String fileExtension = getFileExtension(uri);
        String resourceType = "auto";
        String messageType = "file";

        if (fileExtension.equals("mp4") || fileExtension.equals("mov") || fileExtension.equals("avi")) {
            resourceType = "video";
        } else if (fileExtension.equals("mp3") || fileExtension.equals("wav") || fileExtension.equals("m4a")) {
            resourceType = "video";
        }

        if (fileExtension.equals("jpg") || fileExtension.equals("jpeg") || fileExtension.equals("png") || fileExtension.equals("gif")) {
            messageType = "image";
        } else if (fileExtension.equals("mp4") || fileExtension.equals("mov") || fileExtension.equals("avi")) {
            messageType = "video";
        } else if (fileExtension.equals("mp3") || fileExtension.equals("wav") || fileExtension.equals("m4a")) {
            messageType = "audio";
        } else if (fileExtension.equals("pdf")) {
            messageType = "pdf";
        } else if (fileExtension.equals("epub")) {
            messageType = "epub";
        } else if (fileExtension.equals("doc") || fileExtension.equals("docx")) {
            messageType = "docx";
        }

        final String finalMessageType = messageType;

        MediaManager.get().upload(uri)
                .option("resource_type", resourceType)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String mediaUrl = resultData.get("secure_url").toString();
                        sendMessageWithType(fuser.getUid(), userid, mediaUrl, finalMessageType);
                        Toast.makeText(MessageActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(MessageActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String extension = mime.getExtensionFromMimeType(contentResolver.getType(uri));
        return extension != null ? extension : "file";
    }

    private void sendMessageWithType(String sender, String receiver, String message, String messageType) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        String messageId = reference.child("Chats").push().getKey();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("messageId", messageId);
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);
        hashMap.put("messageType", messageType);
        hashMap.put("isseen", false);
        hashMap.put("reaction", "");
        hashMap.put("timestamp", System.currentTimeMillis());

        String fileName = message.substring(message.lastIndexOf('/') + 1);
        hashMap.put("fileName", fileName);

        reference.child("Chats").child(messageId).setValue(hashMap);

        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(fuser.getUid()).child(userid);
        chatRef.child("id").setValue(userid);
    }
}
