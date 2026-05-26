package com.lumen.reader.model;

public class Reflection {
    public long id;
    public long bookId;
    public String excerpt;
    public String note;
    public String chapter;
    public long createdAt;

    public Reflection(long id, long bookId, String excerpt, String note, String chapter, long createdAt) {
        this.id = id;
        this.bookId = bookId;
        this.excerpt = excerpt;
        this.note = note;
        this.chapter = chapter;
        this.createdAt = createdAt;
    }
}
