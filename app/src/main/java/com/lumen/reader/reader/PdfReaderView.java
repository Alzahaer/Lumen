package com.lumen.reader.reader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

public class PdfReaderView extends ScrollView {
    public interface ProgressListener {
        void onProgress(int progress);
    }

    private final LinearLayout pages;
    private ProgressListener progressListener;

    public PdfReaderView(Context context) {
        super(context);
        setFillViewport(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setBackgroundColor(Color.TRANSPARENT);
        pages = new LinearLayout(context);
        pages.setOrientation(LinearLayout.VERTICAL);
        pages.setPadding(dp(18), dp(12), dp(18), dp(80));
        addView(pages, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        getViewTreeObserver().addOnScrollChangedListener(this::emitProgress);
    }

    public void setProgressListener(ProgressListener listener) {
        progressListener = listener;
    }

    public void load(File file, int savedProgress) {
        pages.removeAllViews();
        try {
            ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(descriptor);
            int width = Math.max(1, getResources().getDisplayMetrics().widthPixels - dp(36));
            for (int i = 0; i < renderer.getPageCount(); i++) {
                PdfRenderer.Page page = renderer.openPage(i);
                float ratio = (float) page.getHeight() / Math.max(1, page.getWidth());
                Bitmap bitmap = Bitmap.createBitmap(width, Math.max(1, (int) (width * ratio)), Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();

                ImageView image = new ImageView(getContext());
                image.setImageBitmap(bitmap);
                image.setAdjustViewBounds(true);
                image.setBackgroundColor(Color.WHITE);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, dp(14));
                pages.addView(image, params);
            }
            renderer.close();
            descriptor.close();
            post(() -> scrollTo(0, Math.round((getChildAt(0).getHeight() - getHeight()) * savedProgress / 100f)));
        } catch (Exception error) {
            TextView message = new TextView(getContext());
            message.setText("Não foi possível abrir este PDF.\n" + error.getMessage());
            message.setTextColor(Color.rgb(244, 241, 234));
            message.setGravity(Gravity.CENTER);
            message.setPadding(dp(20), dp(80), dp(20), dp(80));
            pages.addView(message);
        }
    }

    private void emitProgress() {
        if (progressListener == null || getChildCount() == 0) return;
        View child = getChildAt(0);
        int max = Math.max(1, child.getHeight() - getHeight());
        progressListener.onProgress(Math.round(getScrollY() * 100f / max));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
