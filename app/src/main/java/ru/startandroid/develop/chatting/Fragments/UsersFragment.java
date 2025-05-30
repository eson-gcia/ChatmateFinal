package ru.startandroid.develop.chatting.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ru.startandroid.develop.chatting.Adapter.UserAdapter;
import ru.startandroid.develop.chatting.R;
import ru.startandroid.develop.chatting.model.User;

public class UsersFragment extends Fragment {

    private RecyclerView recyclerView;

    private UserAdapter userAdapter;
    private List<User> mUsers;

    EditText search_users;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_users, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mUsers = new ArrayList<>();

        readUsers();

        search_users = view.findViewById(R.id.search_users);
        search_users.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return  view;

    }

    private void searchUsers(String s) {
        final FirebaseUser fuser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mUsers.clear();
                List<User> startsWithList = new ArrayList<>();
                List<User> containsList = new ArrayList<>();

                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    User user = snapshot1.getValue(User.class);

                    if (user != null && user.getId() != null && fuser != null && fuser.getUid() != null) {
                        String username = user.getUsername() != null ? user.getUsername().toLowerCase() : "";
                        String searchText = s.toLowerCase();

                        if (username.startsWith(searchText)) {
                            startsWithList.add(user);
                        } else if (username.contains(searchText)) {
                            containsList.add(user);
                        }
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

                // Merge results
                startsWithList.addAll(containsList);
                mUsers.addAll(startsWithList);

                userAdapter = new UserAdapter(getContext(), mUsers, false);
                recyclerView.setAdapter(userAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void readUsers() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (search_users.getText().toString().equals("")) {
                    mUsers.clear();
                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                        User user = snapshot1.getValue(User.class);
                        if (user != null && user.getId() != null && firebaseUser != null && firebaseUser.getUid() != null && !user.getId().equals(firebaseUser.getUid())) {
                            mUsers.add(user);
                        }
                    }

                    Collections.sort(mUsers, new Comparator<User>() {
                        @Override
                        public int compare(User u1, User u2) {
                            return u1.getUsername().compareToIgnoreCase(u2.getUsername());
                        }
                    });


                    userAdapter = new UserAdapter(getContext(), mUsers, false);
                    recyclerView.setAdapter(userAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}