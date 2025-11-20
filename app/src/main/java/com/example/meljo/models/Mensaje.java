package com.example.meljo.models;

public class Mensaje {
    private String correo;
    private String message;
    private long timestamp;
    private String type;
    private String uid;

    public Mensaje(String uid, String correo, String type, String message, long timestamp) {
        this.uid = uid;
        this.correo = correo;
        this.type = type;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getUid() {
        return this.uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCorreo() {
        return this.correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
