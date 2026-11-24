package com.gametranslator.tv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class OcrTranslator {

    static final class Result {
        String original = "";
        String translated = "";
    }

    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    synchronized Result process(byte[] image) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(image, 0, image.length, bounds);
        int max = Math.max(bounds.outWidth, bounds.outHeight);
        if (max <= 0) throw new Exception("ما قدرت أقرا الصورة");
        int sample = 1;
        while (max / sample > 3000) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bmp = BitmapFactory.decodeByteArray(image, 0, image.length, opts);
        if (bmp == null) throw new Exception("ما قدرت أقرا الصورة");

        Text text;
        try {
            text = Tasks.await(recognizer.process(InputImage.fromBitmap(bmp, 0)), 30, TimeUnit.SECONDS);
        } finally {
            bmp.recycle();
        }

        StringBuilder sb = new StringBuilder();
        for (Text.TextBlock block : text.getTextBlocks()) {
            String t = block.getText().replace('\n', ' ').trim();
            if (letters(t) < 2) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(t);
        }

        Result r = new Result();
        r.original = sb.toString();
        if (!r.original.isEmpty()) {
            r.translated = translate(r.original);
        }
        return r;
    }

    void close() {
        try {
            recognizer.close();
        } catch (Exception ignored) {
        }
    }

    private static int letters(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i))) n++;
        }
        return n;
    }

    /** Free Google Translate endpoint, auto-detect source, Arabic target. Long text is split by line. */
    static String translate(String text) throws Exception {
        List<String> chunks = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String line : text.split("\n")) {
            if (cur.length() > 0 && cur.length() + line.length() > 1200) {
                chunks.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) cur.append('\n');
            cur.append(line.length() > 1200 ? line.substring(0, 1200) : line);
        }
        if (cur.length() > 0) chunks.add(cur.toString());

        StringBuilder out = new StringBuilder();
        for (String chunk : chunks) {
            if (out.length() > 0) out.append('\n');
            out.append(translateChunk(chunk));
        }
        return out.toString().trim();
    }

    private static String translateChunk(String text) throws Exception {
        URL url = new URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=ar&dt=t&q="
                + URLEncoder.encode(text, "UTF-8"));
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new Exception("خدمة الترجمة ردّت بخطأ " + code);
            String body;
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) buf.write(b, 0, n);
                body = buf.toString("UTF-8");
            }
            JSONArray parts = new JSONArray(body).getJSONArray(0);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length(); i++) {
                JSONArray p = parts.optJSONArray(i);
                if (p != null && !p.isNull(0)) sb.append(p.getString(0));
            }
            return sb.toString();
        } finally {
            c.disconnect();
        }
    }
}
