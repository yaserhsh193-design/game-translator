package com.gametranslator.tv;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tiny HTTP server: serves the phone page and receives screenshots to translate. */
final class WebServer {

    private static final int MAX_BODY = 25 * 1024 * 1024;

    private final TranslatorService svc;
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private ServerSocket socket;
    private volatile boolean running;

    WebServer(TranslatorService svc) {
        this.svc = svc;
    }

    void start(int port) throws IOException {
        ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(true);
        try {
            ss.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            ss.close();
            pool.shutdownNow();
            throw e;
        }
        socket = ss;
        running = true;
        Thread t = new Thread(() -> {
            while (running) {
                try {
                    final Socket s = ss.accept();
                    pool.execute(() -> handle(s));
                } catch (IOException e) {
                    if (!running) break;
                }
            }
        }, "web-server");
        t.setDaemon(true);
        t.start();
    }

    void stop() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
        pool.shutdownNow();
    }

    private void handle(Socket s) {
        try (Socket sock = s) {
            sock.setSoTimeout(60000);
            InputStream in = new BufferedInputStream(sock.getInputStream());
            OutputStream out = sock.getOutputStream();
            String head = readHead(in);
            if (head == null) return;
            String[] lines = head.split("\r\n");
            String[] req = lines[0].split(" ");
            if (req.length < 2) return;
            String method = req[0];
            String target = req[1];
            int length = 0;
            for (int i = 1; i < lines.length; i++) {
                String l = lines[i].toLowerCase(Locale.ROOT);
                if (l.startsWith("content-length:")) {
                    try {
                        length = Integer.parseInt(l.substring(15).trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            String path = target;
            String query = "";
            int q = target.indexOf('?');
            if (q >= 0) {
                path = target.substring(0, q);
                query = target.substring(q + 1);
            }

            if ("GET".equals(method) && ("/".equals(path) || "/index.html".equals(path))) {
                send(out, 200, "text/html; charset=utf-8", readAsset("phone.html"));
            } else if ("GET".equals(method) && "/status".equals(path)) {
                JSONObject j = new JSONObject();
                j.put("ok", true);
                j.put("overlay", svc.canDraw());
                sendJson(out, 200, j);
            } else if ("POST".equals(method) && "/translate".equals(path)) {
                JSONObject j = new JSONObject();
                if (length <= 0 || length > MAX_BODY) {
                    j.put("ok", false);
                    j.put("error", "حجم الصورة غير مقبول");
                    sendJson(out, 400, j);
                    return;
                }
                byte[] body = readFully(in, length);
                try {
                    OcrTranslator.Result r = svc.translate(body);
                    boolean showOnTv = param(query, "tv").equals("1");
                    int size = 26;
                    try {
                        size = Math.max(14, Math.min(60, Integer.parseInt(param(query, "size"))));
                    } catch (NumberFormatException ignored) {
                    }
                    if (showOnTv && !r.translated.isEmpty()) svc.showOverlay(r.translated, size);
                    j.put("ok", true);
                    j.put("original", r.original);
                    j.put("translated", r.translated);
                    j.put("overlay", svc.canDraw());
                    sendJson(out, 200, j);
                } catch (Exception e) {
                    j.put("ok", false);
                    j.put("error", e.getMessage() == null ? e.toString() : e.getMessage());
                    sendJson(out, 500, j);
                }
            } else if ("POST".equals(method) && "/show".equals(path)) {
                JSONObject j = new JSONObject();
                if (length > 0 && length < 65536) {
                    String text = new String(readFully(in, length), StandardCharsets.UTF_8);
                    int size = 26;
                    try {
                        size = Math.max(14, Math.min(60, Integer.parseInt(param(query, "size"))));
                    } catch (NumberFormatException ignored) {
                    }
                    svc.showOverlay(text, size);
                }
                j.put("ok", true);
                sendJson(out, 200, j);
            } else if ("POST".equals(method) && "/hide".equals(path)) {
                if (length > 0 && length < 65536) readFully(in, length);
                svc.hideOverlay();
                JSONObject j = new JSONObject();
                j.put("ok", true);
                sendJson(out, 200, j);
            } else {
                send(out, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
    }

    private static String param(String query, String name) {
        for (String kv : query.split("&")) {
            int e = kv.indexOf('=');
            if (e > 0 && kv.substring(0, e).equals(name)) return kv.substring(e + 1);
        }
        return "";
    }

    private static String readHead(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int matched = 0;
        int b;
        while ((b = in.read()) != -1) {
            buf.write(b);
            if (buf.size() > 32768) return null;
            if ((matched == 0 || matched == 2) && b == '\r') matched++;
            else if ((matched == 1 || matched == 3) && b == '\n') matched++;
            else matched = (b == '\r') ? 1 : 0;
            if (matched == 4) {
                String s = buf.toString("ISO-8859-1");
                return s.substring(0, s.length() - 4);
            }
        }
        return null;
    }

    private static byte[] readFully(InputStream in, int length) throws IOException {
        byte[] data = new byte[length];
        int off = 0;
        while (off < length) {
            int n = in.read(data, off, length - off);
            if (n < 0) throw new IOException("connection closed");
            off += n;
        }
        return data;
    }

    private byte[] readAsset(String name) throws IOException {
        try (InputStream in = svc.getAssets().open(name)) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) buf.write(b, 0, n);
            return buf.toByteArray();
        }
    }

    private static void sendJson(OutputStream out, int code, JSONObject j) throws IOException {
        send(out, code, "application/json; charset=utf-8", j.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void send(OutputStream out, int code, String type, byte[] body) throws IOException {
        String reason = code == 200 ? "OK" : code == 404 ? "Not Found" : code == 400 ? "Bad Request" : "Error";
        String h = "HTTP/1.1 " + code + " " + reason + "\r\n"
                + "Content-Type: " + type + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n\r\n";
        out.write(h.getBytes(StandardCharsets.ISO_8859_1));
        out.write(body);
        out.flush();
    }
}
