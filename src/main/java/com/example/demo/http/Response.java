package com.example.demo.http;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class Response {
    private int status = 200;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private byte[] body = new byte[0];

    public static Response text(String s) {
        Response r = new Response();
        r.header("Content-Type", "text/plain; charset=utf-8");
        r.body(s.getBytes(StandardCharsets.UTF_8));
        return r;
    }

    public Response status(int status) { this.status = status; return this; }
    public Response header(String k, String v) { headers.put(k, v); return this; }
    public Response body(byte[] bytes) { this.body = bytes; return this; }

    public int status() { return status; }
    public Map<String, String> headers() { return headers; }
    public byte[] body() { return body; }
}