package com.lumen.reader.model;

public class FeedItem {
    public final String name;
    public final String mood;
    public final String body;
    public final String tag;

    public FeedItem(String name, String mood, String body, String tag) {
        this.name = name;
        this.mood = mood;
        this.body = body;
        this.tag = tag;
    }
}
