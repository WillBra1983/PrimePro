package com.primeprofast.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

final class PPFResultPdf {
    private static final int WIDTH = 595;
    private static final int HEIGHT = 842;
    private static final int MARGIN = 42;
    private static final int BODY_TOP = 101;
    private static final int BODY_BOTTOM = 796;
    private static final int BODY_WIDTH = WIDTH - 2 * MARGIN;

    private PPFResultPdf() {}

    static File write(Context context, File directory, String title, String text) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Não foi possível preparar o PDF.");
        }
        File file = new File(directory, fileName(title));
        TextPaint body = paint(10, Typeface.MONOSPACE, 0xFF1A2633);
        TextPaint heading = paint(18, Typeface.DEFAULT_BOLD, Color.BLACK);
        TextPaint note = paint(11, Typeface.DEFAULT, Color.DKGRAY);
        Drawable logo = context.getDrawable(R.mipmap.ic_launcher);
        if (logo != null) logo.setBounds(MARGIN, 34, MARGIN + 44, 78);
        StaticLayout layout = StaticLayout.Builder.obtain(text, 0, text.length(), body, BODY_WIDTH)
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .setLineSpacing(3, 1)
            .setIncludePad(false)
            .build();
        String subtitle = TextUtils.ellipsize(title, note, WIDTH - MARGIN - 98,
            TextUtils.TruncateAt.END).toString();

        PdfDocument pdf = new PdfDocument();
        try {
            int line = 0;
            int pageNumber = 1;
            do {
                int top = layout.getLineTop(line);
                int end = line + 1;
                while (end < layout.getLineCount()
                    && layout.getLineTop(end + 1) - top <= BODY_BOTTOM - BODY_TOP) {
                    end++;
                }
                PdfDocument.Page page = pdf.startPage(
                    new PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, pageNumber).create());
                try {
                    Canvas canvas = page.getCanvas();
                    if (logo != null) logo.draw(canvas);
                    canvas.drawText("PrimeProFast", 98, 54, heading);
                    canvas.drawText(subtitle, 98, 71, note);
                    canvas.drawText("PrimeProFast  |  P\u00e1gina " + pageNumber, MARGIN, 822, note);

                    canvas.save();
                    canvas.clipRect(MARGIN, BODY_TOP, WIDTH - MARGIN,
                        BODY_TOP + layout.getLineTop(end) - top);
                    canvas.translate(MARGIN, BODY_TOP - top);
                    layout.draw(canvas);
                    canvas.restore();
                } finally {
                    pdf.finishPage(page);
                }
                line = end;
                pageNumber++;
            } while (line < layout.getLineCount());

            try (FileOutputStream stream = new FileOutputStream(file)) {
                pdf.writeTo(stream);
            }
        } catch (IOException | RuntimeException e) {
            file.delete();
            throw e;
        } finally {
            pdf.close();
        }
        return file;
    }

    static String fileName(String title) {
        return "PrimeProFast - " + title.replaceAll("[^\\p{L}\\p{N} ._-]", "_") + ".pdf";
    }

    private static TextPaint paint(float size, Typeface typeface, int color) {
        TextPaint paint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        paint.setTextSize(size);
        paint.setTypeface(typeface);
        paint.setColor(color);
        return paint;
    }
}
