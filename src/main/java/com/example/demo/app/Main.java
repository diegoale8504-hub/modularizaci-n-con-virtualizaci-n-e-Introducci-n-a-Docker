package com.example.demo.app;

import com.example.demo.http.HttpServer;
import com.example.demo.http.Response;
import com.example.demo.http.Router;

import java.time.Duration;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = getPort();
        int threads = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);

        Router router = new Router();
        HttpServer server = new HttpServer(router, port, threads);

        HelloController hello = new HelloController();

        // endpoints
        router.get("/greeting", hello::greeting);
        router.get("/health", req -> Response.text("OK"));

        // endpoint para apagar elegante
        router.post("/shutdown", req -> {
            new Thread(() -> server.stopGracefully(Duration.ofSeconds(10))).start();
            return Response.text("Shutting down...");
        });

        // para apagado elegante cuando Docker/EC2 mande SIGTERM
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                server.stopGracefully(Duration.ofSeconds(10))
        ));

        System.out.println("Listening on port " + port + " using " + threads + " threads");
        server.start();
    }

    private static int getPort() {
        String p = System.getenv("PORT");
        if (p != null && !p.isBlank()) return Integer.parseInt(p);
        return 6000; // para que coincida con tu Dockerfile actual
    }
}