package com.example.meljo.views.chat;

/* loaded from: classes7.dex */
public class Item2 extends Item {
    private String title;

    public Item2(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    @Override // com.example.meljo.views.chat.Item
    public int getType() {
        return 1;
    }
}
