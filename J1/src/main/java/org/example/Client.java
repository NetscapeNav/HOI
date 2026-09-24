package org.example;

import java.net.*;
import java.nio.file.*;

final class Client {
    static void run(String host, int port, String name, Path output, int delay, boolean abort) throws Exception {
        byte[] request = Protocol.request(name);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 10_000);
            socket.getOutputStream().write(request);
            socket.getOutputStream().flush();
            Thread.sleep(delay * 1000L);
            if (abort) {
                System.out.println("Disconnected without reading the response");
                return;
            }
            Protocol.Keys keys = Protocol.read(socket.getInputStream());
            Path keyPath = Path.of(output + ".key");
            Path certPath = Path.of(output + ".crt");
            Files.write(keyPath, keys.privateKey());
            Files.write(certPath, keys.certificate());
            System.out.println("Saved " + keyPath.toAbsolutePath() + " and " + certPath.toAbsolutePath());
        }
    }
}
