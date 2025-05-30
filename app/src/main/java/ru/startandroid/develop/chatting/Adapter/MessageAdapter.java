package ru.startandroid.develop.chatting.Adapter;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ru.startandroid.develop.chatting.FullscreenImageActivity;
import ru.startandroid.develop.chatting.MessageActivity;
import ru.startandroid.develop.chatting.R;
import ru.startandroid.develop.chatting.WebViewActivity;
import ru.startandroid.develop.chatting.model.Chat;
import ru.startandroid.develop.chatting.model.User;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;
    private final Context mContext;
    private List<Chat> mChat;
    private final String imageurl;
    private final DatabaseReference messagesRef;
    private final String currentUserId;

    public interface OnItemClickListener {
        void onItemClick(Chat chat);
    }
    public MessageAdapter(Context mContext, List<Chat> mChat, String imageurl) {
        this.mChat = mChat != null ? mChat : new ArrayList<>();
        this.mContext = mContext;
        this.imageurl = imageurl;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.messagesRef = FirebaseDatabase.getInstance().getReference("Chats");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(
                viewType == MSG_TYPE_RIGHT ? R.layout.chat_item_right : R.layout.chat_item_left,
                parent, false);
        return new ViewHolder(view, viewType);
    }

    public static String convertTimestampToDateTime(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());
        return sdf.format(date);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = mChat.get(position);
        long timestamp = chat.getTimestamp();
        String formattedTime = convertTimestampToDateTime(timestamp);
        holder.timestampTextView.setText(formattedTime);
        boolean isCurrentUser = chat.getSender() != null && chat.getSender().equals(currentUserId);
        String message = chat.getMessage();
        String messageType = chat.getMessageType();
        String fileName = chat.getFileName() != null ? chat.getFileName() : "File";

        holder.image_message.setVisibility(View.GONE);
        holder.show_message.setVisibility(View.GONE);

        if (messageType == null || messageType.equals("text")) {
            // TEXT
            holder.show_message.setVisibility(View.VISIBLE);
            holder.show_message.setText(message);
            holder.show_message.setAutoLinkMask(Linkify.WEB_URLS);
            holder.show_message.setLinksClickable(true);

            if (Patterns.WEB_URL.matcher(message).matches()) {
                holder.show_message.setTextColor(Color.GREEN);
                holder.show_message.setOnClickListener(v -> {
                    Intent intent = new Intent(mContext, WebViewActivity.class);
                    intent.putExtra("url", message);
                    mContext.startActivity(intent);
                });
            } else {
                holder.show_message.setTextColor(Color.BLACK);
                holder.show_message.setOnClickListener(null);
            }

            adjustAlignment(holder, isCurrentUser, R.id.show_message);

        } else if (messageType.equals("image")) {
            // IMAGE
            holder.image_message.setVisibility(View.VISIBLE);
            Glide.with(mContext).load(message).into(holder.image_message);

            holder.image_message.setOnClickListener(v -> {
                Intent intent = new Intent(mContext, FullscreenImageActivity.class);
                intent.putExtra("image_url", message);
                mContext.startActivity(intent);
            });

            adjustAlignment(holder, isCurrentUser, R.id.image_message);

        } else if (messageType.equals("video")) {
            // VIDEO
            holder.image_message.setVisibility(View.VISIBLE);

            RequestOptions requestOptions = new RequestOptions()
                    .frame(1000000)
                    .override(holder.image_message.getWidth(), holder.image_message.getHeight());

            Glide.with(mContext)
                    .asBitmap()
                    .load(message)
                    .apply(requestOptions)
                    .error(Glide.with(mContext)
                            .load(message + ".jpg")
                            .placeholder(R.drawable.ic_video_placeholder))
                    .into(holder.image_message);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Drawable playButton = ContextCompat.getDrawable(mContext, R.drawable.ic_play_circle);

                holder.image_message.setForeground(playButton);
                holder.image_message.setForegroundGravity(Gravity.CENTER);
            } else {
                holder.image_message.post(() -> {
                    Bitmap thumbnail = ((BitmapDrawable) holder.image_message.getDrawable()).getBitmap();
                    Bitmap playIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_play_circle);

                    Bitmap combined = Bitmap.createBitmap(thumbnail.getWidth(), thumbnail.getHeight(), thumbnail.getConfig());
                    Canvas canvas = new Canvas(combined);
                    canvas.drawBitmap(thumbnail, 0, 0, null);
                    canvas.drawBitmap(playIcon,
                            (thumbnail.getWidth() - playIcon.getWidth()) / 2,
                            (thumbnail.getHeight() - playIcon.getHeight()) / 2,
                            null);

                    holder.image_message.setImageBitmap(combined);
                });
            }

            holder.image_message.setOnClickListener(v -> {

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(message), "video/*");
                mContext.startActivity(intent);
            });

            adjustAlignment(holder, isCurrentUser, R.id.image_message);
        } else if (messageType.equals("audio")) {
            // AUDIO
            holder.show_message.setVisibility(View.VISIBLE);
            holder.show_message.setText("🎵 " + fileName);
            holder.show_message.setTextColor(Color.MAGENTA);

            holder.show_message.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(message), "audio/*");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                mContext.startActivity(intent);
            });

            adjustAlignment(holder, isCurrentUser, R.id.show_message);

        } else if (messageType.equals("pdf") || messageType.equals("docx") || messageType.equals("epub")) {
            // DOCUMENT
            holder.show_message.setVisibility(View.VISIBLE);
            holder.show_message.setText("📄 " + fileName);
            holder.show_message.setTextColor(Color.DKGRAY);

            holder.show_message.setOnClickListener(v -> {
                new AlertDialog.Builder(mContext)
                        .setTitle("Download File")
                        .setMessage("Do you want to download this file?")
                        .setPositiveButton("Download", (dialog, which) -> downloadFile(message, fileName))
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            adjustAlignment(holder, isCurrentUser, R.id.show_message);

        } else {
            // UNKNOWN TYPE
            holder.show_message.setVisibility(View.VISIBLE);
            holder.show_message.setText("📁 " + fileName);
            holder.show_message.setTextColor(Color.GRAY);
            holder.show_message.setOnClickListener(null);

            adjustAlignment(holder, isCurrentUser, R.id.show_message);
        }


        // Profile image
        if (imageurl.equals("default")) {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(imageurl).into(holder.profile_image);
        }

        // Seen/Delivered
        int lastSentIndex = -1;
        for (int i = mChat.size() - 1; i >= 0; i--) {
            if (mChat.get(i).getSender().equals(currentUserId)) {
                lastSentIndex = i;
                break;
            }
        }

        if (position == lastSentIndex) {
            holder.txt_seen.setText(chat.isIsseen() ? "Seen" : "Delivered");
            holder.txt_seen.setVisibility(View.VISIBLE);
        } else {
            holder.txt_seen.setVisibility(View.GONE);
        }

        // Reactions
        if (chat.getReaction() != null && !chat.getReaction().isEmpty()) {
            holder.reaction_text.setText(chat.getReaction());
            holder.reaction_text.setVisibility(View.VISIBLE);
        } else {
            holder.reaction_text.setVisibility(View.GONE);
        }

        // Long press options
        holder.itemView.setOnLongClickListener(v -> {
            showMessageOptions(chat, position, isCurrentUser, v);
            return true;
        });

        View.OnLongClickListener longClickListener = v -> {
            showMessageOptions(chat, position, isCurrentUser, v);
            return true;
        };

        holder.itemView.setOnLongClickListener(longClickListener);

        if (messageType == null || messageType.equals("text") ||
                messageType.equals("audio") ||
                messageType.equals("pdf") ||
                messageType.equals("docx") ||
                messageType.equals("epub")) {
            holder.show_message.setOnLongClickListener(longClickListener);
        } else if (messageType.equals("image") || messageType.equals("video")) {
            holder.image_message.setOnLongClickListener(longClickListener);
        }

    }

    private void adjustAlignment(ViewHolder holder, boolean isCurrentUser, int anchorId) {
        RelativeLayout.LayoutParams seenParams = (RelativeLayout.LayoutParams) holder.txt_seen.getLayoutParams();
        seenParams.addRule(RelativeLayout.BELOW, anchorId);
        holder.txt_seen.setLayoutParams(seenParams);

        RelativeLayout.LayoutParams reactionParams = (RelativeLayout.LayoutParams) holder.reaction_text.getLayoutParams();
        reactionParams.addRule(RelativeLayout.ALIGN_BOTTOM, anchorId);
        reactionParams.removeRule(RelativeLayout.LEFT_OF);
        reactionParams.removeRule(RelativeLayout.RIGHT_OF);
        if (isCurrentUser) {
            reactionParams.addRule(RelativeLayout.LEFT_OF, anchorId);
            reactionParams.rightMargin = 0;
            reactionParams.leftMargin = 4;
        } else {
            reactionParams.addRule(RelativeLayout.RIGHT_OF, anchorId);
            reactionParams.leftMargin = 0;
            reactionParams.rightMargin = 4;
        }
        holder.reaction_text.setLayoutParams(reactionParams);
    }

    private void downloadFile(String fileUrl, String fileName) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setTitle(fileName);
        request.setDescription("Downloading...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager manager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }

    private void showMessageOptions(Chat chat, int position, boolean isCurrentUser, View anchorView) {
        PopupMenu popupMenu = new PopupMenu(mContext, anchorView);
        popupMenu.inflate(R.menu.message_options_menu);

        Menu menu = popupMenu.getMenu();
        menu.findItem(R.id.unsend).setVisible(isCurrentUser);
        menu.findItem(R.id.edit).setVisible(isCurrentUser);

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.unsend) {
                unsendMessage(chat, position);
                return true;
            } else if (itemId == R.id.edit) {
                editMessage(chat, position);
                return true;
            } else if (itemId == R.id.react) {
                showReactionOptions(chat, anchorView);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }


    private void unsendMessage(Chat chat, int position) {
        String messageId = chat.getMessageId();
        if (messageId == null) {
            Toast.makeText(mContext, "Invalid message", Toast.LENGTH_SHORT).show();
            return;
        }

        messagesRef.child(messageId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int indexToRemove = -1;
                        for (int i = 0; i < mChat.size(); i++) {
                            String id = mChat.get(i).getMessageId();
                            if (messageId.equals(id)) {  // safe equals check
                                indexToRemove = i;
                                break;
                            }
                        }
                        if (indexToRemove != -1) {
                            mChat.remove(indexToRemove);
                            notifyItemRemoved(indexToRemove);
                            notifyItemRangeChanged(indexToRemove, mChat.size());
                        } else {
                            Toast.makeText(mContext, "Message not found in list", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(mContext, "Failed to unsend message", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void updateMessage(Chat chat, String newMessage, int position) {
        String messageId = chat.getMessageId();
        if (messageId == null) {
            Toast.makeText(mContext, "Invalid message", Toast.LENGTH_SHORT).show();
            return;
        }

        messagesRef.child(messageId).child("message").setValue(newMessage)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mChat.get(position).setMessage(newMessage);
                        notifyItemChanged(position);
                    } else {
                        Toast.makeText(mContext, "Failed to edit message", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void editMessage(Chat chat, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Edit Message");

        final EditText input = new EditText(mContext);
        input.setText(chat.getMessage());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newMessage = input.getText().toString().trim();
            if (!newMessage.isEmpty()) {
                updateMessage(chat, newMessage, position);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showReactionOptions(Chat chat, View anchorView) {
        PopupMenu reactionMenu = new PopupMenu(mContext, anchorView);
        reactionMenu.inflate(R.menu.reaction_menu);

        reactionMenu.setOnMenuItemClickListener(item -> {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String senderId = chat.getSender();
            boolean isSender = currentUserId.equals(senderId);

            String selectedReaction = "";
            int itemId = item.getItemId();

            if (itemId == R.id.react_like) selectedReaction = "👍";
            else if (itemId == R.id.react_love) selectedReaction = "❤️";
            else if (itemId == R.id.react_laugh) selectedReaction = "😂";
            else if (itemId == R.id.react_sad) selectedReaction = "😢";
            else if (itemId == R.id.react_angry) selectedReaction = "😠";
            else if (itemId == R.id.react_remove) selectedReaction = " ";

            String finalSelectedReaction = selectedReaction;

            messagesRef.child(chat.getMessageId()).child("reaction")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String currentReaction = snapshot.getValue(String.class);

                            String senderReact = " ";
                            String receiverReact = " ";

                            if (currentReaction != null) {
                                senderReact = getEmojiAt(currentReaction, 0);
                                receiverReact = getEmojiAt(currentReaction, 1);
                            }

                            if (isSender) {
                                senderReact = finalSelectedReaction.isEmpty() ? " " : finalSelectedReaction;
                            } else {
                                receiverReact = finalSelectedReaction.isEmpty() ? " " : finalSelectedReaction;
                            }

                            String updatedReaction = senderReact + receiverReact;

                            messagesRef.child(chat.getMessageId()).child("reaction").setValue(updatedReaction)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            chat.setReaction(updatedReaction);
                                            notifyItemChanged(mChat.indexOf(chat));
                                        }
                                    });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Reaction", "Error: " + error.getMessage());
                        }
                    });

            return true;
        });

        reactionMenu.show();
    }

    private String getEmojiAt(String reactionStr, int position) {
        int count = 0;
        int index = 0;
        while (index < reactionStr.length()) {
            int codePoint = reactionStr.codePointAt(index);
            int charCount = Character.charCount(codePoint);

            if (count == position) {
                return reactionStr.substring(index, index + charCount);
            }
            index += charCount;
            count++;
        }
        return " ";
    }


    @Override
    public int getItemCount() {
        return mChat != null ? mChat.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView show_message;
        public ImageView profile_image;
        public TextView txt_seen;
        public TextView reaction_text;
        public ImageView image_message;
        public TextView timestampTextView;

        public ViewHolder(View itemView, int viewType) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
            txt_seen = itemView.findViewById(R.id.txt_seen);
            reaction_text = itemView.findViewById(R.id.reaction_text);
            image_message = itemView.findViewById(R.id.image_message);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);

            if (reaction_text == null) {
                throw new RuntimeException("reaction_text TextView not found in layout");
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Chat chat = mChat.get(position);
        if (chat.getSender() != null && chat.getSender().equals(currentUserId)) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }
}
