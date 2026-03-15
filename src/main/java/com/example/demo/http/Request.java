package com.example.demo.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final Map<String, String> query;
    private final Map<String, String> headers;
    private final String body;

    public Request(String method, String path, Map<String, String> query, Map<String, String> headers, String body) {
        this.method = method;
        this.path = path;
        this.query = query == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(query));
        this.headers = headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(headers));
        this.body = body;
    }

    public String method() { return method; }
    public String path() { return path; }
    public Map<String, String> query() { return query; }
    public Map<String, String> headers() { return headers; }
    public String body() { return body; }

    public String queryParam(String key, String defaultValue) {
        return query.getOrDefault(key, defaultValue);
    }
}