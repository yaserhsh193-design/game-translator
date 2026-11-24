package com.gametranslator.tv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

public class TranslatorService extends Service {

    static volatile TranslatorService instance;
    static volatile int port = 8080;
    static volatile int httpsPort = -1;

    private static final String CHANNEL = "translator";

    private WebServer httpServer;
    private WebServer httpsServer;
    private OcrTranslator translator;
    private Handler main;
    private WindowManager wm;
    private TextView overlay;
    private boolean overlayTop = true;
    private final Runnable hideRunnable = this::removeOverlayNow;

    volatile String lastError = "";

    // Live (auto) mode: remember what is on screen so the same text is not re-translated every frame.
    private String autoKey = "";
    private String autoTranslation = "";

    @Override
    public void onCreate() {
        super.onCreate();
        installCrashLogger();
        main = new Handler(Looper.getMainLooper());
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        startAsForeground();
        try {
            translator = new OcrTranslator();
        } catch (Throwable t) {
            lastError = "OCR init: " + stack(t);
        }

        for (int p = 8080; p < 8090 && httpServer == null; p++) {
            WebServer s = new WebServer(this, null);
            try {
                s.start(p);
                httpServer = s;
                port = p;
            } catch (Exception ignored) {
            }
        }

        SSLServerSocketFactory ssl = null;
        try {
            ssl = sslFactory();
        } catch (Throwable t) {
            lastError = "HTTPS init: " + stack(t);
        }
        if (ssl != null) {
            for (int p = 8443; p < 8453 && httpsServer == null; p++) {
                WebServer s = new WebServer(this, ssl);
                try {
                    s.start(p);
                    httpsServer = s;
                    httpsPort = p;
                } catch (Exception ignored) {
                }
            }
        }
        instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
        if (httpServer != null) httpServer.stop();
        if (httpsServer != null) httpsServer.stop();
        httpsPort = -1;
        main.removeCallbacks(hideRunnable);
        removeOverlayNow();
        if (translator != null) translator.close();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static SSLServerSocketFactory sslFactory() throws Exception {
        char[] pass = TlsCert.PASSWORD.toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new ByteArrayInputStream(Base64.decode(TlsCert.P12_BASE64, Base64.DEFAULT)), pass);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, pass);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);
        return ctx.getServerSocketFactory();
    }

    private void installCrashLogger() {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            try (FileOutputStream out = new FileOutputStream(new File(getFilesDir(), "crash.txt"))) {
                out.write(("Thread " + thread.getName() + "\n" + stack(e)).getBytes(StandardCharsets.UTF_8));
            } catch (Exception ignored) {
            }
            if (prev != null) prev.uncaughtException(thread, e);
        });
    }

    String debugInfo() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("version ").append(getPackageManager().getPackageInfo(getPackageName(), 0).versionName).append('\n');
        } catch (Exception ignored) {
        }
        sb.append("sdk ").append(Build.VERSION.SDK_INT).append(", ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        sb.append("abi ").append(Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "?").append('\n');
        sb.append("https ").append(httpsPort).append(", overlay ").append(canDraw()).append('\n');
        if (!lastError.isEmpty()) sb.append("last error:\n").append(lastError).append('\n');
        File crash = new File(getFilesDir(), "crash.txt");
        if (crash.exists()) {
            try (FileInputStream in = new FileInputStream(crash)) {
                byte[] b = new byte[(int) Math.min(crash.length(), 8000)];
                int n = in.read(b);
                sb.append("last crash:\n").append(new String(b, 0, Math.max(n, 0), StandardCharsets.UTF_8));
            } catch (Exception ignored) {
            }
        }
        return sb.toString();
    }

    static String stack(Throwable t) {
        StringWriter w = new StringWriter();
        t.printStackTrace(new PrintWriter(w));
        String s = w.toString();
        return s.length() > 4000 ? s.substring(0, 4000) : s;
    }

    private void startAsForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "مترجم الألعاب", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification n = b.setContentTitle("مترجم الألعاب شغّال")
                .setContentText("وجّه كاميرا الجوال على الشاشة")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, n);
        }
    }

    boolean canDraw() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
    }

    /** One photo: read text, translate, return both. */
    OcrTranslator.Result translateOnce(byte[] image) throws Exception {
        if (translator == null) throw new Exception("قارئ النص ما اشتغل على هذا التلفزيون");
        OcrTranslator.Result r = new OcrTranslator.Result();
        r.original = translator.readText(image);
        if (!r.original.isEmpty()) r.translated = OcrTranslator.translate(r.original);
        return r;
    }

    /** Live frame sent as a photo: read it here, then translate. */
    OcrTranslator.Result translateLive(byte[] image) throws Exception {
        if (translator == null) throw new Exception("قارئ النص ما اشتغل على هذا التلفزيون");
        return translateText(translator.readText(image), true);
    }

    /** Text already read by the phone. In live mode Google is only called when the text actually changed. */
    synchronized OcrTranslator.Result translateText(String text, boolean live) throws Exception {
        OcrTranslator.Result r = new OcrTranslator.Result();
        r.original = text == null ? "" : text.trim();
        if (r.original.isEmpty()) return r;
        String key = OcrTranslator.normalize(r.original);
        if (live && !autoTranslation.isEmpty() && OcrTranslator.similar(key, autoKey)) {
            r.translated = autoTranslation;
            r.same = true;
            return r;
        }
        r.translated = OcrTranslator.translate(r.original);
        autoKey = key;
        autoTranslation = r.translated;
        return r;
    }

    /** Shows Arabic text over whatever is on the TV. Safe to call from any thread. */
    void showOverlay(final String text, final int sizeSp, final boolean top) {
        main.post(() -> {
            if (!canDraw() || TextUtils.isEmpty(text)) return;
            try {
                if (overlay != null && overlayTop != top) removeOverlayNow();
                if (overlay == null) {
                    TextView tv = new TextView(this);
                    tv.setTextColor(Color.WHITE);
                    tv.setBackgroundColor(0xE6000000);
                    tv.setGravity(Gravity.CENTER);
                    tv.setTextDirection(View.TEXT_DIRECTION_RTL);
                    tv.setLineSpacing(0f, 1.2f);
                    tv.setMaxLines(8);
                    tv.setEllipsize(TextUtils.TruncateAt.END);
                    int padH = dp(48);
                    int padV = dp(14);
                    tv.setPadding(padH, padV, padH, padV);

                    int type = Build.VERSION.SDK_INT >= 26
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE;
                    WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            type,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                            PixelFormat.TRANSLUCENT);
                    lp.gravity = top ? Gravity.TOP : Gravity.BOTTOM;
                    lp.y = dp(20);
                    wm.addView(tv, lp);
                    overlay = tv;
                    overlayTop = top;
                }
                overlay.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
                if (!text.contentEquals(overlay.getText())) overlay.setText(text);
                main.removeCallbacks(hideRunnable);
                long ms = Math.min(45000, 8000 + text.length() * 90L);
                main.postDelayed(hideRunnable, ms);
            } catch (Throwable t) {
                lastError = "overlay: " + stack(t);
                overlay = null;
            }
        });
    }

    void hideOverlay() {
        main.post(() -> {
            main.removeCallbacks(hideRunnable);
            removeOverlayNow();
        });
    }

    private void removeOverlayNow() {
        if (overlay != null) {
            try {
                wm.removeView(overlay);
            } catch (Exception ignored) {
            }
            overlay = null;
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
