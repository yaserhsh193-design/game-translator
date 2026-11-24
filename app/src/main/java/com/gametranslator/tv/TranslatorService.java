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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.IOException;

public class TranslatorService extends Service {

    static volatile TranslatorService instance;
    static volatile int port = 8080;

    private static final String CHANNEL = "translator";

    private WebServer server;
    private OcrTranslator translator;
    private Handler main;
    private WindowManager wm;
    private TextView overlay;
    private final Runnable hideRunnable = this::removeOverlayNow;

    @Override
    public void onCreate() {
        super.onCreate();
        main = new Handler(Looper.getMainLooper());
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        startAsForeground();
        translator = new OcrTranslator();
        for (int p = 8080; p < 8090; p++) {
            WebServer s = new WebServer(this);
            try {
                s.start(p);
                server = s;
                port = p;
                break;
            } catch (IOException ignored) {
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
        if (server != null) server.stop();
        main.removeCallbacks(hideRunnable);
        removeOverlayNow();
        if (translator != null) translator.close();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                .setContentText("صوّر الشاشة من الجوال عشان تترجم")
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

    OcrTranslator.Result translate(byte[] image) throws Exception {
        return translator.process(image);
    }

    /** Shows Arabic text over whatever is on the TV. Safe to call from any thread. */
    void showOverlay(final String text, final int sizeSp) {
        main.post(() -> {
            if (!canDraw() || TextUtils.isEmpty(text)) return;
            try {
                if (overlay == null) {
                    TextView tv = new TextView(this);
                    tv.setTextColor(Color.WHITE);
                    tv.setBackgroundColor(0xE6000000);
                    tv.setGravity(Gravity.CENTER);
                    tv.setTextDirection(View.TEXT_DIRECTION_RTL);
                    tv.setLineSpacing(0f, 1.2f);
                    tv.setMaxLines(9);
                    tv.setEllipsize(TextUtils.TruncateAt.END);
                    int padH = dp(48);
                    int padV = dp(18);
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
                    lp.gravity = Gravity.BOTTOM;
                    lp.y = dp(24);
                    wm.addView(tv, lp);
                    overlay = tv;
                }
                overlay.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
                overlay.setText(text);
                main.removeCallbacks(hideRunnable);
                long ms = Math.min(45000, 8000 + text.length() * 90L);
                main.postDelayed(hideRunnable, ms);
            } catch (Exception ignored) {
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
