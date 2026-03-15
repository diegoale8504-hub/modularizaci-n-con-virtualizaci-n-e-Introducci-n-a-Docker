package com.example.demo.app;

import com.example.demo.http.Request;
import com.example.demo.http.Response;

public class HelloController {

    public Response greeting(Request req) {
        String name = req.queryParam("name", "World");
        return Response.text("Hello, " + name + "!");
    }
}