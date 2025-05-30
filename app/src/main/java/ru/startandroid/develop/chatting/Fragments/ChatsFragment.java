package ru.startandroid.develop.chatting.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ru.startandroid.develop.chatting.Adapter.UserAdapter;
import ru.startandroid.develop.chatting.Notifications.Token;
import ru.startandroid.develop.chatting.R;
import ru.startandroid.develop.chatting.model.Chat;
import ru.startandroid.develop.chatting.model.Chatlist;
import ru.startandroid.develop.chatting.model.User;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> mUsers;

    EditText search_users;

    FirebaseUser fuser;
    DatabaseReference reference;

    private List<Chatlist> usersList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_users, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fuser = FirebaseAuth.getInstance().getCurrentUser();

        usersList = new ArrayList<>();
        mUsers = new ArrayList<>();

        search_users = view.findViewById(R.id.search_users);
        search_users.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersList.clear();

                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    Chat chat = snapshot1.getValue(Chat.class);

                    if (chat == null || fuser == null) continue;

                    String sender = chat.getSender();
                    String receiver = chat.getReceiver();
                    String currentUserId = fuser.getUid();

                    if (currentUserId != null) {
                        if (sender != null && currentUserId.equals(sender)) {
                            if (receiver != null) {
                                addUserToChatList(receiver, chat.getTimestamp());
                            }
                        } else if (receiver != null && currentUserId.equals(receiver)) {
                            if (sender != null) {
                                addUserToChatList(sender, chat.getTimestamp());
                            }
                        }
                    } else {
                        Log.e("ChatsFragment", "currentUserId is null");
                    }
                }

                usersList.sort(new Comparator<Chatlist>() {
                    @Override
                    public int compare(Chatlist o1, Chatlist o2) {
                        return Long.compare(o2.getTimestamp(), o1.getTimestamp());
                    }
                });

                chatList();
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        updateToken(token);
                    } else {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                    }
                });

        return view;
    }

    private void updateToken(String token) {
        FirebaseUser fuser = FirebaseAuth.getInstance().getCurrentUser();
        if (fuser != null) {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
            Token token1 = new Token(token);
            reference.child(fuser.getUid()).setValue(token1);
        }
    }

    private void addUserToChatList(String userId, long timestamp) {
        if (userId == null) return;

        for (Chatlist cl : usersList) {
            if (cl.getId() != null && userId.equals(cl.getId())) {
                if (timestamp > cl.getTimestamp()) {
                    cl.setTimestamp(timestamp);
                }
                return;
            }
        }

        usersList.add(new Chatlist(userId, timestamp));
    }

    private void chatList() {
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mUsers.clear();

                Map<String, User> allUsersMap = new HashMap<>();
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    User user = snapshot1.getValue(User.class);
                    if (user != null) {
                        allUsersMap.put(user.getId(), user);
                    }
                }

                for (Chatlist chatlist : usersList) {
                    User user = allUsersMap.get(chatlist.getId());
                    if (user != null) {
                        mUsers.add(user);
                    }
                }

                userAdapter = new UserAdapter(getContext(), mUsers, true);
                recyclerView.setAdapter(userAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void filterUsers(String text) {
        List<User> startsWithList = new ArrayList<>();
        List<User> containsList = new ArrayList<>();

        for (User user : mUsers) {
            String username = user.getUsername().toLowerCase();

            if (username.startsWith(text)) {
                startsWithList.add(user);
            } else if (username.contains(text)) {
                containsList.add(user);
            }
        }

        Comparator<User> comparator = new Comparator<User>() {
            @Override
            public int compare(User u1, User u2) {
                return u1.getUsername().compareToIgnoreCase(u2.getUsername());
            }
        };

        Collections.sort(startsWithList, comparator);
        Collections.sort(containsList, comparator);

        startsWithList.addAll(containsList);

        userAdapter = new UserAdapter(getContext(), startsWithList, true);
        recyclerView.setAdapter(userAdapter);
    }

}
