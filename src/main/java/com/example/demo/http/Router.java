package com.example.demo.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Router {
    // key: "GET /path" o "POST /path"
    private final Map<String, Function<Request, Response>> routes = new ConcurrentHashMap<>();

    public void get(String path, Function<Request, Response> handler) {
        routes.put("GET " + path, handler);
    }

    public void post(String path, Function<Request, Response> handler) {
        routes.put("POST " + path, handler);
    }

    public Response route(Request req) {
        Function<Request, Response> h = routes.get(req.method() + " " + req.path());
        if (h == null) return Response.text("Not Found").status(404);

        try {
            return h.apply(req);
        } catch (Exception e) {
            return Response.text("Internal Server Error: " + e.getMessage()).status(500);
        }
    }
}