package ru.startandroid.develop.chatting.Adapter;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import ru.startandroid.develop.chatting.MessageActivity;
import ru.startandroid.develop.chatting.R;
import ru.startandroid.develop.chatting.model.Chat;
import ru.startandroid.develop.chatting.model.User;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private final Context mContext;
    private final List<User> mUsers;
    private boolean ischat;

    String theLastMessage;

    public UserAdapter (Context mContext, List<User> mUsers, boolean ischat) {
        this.mUsers = mUsers;
        this.mContext = mContext;
        this.ischat = ischat;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.user_item, parent, false);
        return new UserAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = mUsers.get(position);
        holder.username.setText(user.getUsername());
        if (user.getImageURL().equals("default")) {
           holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(user.getImageURL()).into(holder.profile_image);
        }

        if (ischat) {
            lastMessage(user.getId(), holder.last_msg);
        } else {
            holder.last_msg.setVisibility(View.GONE);
        }

        if (ischat) {
            if (user.getStatus().equals("Online")) {
                holder.img_on.setVisibility(View.VISIBLE);
                holder.img_off.setVisibility(View.GONE);
            } else {
                holder.img_on.setVisibility(View.GONE);
                holder.img_off.setVisibility(View.VISIBLE);
            }
        } else {
            holder.img_on.setVisibility(View.GONE);
            holder.img_off.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MessageActivity.class);
                intent.putExtra("userid", user.getId());
                mContext.startActivity(intent);
            }
        });


    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public class ViewHolder extends  RecyclerView.ViewHolder {
        public TextView username;
        public ImageView profile_image;
        private ShapeableImageView img_on;
        private ShapeableImageView img_off;
        private  TextView last_msg;

        public ViewHolder(View itemView) {
            super(itemView);

            username = itemView.findViewById(R.id.username);
            profile_image = itemView.findViewById(R.id.profile_image);
            img_on = itemView.findViewById(R.id.img_on);
            img_off = itemView.findViewById(R.id.img_off);
            last_msg = itemView.findViewById(R.id.last_msg);
        }
    }

    private void lastMessage(String userId, TextView last_msg) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            last_msg.setText("No message");
            return;
        }

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                theLastMessage = null;

                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    Chat chat = snapshot1.getValue(Chat.class);
                    if (chat == null) continue;

                    if ((chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(userId)) ||
                            (chat.getReceiver().equals(userId) && chat.getSender().equals(firebaseUser.getUid()))) {
                        theLastMessage = chat.getMessage();
                    }
                }

                if (theLastMessage == null) {
                    last_msg.setText("No message yet");
                } else {
                    if (theLastMessage.contains("firebasestorage") || theLastMessage.startsWith("http")) {
                        if (theLastMessage.contains(".jpg") || theLastMessage.contains(".png") || theLastMessage.contains("image")) {
                            last_msg.setText("Image sent");
                        } else if (theLastMessage.contains(".mp4") || theLastMessage.contains("video")) {
                            last_msg.setText("Video sent");
                        } else {
                            last_msg.setText("File sent");
                        }
                    } else {
                        last_msg.setText(theLastMessage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                last_msg.setText("Error loading message");
            }
        });
    }
}
