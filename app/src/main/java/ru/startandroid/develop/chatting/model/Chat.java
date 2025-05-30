package ru.startandroid.develop.chatting.model;

public class Chat {

    private String sender;
    private String receiver;
    private String message;
    private String messageType = "text";
    private String fileName;
    private boolean isseen;
    private String lastMessage;
    private String messageId;
    private String reaction;
    private long timestamp;
    public Chat(String sender, String receiver, String message, boolean isseen,
                String lastMessage, String messageId, String reaction,
                String messageType, String fileName, long timestamp) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.isseen = isseen;
        this.lastMessage = lastMessage;
        this.messageId = messageId;
        this.reaction = reaction;
        this.messageType = messageType;
        this.fileName = fileName;
        this.timestamp = timestamp;
    }

    public Chat() {
    }

    // Getters and Setters
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isIsseen() { return isseen; }
    public void setIsseen(boolean isseen) { this.isseen = isseen; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
