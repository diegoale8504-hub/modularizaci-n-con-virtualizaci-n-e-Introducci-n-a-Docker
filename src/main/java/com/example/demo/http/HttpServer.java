package com.example.demo.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpServer {
    private final Router router;
    private final int port;
    private final ExecutorService pool;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;

    public HttpServer(Router router, int port, int threads) {
        this.router = router;
        this.port = port;
        this.pool = Executors.newFixedThreadPool(threads);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running.set(true);

        while (running.get()) {
            try {
                Socket client = serverSocket.accept();
                pool.submit(() -> handleClient(client));
            } catch (IOException e) {
                // En shutdown se cierra el ServerSocket y accept() puede fallar.
                if (running.get()) throw e;
            }
        }
    }

    public void stopGracefully(Duration timeout) {
        running.set(false);

        // 1) Deja de aceptar nuevas conexiones
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}

        // 2) Termina las tareas actuales de forma ordenada
        pool.shutdown();
        try {
            if (!pool.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void handleClient(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = client.getOutputStream()) {

            Request req = parseRequest(in);
            Response resp = router.route(req);
            writeResponse(out, resp);

        } catch (Exception ignored) {
            // Si quieres, loggea el error
        }
    }

    private Request parseRequest(BufferedReader in) throws IOException {
        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isBlank()) {
            return new Request("GET", "/", Map.of(), Map.of(), "");
        }

        String[] parts = requestLine.split(" ");
        String method = parts[0].trim();
        String fullPath = parts[1].trim();

        String path = fullPath;
        Map<String, String> query = new HashMap<>();
        int qIdx = fullPath.indexOf('?');
        if (qIdx >= 0) {
            path = fullPath.substring(0, qIdx);
            String qs = fullPath.substring(qIdx + 1);
            for (String kv : qs.split("&")) {
                if (kv.isBlank()) continue;
                String[] kvp = kv.split("=", 2);
                String k = urlDecode(kvp[0]);
                String v = kvp.length > 1 ? urlDecode(kvp[1]) : "";
                query.put(k, v);
            }
        }

        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = in.readLine()) != null && !line.isBlank()) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                headers.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        }

        int contentLength = 0;
        if (headers.containsKey("Content-Length")) {
            try { contentLength = Integer.parseInt(headers.get("Content-Length")); }
            catch (NumberFormatException ignored) {}
        }

        String body = "";
        if (contentLength > 0) {
            char[] buf = new char[contentLength];
            int read = in.read(buf);
            if (read > 0) body = new String(buf, 0, read);
        }

        return new Request(method, path, query, headers, body);
    }

    private void writeResponse(OutputStream out, Response r) throws IOException {
        byte[] body = r.body() == null ? new byte[0] : r.body();

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(r.status()).append(" ").append(reason(r.status())).append("\r\n");
        r.headers().forEach((k, v) -> sb.append(k).append(": ").append(v).append("\r\n"));
        sb.append("Content-Length: ").append(body.length).append("\r\n");
        sb.append("Connection: close\r\n");
        sb.append("\r\n");

        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    private static String reason(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "OK";
        };
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}