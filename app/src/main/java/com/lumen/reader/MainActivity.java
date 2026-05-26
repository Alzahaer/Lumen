package com.lumen.reader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.lumen.reader.data.LocalStore;
import com.lumen.reader.model.Book;
import com.lumen.reader.model.FeedItem;
import com.lumen.reader.model.Reflection;
import com.lumen.reader.reader.EpubReaderView;
import com.lumen.reader.reader.PdfReaderView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_BOOK = 42;

    private LocalStore store;
    private FrameLayout root;
    private Book currentBook;
    private String filter = "all";
    private int currentBrightness = 0;
    private String epubTheme = "amoled";
    private String epubFont = "serif";

    private final int black = Color.rgb(5, 5, 6);
    private final int panel = Color.rgb(18, 19, 22);
    private final int glass = Color.argb(186, 24, 26, 30);
    private final int text = Color.rgb(244, 241, 234);
    private final int muted = Color.rgb(155, 158, 164);
    private final int cyan = Color.rgb(185, 247, 230);
    private final int sepia = Color.rgb(245, 232, 210);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(black);
        window.setNavigationBarColor(black);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        store = new LocalStore(this);
        root = new FrameLayout(this);
        root.setBackgroundColor(black);
        setContentView(root);
        seedDemoBooks();
        showLibrary("");
    }

    private void seedDemoBooks() {
        File demo = new File(getFilesDir(), "lumen-welcome.epub");
        if (demo.exists()) return;
        try {
            java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(new FileOutputStream(demo));
            addZip(zip, "mimetype", "application/epub+zip");
            addZip(zip, "chapter-01.xhtml", "<html><body><h1>Lumen</h1><p>Leia no seu ritmo. Este espaco existe para a leitura respirar antes de virar qualquer coisa util.</p><p>Aqui, progresso e apenas uma sombra discreta. A parte viva esta no encontro entre voce e uma frase que muda de temperatura por dentro.</p></body></html>");
            addZip(zip, "chapter-02.xhtml", "<html><body><h2>Diario intelectual</h2><p>Selecione uma passagem mentalmente, escreva o que ficou ecoando e guarde como uma pequena constelacao privada.</p><p>O feed contextual sera o lugar das interpretacoes daquele livro, sem ruido de fora.</p></body></html>");
            zip.close();
            store.upsertBook("Lumen: carta de boas-vindas", "EPUB", demo.getAbsolutePath());
        } catch (Exception ignored) {
        }
    }

    private void addZip(java.util.zip.ZipOutputStream zip, String name, String body) throws Exception {
        zip.putNextEntry(new java.util.zip.ZipEntry(name));
        zip.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void showLibrary(String query) {
        currentBook = null;
        LinearLayout page = vertical();
        page.setPadding(dp(18), dp(22), dp(18), dp(8));
        page.addView(topBar("Lumen", "Leia no seu ritmo.", "+", v -> pickBook()));

        EditText search = input("Buscar na biblioteca viva");
        search.setSingleLine(true);
        search.setText(query);
        search.setOnEditorActionListener((v, actionId, event) -> {
            showLibrary(v.getText().toString());
            return true;
        });
        page.addView(search, matchWrap(0, 12, 0, 8));
        page.addView(filterRow());

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = vertical();
        List<Book> books = store.books(query, filter);
        if (books.isEmpty()) {
            list.addView(emptyState());
        } else {
            for (Book book : books) list.addView(bookCard(book));
        }
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        swap(page);
    }

    private View filterRow() {
        HorizontalScrollView wrap = new HorizontalScrollView(this);
        wrap.setHorizontalScrollBarEnabled(false);
        LinearLayout row = horizontal();
        row.addView(chip("Tudo", "all"));
        row.addView(chip("Em andamento", "reading"));
        row.addView(chip("Favoritos", "favorite"));
        row.addView(chip("Concluidos", "done"));
        wrap.addView(row);
        return wrap;
    }

    private TextView chip(String label, String value) {
        TextView chip = pill(label, value.equals(filter) ? cyan : muted, value.equals(filter) ? Color.argb(34, 185, 247, 230) : Color.argb(34, 255, 255, 255));
        chip.setOnClickListener(v -> {
            filter = value;
            showLibrary("");
        });
        return chip;
    }

    private View bookCard(Book book) {
        LinearLayout card = horizontal();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(round(glass, dp(18), Color.argb(50, 255, 255, 255)));
        card.setOnClickListener(v -> showReader(book.id));

        TextView cover = label(book.type.equals("PDF") ? "PDF" : "EPUB", 18, cyan, Typeface.BOLD);
        cover.setGravity(Gravity.CENTER);
        cover.setBackground(round(Color.rgb(11, 12, 14), dp(16), Color.argb(80, 185, 247, 230)));
        card.addView(cover, new LinearLayout.LayoutParams(dp(82), dp(116)));

        LinearLayout info = vertical();
        info.setPadding(dp(14), 0, 0, 0);
        info.addView(label(book.title, 18, text, Typeface.NORMAL));
        info.addView(label(book.author, 13, muted, Typeface.NORMAL));
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(book.progress);
        info.addView(bar, matchWrap(0, 12, 0, 6));
        info.addView(label(book.progress + "% em silencio", 12, muted, Typeface.NORMAL));
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

        TextView fav = label(book.favorite ? "*" : "o", 24, book.favorite ? cyan : muted, Typeface.NORMAL);
        fav.setGravity(Gravity.CENTER);
        fav.setOnClickListener(v -> {
            store.toggleFavorite(book.id, !book.favorite);
            showLibrary("");
        });
        card.addView(fav, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout.LayoutParams params = matchWrap(0, 8, 0, 10);
        card.setLayoutParams(params);
        return card;
    }

    private TextView emptyState() {
        TextView empty = label("Sua biblioteca ainda esta respirando em branco.\nToque em + para abrir um PDF ou EPUB.", 17, muted, Typeface.NORMAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(18), dp(70), dp(18), dp(70));
        return empty;
    }

    private void showReader(long bookId) {
        currentBook = store.book(bookId);
        if (currentBook == null) return;
        LinearLayout page = vertical();
        page.setBackgroundColor("sepia".equals(epubTheme) ? Color.rgb(33, 27, 22) : black);
        page.addView(readerChrome());

        FrameLayout readingSurface = new FrameLayout(this);
        File file = new File(currentBook.path);
        if ("PDF".equals(currentBook.type)) {
            PdfReaderView pdf = new PdfReaderView(this);
            pdf.setProgressListener(p -> store.updateProgress(currentBook.id, p));
            readingSurface.addView(pdf, new FrameLayout.LayoutParams(-1, -1));
            pdf.post(() -> pdf.load(file, currentBook.progress));
        } else {
            EpubReaderView epub = new EpubReaderView(this);
            epub.setReaderStyle(epubTheme, epubFont);
            epub.setProgressListener(p -> store.updateProgress(currentBook.id, p));
            readingSurface.addView(epub, new FrameLayout.LayoutParams(-1, -1));
            epub.load(file, currentBook.progress);
        }
        page.addView(readingSurface, new LinearLayout.LayoutParams(-1, 0, 1));
        page.addView(readerNav());
        swap(page);
    }

    private View readerChrome() {
        LinearLayout chrome = horizontal();
        chrome.setGravity(Gravity.CENTER_VERTICAL);
        chrome.setPadding(dp(12), dp(12), dp(12), dp(8));
        chrome.setBackgroundColor(Color.argb(180, 5, 5, 6));
        chrome.addView(icon("<", v -> showLibrary("")));
        LinearLayout titleBox = vertical();
        titleBox.addView(label(currentBook.title, 15, text, Typeface.NORMAL));
        titleBox.addView(label(currentBook.progress + "% guardado sem pressa", 11, muted, Typeface.NORMAL));
        chrome.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
        chrome.addView(icon("Aa", v -> showReadingSettings()));
        chrome.addView(icon("+", v -> showReflectionDialog()));
        return chrome;
    }

    private View readerNav() {
        LinearLayout nav = horizontal();
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(10), dp(8), dp(10), dp(12));
        nav.setBackgroundColor(Color.argb(210, 7, 8, 10));
        nav.addView(navButton("Ler", v -> showReader(currentBook.id)));
        nav.addView(navButton("Reflexoes", v -> showReflections()));
        nav.addView(navButton("Feed", v -> showFeed()));
        return nav;
    }

    private void showReadingSettings() {
        LinearLayout box = vertical();
        box.setPadding(dp(18), dp(8), dp(18), dp(4));
        box.addView(label("Ambiente de leitura", 20, text, Typeface.NORMAL));
        box.addView(label("Sem metas, sem ruido. Apenas ajuste a luz.", 13, muted, Typeface.NORMAL));
        box.addView(navButton("AMOLED", v -> {
            epubTheme = "amoled";
            showReader(currentBook.id);
        }));
        box.addView(navButton("Sepia", v -> {
            epubTheme = "sepia";
            showReader(currentBook.id);
        }));
        box.addView(navButton("Fonte serifada", v -> {
            epubFont = "serif";
            showReader(currentBook.id);
        }));
        box.addView(navButton("Fonte limpa", v -> {
            epubFont = "sans";
            showReader(currentBook.id);
        }));
        box.addView(navButton("Brilho interno " + currentBrightness + "%", v -> {
            currentBrightness = currentBrightness >= 80 ? 0 : currentBrightness + 20;
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = currentBrightness == 0 ? -1f : currentBrightness / 100f;
            getWindow().setAttributes(lp);
        }));
        dialog("Leitura", box);
    }

    private void showReflectionDialog() {
        LinearLayout box = vertical();
        EditText excerpt = input("Trecho ou ideia que ficou vibrando");
        EditText note = input("Sua reflexao");
        note.setMinLines(4);
        note.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        box.addView(excerpt);
        box.addView(note, matchWrap(0, 10, 0, 0));
        new AlertDialog.Builder(this)
                .setTitle("Diario intelectual")
                .setView(box)
                .setPositiveButton("Guardar", (d, w) -> {
                    if (note.getText().toString().trim().isEmpty()) return;
                    store.addReflection(currentBook.id, excerpt.getText().toString(), note.getText().toString(), "Leitura atual");
                    Toast.makeText(this, "Reflexao guardada.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Voltar", null)
                .show();
    }

    private void showReflections() {
        LinearLayout page = subPage("Reflexoes", "Um diario intelectual para este livro.");
        LinearLayout list = scrollList(page);
        List<Reflection> notes = store.reflections(currentBook.id);
        if (notes.isEmpty()) list.addView(emptyNote());
        for (Reflection note : notes) list.addView(reflectionCard(note));
        page.addView(readerNav());
        swap(page);
    }

    private View reflectionCard(Reflection note) {
        LinearLayout card = vertical();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(round(glass, dp(18), Color.argb(44, 255, 255, 255)));
        if (note.excerpt != null && note.excerpt.trim().length() > 0) card.addView(label("\"" + note.excerpt + "\"", 15, cyan, Typeface.ITALIC));
        card.addView(label(note.note, 17, text, Typeface.NORMAL), matchWrap(0, 8, 0, 8));
        card.addView(label(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(note.createdAt)), 12, muted, Typeface.NORMAL));
        card.setLayoutParams(matchWrap(0, 8, 0, 10));
        return card;
    }

    private TextView emptyNote() {
        TextView empty = label("Nenhuma reflexao ainda.\nQuando uma frase acender por dentro, toque em +.", 16, muted, Typeface.NORMAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(18), dp(80), dp(18), dp(80));
        return empty;
    }

    private void showFeed() {
        LinearLayout page = subPage("Feed do livro", "Interpretacoes, frases e emocoes apenas daqui.");
        LinearLayout list = scrollList(page);
        for (FeedItem item : mockFeed(currentBook.title)) list.addView(feedCard(item));
        page.addView(readerNav());
        swap(page);
    }

    private List<FeedItem> mockFeed(String title) {
        ArrayList<FeedItem> items = new ArrayList<>();
        items.add(new FeedItem("Clara", "silencio", "Este livro parece pedir menos conclusoes e mais permanencia. Voltei a um paragrafo tres vezes e ele mudou de lugar em mim.", "reflexao"));
        items.add(new FeedItem("Theo", "assombro", "Minha frase favorita ate agora: a ideia de liberdade aqui nao grita, ela respira no intervalo.", "frase"));
        items.add(new FeedItem("Mina", "duvida", "Alguem mais sentiu que o capitulo central fala sobre memoria como abrigo, nao como arquivo?", "debate"));
        items.add(new FeedItem("Lumen", "contexto", "Feed simulado para " + title + ". A estrutura esta pronta para receber um backend social por livro.", "sistema"));
        return items;
    }

    private View feedCard(FeedItem item) {
        LinearLayout card = vertical();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(round(Color.argb(170, 17, 18, 22), dp(18), Color.argb(44, 185, 247, 230)));
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(label(item.name, 15, text, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(pill(item.tag, cyan, Color.argb(28, 185, 247, 230)));
        card.addView(row);
        card.addView(label(item.mood, 12, muted, Typeface.NORMAL));
        card.addView(label(item.body, 16, text, Typeface.NORMAL), matchWrap(0, 10, 0, 0));
        card.setLayoutParams(matchWrap(0, 8, 0, 10));
        return card;
    }

    private LinearLayout subPage(String title, String subtitle) {
        LinearLayout page = vertical();
        page.setPadding(dp(18), dp(22), dp(18), dp(0));
        page.addView(topBar(title, subtitle, "+", v -> showReflectionDialog()));
        return page;
    }

    private LinearLayout scrollList(LinearLayout page) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = vertical();
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return list;
    }

    private void pickBook() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "application/epub+zip", "application/octet-stream"});
        startActivityForResult(intent, PICK_BOOK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_BOOK || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
        try {
            String name = displayName(uri);
            String lower = name.toLowerCase(Locale.ROOT);
            String type = lower.endsWith(".pdf") ? "PDF" : "EPUB";
            File out = new File(getFilesDir(), System.currentTimeMillis() + "-" + cleanName(name));
            InputStream input = getContentResolver().openInputStream(uri);
            FileOutputStream output = new FileOutputStream(out);
            byte[] buffer = new byte[8192];
            int read;
            while (input != null && (read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            if (input != null) input.close();
            output.close();
            long id = store.upsertBook(name.replaceAll("\\.(pdf|epub)$", ""), type, out.getAbsolutePath());
            showReader(id);
        } catch (Exception error) {
            Toast.makeText(this, "Nao foi possivel importar: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String displayName(Uri uri) {
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        }
        String path = uri.getLastPathSegment();
        return path == null ? "livro.epub" : path;
    }

    private String cleanName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private LinearLayout topBar(String title, String subtitle, String action, View.OnClickListener listener) {
        LinearLayout bar = horizontal();
        bar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout copy = vertical();
        copy.addView(label(title, 30, text, Typeface.NORMAL));
        copy.addView(label(subtitle, 14, muted, Typeface.NORMAL));
        bar.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(icon(action, listener));
        return bar;
    }

    private TextView navButton(String label, View.OnClickListener listener) {
        TextView button = pill(label, text, Color.argb(24, 255, 255, 255));
        button.setGravity(Gravity.CENTER);
        button.setOnClickListener(listener);
        button.setLayoutParams(new LinearLayout.LayoutParams(0, dp(46), 1));
        return button;
    }

    private TextView icon(String label, View.OnClickListener listener) {
        TextView icon = label(label, 18, text, Typeface.NORMAL);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(Color.argb(42, 255, 255, 255), dp(18), Color.argb(55, 255, 255, 255)));
        icon.setOnClickListener(listener);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(46), dp(46)));
        return icon;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(muted);
        input.setTextColor(text);
        input.setTextSize(15);
        input.setPadding(dp(16), dp(10), dp(16), dp(10));
        input.setBackground(round(Color.argb(34, 255, 255, 255), dp(18), Color.argb(44, 255, 255, 255)));
        return input;
    }

    private TextView pill(String label, int color, int bg) {
        TextView view = label(label, 13, color, Typeface.NORMAL);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(14), dp(8), dp(14), dp(8));
        view.setBackground(round(bg, dp(999), Color.argb(38, 255, 255, 255)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(40));
        params.setMargins(0, 0, dp(8), dp(8));
        view.setLayoutParams(params);
        return view;
    }

    private TextView label(String value, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(sp);
        view.setLineSpacing(dp(2), 1.0f);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private LinearLayout vertical() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private LinearLayout horizontal() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.HORIZONTAL);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap(int l, int t, int r, int b) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(l), dp(t), dp(r), dp(b));
        return params;
    }

    private android.graphics.drawable.GradientDrawable round(int color, int radius, int stroke) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        shape.setStroke(1, stroke);
        return shape;
    }

    private void swap(View view) {
        root.removeAllViews();
        view.setAlpha(0f);
        root.addView(view, new FrameLayout.LayoutParams(-1, -1));
        view.animate().alpha(1f).setDuration(220).start();
    }

    private void dialog(String title, View view) {
        new AlertDialog.Builder(this).setTitle(title).setView(view).setNegativeButton("Fechar", null).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
