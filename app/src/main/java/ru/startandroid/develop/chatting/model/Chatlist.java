package ru.startandroid.develop.chatting.model;

public class Chatlist {
    private String id;
    private long timestamp;

    public Chatlist() { }

    public Chatlist(String id, long timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
