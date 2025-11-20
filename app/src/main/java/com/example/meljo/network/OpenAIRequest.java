package com.example.meljo.network;

import java.util.List;

public class OpenAIRequest {
    public List<Message> messages;
    public String model;

    public OpenAIRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    public static class Message {
        public String content;
        public String role;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
