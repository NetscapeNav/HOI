package org.example;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.*;

public final class Main {
    public static void main(String[] args) {
        try {
            if (args.length == 0 || args[0].equals("--help")) {
                help();
                return;
            }
            Options options = new Options(args);
            switch (args[0]) {
                case "init-ca" -> {
                    options.allow("out");
                    Path path = Path.of(options.get("out", "ca.key"));
                    Crypto.createSigningKey(path);
                    System.out.println("Signing key created: " + path.toAbsolutePath());
                }
                case "server" -> {
                    options.allow("bind", "port", "workers", "ca-key", "issuer");
                    int port = options.number("port", 9000, 1, 65535);
                    int workers = options.number("workers",
                            Runtime.getRuntime().availableProcessors(), 1, Integer.MAX_VALUE);
                    Crypto crypto = new Crypto(Path.of(options.required("ca-key")),
                            options.get("issuer", "CN=J1 Lab CA"));
                    try (Server server = new Server(new InetSocketAddress(
                            options.get("bind", "0.0.0.0"), port), workers, crypto::generate)) {
                        Thread hook = new Thread(server::stop, "shutdown");
                        Runtime.getRuntime().addShutdownHook(hook);
                        System.out.printf("Listening on %s:%d, workers=%d%n",
                                options.get("bind", "0.0.0.0"), server.port(), workers);
                        try {
                            server.run();
                        } finally {
                            try {
                                Runtime.getRuntime().removeShutdownHook(hook);
                            } catch (IllegalStateException ignored) {
                                /* JVM is shutting down. */
                            }
                        }
                    }
                }
                case "client" -> {
                    options.allow("host", "port", "name", "out", "delay", "abort");
                    Client.run(options.get("host", "localhost"),
                            options.number("port", 9000, 1, 65535),
                            options.required("name"), Path.of(options.get("out", "client")),
                            options.number("delay", 0, 0, Integer.MAX_VALUE), options.flag("abort"));
                }
                default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void help() {
        System.out.println("""
                J1 RSA key server (Java 21+)
                init-ca [--out ca.key]
                server --ca-key ca.key [--issuer "CN=J1 Lab CA"]
                       [--bind 0.0.0.0] [--port 9000] [--workers 4]
                client --name alice [--host localhost] [--port 9000]
                       [--out alice] [--delay 5] [--abort]
                Output: <out>.key (PKCS#8 PEM), <out>.crt (X.509 PEM).
                --abort closes after sending the request and optional delay.
                """);
    }

    private static class Options {
        private final Map<String, String> values = new HashMap<>();
        Options(String[] args) {
            for (int i = 1; i < args.length; i++) {
                String option = args[i];
                if (!option.startsWith("--")) {
                    throw new IllegalArgumentException("Expected option: " + option);
                }
                String key = option.substring(2);
                String value;
                if (key.equals("abort")) {
                    value = "true";
                } else {
                    if (++i == args.length) {
                        throw new IllegalArgumentException("Missing value: " + option);
                    }
                    value = args[i];
                }
                if (values.putIfAbsent(key, value) != null) {
                    throw new IllegalArgumentException("Duplicate option: " + option);
                }
            }
        }
        void allow(String... names) {
            Set<String> allowed = Set.of(names);
            for (String key : values.keySet()) {
                if (!allowed.contains(key)) {
                    throw new IllegalArgumentException("Unknown option: --" + key);
                }
            }
        }
        String get(String key, String fallback) {
            return values.getOrDefault(key, fallback);
        }
        boolean flag(String key) {
            return values.containsKey(key);
        }
        String required(String key) {
            if (!values.containsKey(key)) {
                throw new IllegalArgumentException("Required: --" + key);
            }
            return values.get(key);
        }
        int number(String key, int fallback, int min, int max) {
            int value = Integer.parseInt(get(key, Integer.toString(fallback)));
            if (value < min || value > max) {
                throw new IllegalArgumentException("Invalid --" + key);
            }
            return value;
        }
    }
}
