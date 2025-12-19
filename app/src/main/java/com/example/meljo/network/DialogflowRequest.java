package com.example.meljo.network;

public class DialogflowRequest {

    public QueryInput queryInput;

    public DialogflowRequest(String text) {
        this.queryInput = new QueryInput(text);
    }

    public static class QueryInput {
        public TextInput text;

        public QueryInput(String text) {
            this.text = new TextInput(text);
        }
    }

    public static class TextInput {
        public String text;
        public String languageCode = "es";

        public TextInput(String text) {
            this.text = text;
        }
    }
}
