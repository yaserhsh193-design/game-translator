package com.gametranslator.tv;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class MainActivity extends Activity {

    private TextView status;
    private TextView urlText;
    private ImageView qr;
    private Button overlayBtn;
    private Button testBtn;
    private String shownUrl = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresher = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        urlText = findViewById(R.id.url);
        qr = findViewById(R.id.qr);
        overlayBtn = findViewById(R.id.btn_overlay);
        testBtn = findViewById(R.id.btn_test);

        overlayBtn.setOnClickListener(v -> openOverlaySettings());
        testBtn.setOnClickListener(v -> {
            TranslatorService s = TranslatorService.instance;
            if (s != null) {
                s.showOverlay("تجربة: هنا بتظهر ترجمة اللعبة فوق الشاشة", 26, true);
            }
        });
        findViewById(R.id.btn_stop).setOnClickListener(v -> {
            stopService(new Intent(this, TranslatorService.class));
            finish();
        });

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        Intent service = new Intent(this, TranslatorService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(service);
        } else {
            startService(service);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refresher);
        handler.postDelayed(refresher, 500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refresher);
    }

    private boolean canDraw() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
    }

    private void refresh() {
        String ip = Net.localIp();
        if (ip == null) {
            urlText.setText("");
            qr.setImageBitmap(null);
            shownUrl = null;
        } else {
            int https = TranslatorService.httpsPort;
            String url = https > 0 ? "https://" + ip + ":" + https : "http://" + ip + ":" + TranslatorService.port;
            urlText.setText(url);
            if (!url.equals(shownUrl)) {
                Bitmap b = makeQr(url);
                if (b != null) qr.setImageBitmap(b);
                shownUrl = url;
            }
        }

        StringBuilder sb = new StringBuilder();
        if (ip == null) {
            sb.append("التلفزيون مو متصل بالشبكة. وصّله بالواي فاي أو بالكيبل.\n");
        }
        if (TranslatorService.instance == null) {
            sb.append("المترجم يشتغل الحين...\n");
        }
        if (canDraw()) {
            sb.append("الظهور فوق الشاشة: مسموح ✓");
            overlayBtn.setVisibility(View.GONE);
            testBtn.setVisibility(View.VISIBLE);
        } else {
            sb.append("الظهور فوق الشاشة: غير مسموح. الترجمة بتطلع في الجوال بس.\nاضغط \"السماح بالظهور فوق الشاشة\" وفعّل الخيار لهذا التطبيق.");
            overlayBtn.setVisibility(View.VISIBLE);
            testBtn.setVisibility(View.GONE);
        }
        status.setText(sb.toString());
    }

    private void openOverlaySettings() {
        if (Build.VERSION.SDK_INT < 23) return;
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            return;
        } catch (Exception ignored) {
        }
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
            return;
        } catch (Exception ignored) {
        }
        status.setText("هذا التلفزيون ما فيه شاشة إعداد للإذن. تقدر تفعّله من الكمبيوتر بأمر adb:\n"
                + "adb shell appops set " + getPackageName() + " SYSTEM_ALERT_WINDOW allow\n"
                + "وبدونه الترجمة تشتغل عادي وتطلع في الجوال.");
    }

    private static Bitmap makeQr(String text) {
        try {
            int size = 480;
            BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
            int w = m.getWidth();
            int h = m.getHeight();
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    px[y * w + x] = m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
                }
            }
            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            b.setPixels(px, 0, w, 0, 0, w, h);
            return b;
        } catch (Exception e) {
            return null;
        }
    }
}
