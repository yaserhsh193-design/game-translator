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
import java.util.Locale;
import java.util.concurrent.TimeUnit;

final class OcrTranslator {

    static final class Result {
        String original = "";
        String translated = "";
        boolean same = false;
    }

    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    /** Reads the text in a photo. Lines inside one block are joined so sentences translate as a whole. */
    synchronized String readText(byte[] image) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(image, 0, image.length, bounds);
        int max = Math.max(bounds.outWidth, bounds.outHeight);
        if (max <= 0) throw new Exception("ما قدرت أقرا الصورة");
        int sample = 1;
        while (max / sample > 2400) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bmp = BitmapFactory.decodeByteArray(image, 0, image.length, opts);
        if (bmp == null) throw new Exception("ما قدرت أقرا الصورة");

        Text text;
        try {
            text = Tasks.await(recognizer.process(InputImage.fromBitmap(bmp, 0)), 40, TimeUnit.SECONDS);
        } finally {
            bmp.recycle();
        }

        StringBuilder sb = new StringBuilder();
        for (Text.TextBlock block : text.getTextBlocks()) {
            String t = block.getText().replace('\n', ' ').trim();
            if (letters(t) < 3) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(t);
        }
        return sb.toString();
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

    static String normalize(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c)) sb.append(c);
        }
        return sb.toString();
    }

    /** True when two OCR readings are almost the same text (camera noise changes a few letters). */
    static boolean similar(String a, String b) {
        if (a.equals(b)) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        if (a.length() > 600) a = a.substring(0, 600);
        if (b.length() > 600) b = b.substring(0, 600);
        int longest = Math.max(a.length(), b.length());
        if (Math.abs(a.length() - b.length()) > longest * 0.2) return false;
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()] <= longest * 0.15;
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
