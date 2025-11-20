package com.example.meljo.network;

import java.util.List;

/* loaded from: classes9.dex */
public class WitResponse {
    private List<Intent> intents;
    private String text;

    public List<Intent> getIntents() {
        return this.intents;
    }

    public String getText() {
        return this.text;
    }

    public static class Intent {
        private String id;
        private String name;

        public String getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }
    }
}
