package com.lumen.reader.reader;

import android.content.Context;
import android.graphics.Color;
import android.util.Base64;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class EpubReaderView extends WebView {
    public interface ProgressListener {
        void onProgress(int progress);
    }

    private ProgressListener progressListener;
    private int savedProgress;
    private String theme = "amoled";
    private String font = "serif";

    public EpubReaderView(Context context) {
        super(context);
        setBackgroundColor(Color.TRANSPARENT);
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDefaultTextEncodingName("utf-8");
        settings.setBuiltInZoomControls(false);
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                evaluateJavascript("window.scrollTo(0,(document.body.scrollHeight-innerHeight)*" + savedProgress + "/100);", null);
            }
        });
    }

    public void setProgressListener(ProgressListener listener) {
        progressListener = listener;
        addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void progress(int value) {
                if (progressListener != null) progressListener.onProgress(value);
            }
        }, "Lumen");
    }

    public void setReaderStyle(String nextTheme, String nextFont) {
        theme = nextTheme;
        font = nextFont;
        evaluateJavascript("document.documentElement.dataset.theme='" + theme + "';document.documentElement.dataset.font='" + font + "';", null);
    }

    public void load(File file, int progress) {
        savedProgress = progress;
        try {
            String body = readEpubHtml(file);
            String html = shell(body);
            loadDataWithBaseURL("file://" + file.getParentFile().getAbsolutePath() + "/", html, "text/html", "utf-8", null);
        } catch (Exception error) {
            loadData(shell("<h1>Este EPUB não abriu</h1><p>" + escape(error.getMessage()) + "</p>"), "text/html", "utf-8");
        }
    }

    private String readEpubHtml(File file) throws Exception {
        ZipFile zip = new ZipFile(file);
        ArrayList<? extends ZipEntry> entries = Collections.list(zip.entries());
        ArrayList<ZipEntry> docs = new ArrayList<>();
        for (ZipEntry entry : entries) {
            String name = entry.getName().toLowerCase(Locale.ROOT);
            if (!entry.isDirectory() && (name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm"))) docs.add(entry);
        }
        docs.sort(Comparator.comparing(ZipEntry::getName));
        StringBuilder builder = new StringBuilder();
        for (ZipEntry doc : docs) {
            String part = new String(readAll(zip.getInputStream(doc)), StandardCharsets.UTF_8);
            builder.append("<section class='chapter'>").append(extractBody(part)).append("</section>");
        }
        zip.close();
        return builder.length() == 0 ? "<p>Este EPUB não possui capítulos HTML legíveis.</p>" : inlineImages(file, builder.toString());
    }

    private String inlineImages(File file, String html) throws Exception {
        ZipFile zip = new ZipFile(file);
        ArrayList<? extends ZipEntry> entries = Collections.list(zip.entries());
        for (ZipEntry entry : entries) {
            String lower = entry.getName().toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) {
                String mime = lower.endsWith(".png") ? "image/png" : lower.endsWith(".webp") ? "image/webp" : "image/jpeg";
                String data = Base64.encodeToString(readAll(zip.getInputStream(entry)), Base64.NO_WRAP);
                String fileName = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
                html = html.replace(entry.getName(), "data:" + mime + ";base64," + data);
                html = html.replace(fileName, "data:" + mime + ";base64," + data);
            }
        }
        zip.close();
        return html;
    }

    private String shell(String body) {
        return "<!doctype html><html data-theme='" + theme + "' data-font='" + font + "'><head><meta name='viewport' content='width=device-width,initial-scale=1'/>" +
                "<style>html{background:#050506;color:#f4f1ea}html[data-theme='sepia']{background:#211b16;color:#f5e8d2}body{margin:0;padding:34px 22px 96px;line-height:1.72;font-size:19px;letter-spacing:0;font-family:Georgia,serif}html[data-font='sans'] body{font-family:-apple-system,Roboto,sans-serif}html[data-font='mono'] body{font-family:monospace}p{margin:0 0 1.15em}h1,h2,h3{font-weight:500;line-height:1.2;margin:1.6em 0 .7em}img{max-width:100%;height:auto;border-radius:10px}.chapter{max-width:760px;margin:auto}::selection{background:#b9f7e655;color:#fff}</style>" +
                "</head><body>" + body + "<script>function emit(){let h=document.body.scrollHeight-innerHeight;Lumen.progress(Math.max(0,Math.min(100,Math.round(scrollY*100/Math.max(1,h)))))}addEventListener('scroll',emit,{passive:true});setInterval(emit,2200)</script></body></html>";
    }

    private String extractBody(String html) {
        String lower = html.toLowerCase(Locale.ROOT);
        int start = lower.indexOf("<body");
        if (start >= 0) start = html.indexOf(">", start) + 1;
        int end = lower.lastIndexOf("</body>");
        if (start > 0 && end > start) return html.substring(start, end);
        return html;
    }

    private byte[] readAll(InputStream stream) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) output.write(buffer, 0, read);
        stream.close();
        return output.toByteArray();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
