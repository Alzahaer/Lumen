package com.lumen.reader.model;

public class Book {
    public long id;
    public String title;
    public String author;
    public String type;
    public String path;
    public int progress;
    public boolean favorite;
    public long lastOpened;

    public Book(long id, String title, String author, String type, String path, int progress, boolean favorite, long lastOpened) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.type = type;
        this.path = path;
        this.progress = progress;
        this.favorite = favorite;
        this.lastOpened = lastOpened;
    }
}
