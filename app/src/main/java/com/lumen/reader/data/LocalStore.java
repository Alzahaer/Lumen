package com.lumen.reader.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.lumen.reader.model.Book;
import com.lumen.reader.model.Reflection;

import java.util.ArrayList;
import java.util.List;

public class LocalStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "lumen.db";
    private static final int DB_VERSION = 1;

    public LocalStore(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE books (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, author TEXT, type TEXT NOT NULL, path TEXT NOT NULL UNIQUE, " +
                "progress INTEGER DEFAULT 0, favorite INTEGER DEFAULT 0, last_opened INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE reflections (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, book_id INTEGER NOT NULL, excerpt TEXT, note TEXT NOT NULL, " +
                "chapter TEXT, created_at INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS reflections");
        db.execSQL("DROP TABLE IF EXISTS books");
        onCreate(db);
    }

    public long upsertBook(String title, String type, String path) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM books WHERE path = ?", new String[]{path});
        try {
            if (cursor.moveToFirst()) return cursor.getLong(0);
        } finally {
            cursor.close();
        }
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("author", "Autor desconhecido");
        values.put("type", type);
        values.put("path", path);
        values.put("last_opened", System.currentTimeMillis());
        return db.insert("books", null, values);
    }

    public List<Book> books(String query, String filter) {
        String where = "";
        ArrayList<String> args = new ArrayList<>();
        if (query != null && query.trim().length() > 0) {
            where = "WHERE title LIKE ?";
            args.add("%" + query.trim() + "%");
        }
        if ("favorite".equals(filter)) {
            where += where.isEmpty() ? "WHERE favorite = 1" : " AND favorite = 1";
        } else if ("reading".equals(filter)) {
            where += where.isEmpty() ? "WHERE progress BETWEEN 1 AND 99" : " AND progress BETWEEN 1 AND 99";
        } else if ("done".equals(filter)) {
            where += where.isEmpty() ? "WHERE progress >= 100" : " AND progress >= 100";
        }

        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,title,author,type,path,progress,favorite,last_opened FROM books " + where + " ORDER BY last_opened DESC, title ASC",
                args.toArray(new String[0]));
        ArrayList<Book> result = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                result.add(new Book(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                        cursor.getString(4), cursor.getInt(5), cursor.getInt(6) == 1, cursor.getLong(7)));
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    public Book book(long id) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,title,author,type,path,progress,favorite,last_opened FROM books WHERE id = ?",
                new String[]{String.valueOf(id)});
        try {
            if (cursor.moveToFirst()) {
                return new Book(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                        cursor.getString(4), cursor.getInt(5), cursor.getInt(6) == 1, cursor.getLong(7));
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public void updateProgress(long bookId, int progress) {
        ContentValues values = new ContentValues();
        values.put("progress", Math.max(0, Math.min(100, progress)));
        values.put("last_opened", System.currentTimeMillis());
        getWritableDatabase().update("books", values, "id = ?", new String[]{String.valueOf(bookId)});
    }

    public void toggleFavorite(long bookId, boolean favorite) {
        ContentValues values = new ContentValues();
        values.put("favorite", favorite ? 1 : 0);
        getWritableDatabase().update("books", values, "id = ?", new String[]{String.valueOf(bookId)});
    }

    public void addReflection(long bookId, String excerpt, String note, String chapter) {
        ContentValues values = new ContentValues();
        values.put("book_id", bookId);
        values.put("excerpt", excerpt);
        values.put("note", note);
        values.put("chapter", chapter);
        values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insert("reflections", null, values);
    }

    public List<Reflection> reflections(long bookId) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,book_id,excerpt,note,chapter,created_at FROM reflections WHERE book_id = ? ORDER BY created_at DESC",
                new String[]{String.valueOf(bookId)});
        ArrayList<Reflection> result = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                result.add(new Reflection(cursor.getLong(0), cursor.getLong(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getLong(5)));
            }
        } finally {
            cursor.close();
        }
        return result;
    }
}
