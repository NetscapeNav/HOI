package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class ServerTest {
    private static byte[] reply(String name) {
        return Protocol.success(("key:" + name).getBytes(StandardCharsets.US_ASCII),
                ("crt:" + name).getBytes(StandardCharsets.US_ASCII));
    }

    private static class Running implements AutoCloseable {
        final Server server;
        final ExecutorService runner = Executors.newSingleThreadExecutor();
        final Future<?> task;
        Running(int workers, Server.Generator generator) throws IOException {
            server = new Server(new InetSocketAddress("127.0.0.1", 0), workers, generator);
            task = runner.submit(() -> {
                server.run();
                return null;
            });
        }
        Socket connect() throws IOException {
            Socket socket = new Socket();
            socket.setReceiveBufferSize(1024);
            socket.connect(new InetSocketAddress("127.0.0.1", server.port()), 5000);
            socket.setSoTimeout(10_000);
            return socket;
        }
        Socket request(String name) throws IOException {
            Socket socket = connect();
            socket.getOutputStream().write(Protocol.request(name));
            return socket;
        }
        @Override public void close() throws Exception {
            server.stop();
            try {
                task.get(5, TimeUnit.SECONDS);
            } finally {
                server.close();
                runner.shutdownNow();
            }
        }
    }

    @Test void handles120ClientsAndGeneratesSameNameOnlyOnce() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<Socket> clients = new ArrayList<>();
        try (Running running = new Running(3, name -> {
            calls.incrementAndGet();
            started.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IOException("Test gate timeout");
            }
            return reply(name);
        })) {
            try {
                clients.add(running.request("alice"));
                assertTrue(started.await(5, TimeUnit.SECONDS));
                for (int i = 1; i < 120; i++) {
                    clients.add(running.request("alice"));
                }
                release.countDown();
                for (Socket client : clients) {
                    assertArrayEquals("key:alice".getBytes(StandardCharsets.US_ASCII),
                            Protocol.read(client.getInputStream()).privateKey());
                    assertEquals(-1, client.getInputStream().read());
                }
                try (Socket cached = running.request("alice")) {
                    assertArrayEquals("crt:alice".getBytes(StandardCharsets.US_ASCII),
                            Protocol.read(cached.getInputStream()).certificate());
                }
                assertEquals(1, calls.get());
            } finally {
                release.countDown();
                for (Socket client : clients) {
                    client.close();
                }
            }
        }
    }

    @Test void partialRequestAndHalfCloseStillReceiveResponse() throws Exception {
        try (Running running = new Running(1, ServerTest::reply);
             Socket client = running.connect()) {
            for (byte b : Protocol.request("fragmented")) {
                client.getOutputStream().write(b);
                client.getOutputStream().flush();
                Thread.sleep(5);
            }
            client.shutdownOutput();
            assertArrayEquals("key:fragmented".getBytes(StandardCharsets.US_ASCII),
                    Protocol.read(client.getInputStream()).privateKey());
        }
    }

    @Test void slowAndAbortedClientsDoNotBlockNewWork() throws Exception {
        byte[] large = new byte[Protocol.MAX_FIELD];
        Arrays.fill(large, (byte) 'x');
        try (Running running = new Running(1, name -> name.equals("slow")
                ? Protocol.success(large, large) : reply(name));
             Socket slow = running.request("slow")) {
            try (Socket aborted = running.request("aborted")) {
                aborted.setSoLinger(true, 0);
            }
            try (Socket fast = running.request("fast")) {
                assertArrayEquals("key:fast".getBytes(StandardCharsets.US_ASCII),
                        Protocol.read(fast.getInputStream()).privateKey());
            }
            Protocol.Keys keys = Protocol.read(slow.getInputStream());
            assertArrayEquals(large, keys.privateKey());
            assertArrayEquals(large, keys.certificate());
            try (Socket retry = running.request("aborted")) {
                assertArrayEquals("key:aborted".getBytes(StandardCharsets.US_ASCII),
                        Protocol.read(retry.getInputStream()).privateKey());
            }
        }
    }

    @Test void fixedPoolLimitsParallelGeneration() throws Exception {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        List<Socket> clients = new ArrayList<>();
        try (Running running = new Running(2, name -> {
            int current = active.incrementAndGet();
            peak.accumulateAndGet(current, (previous, next) -> {
                if (next > previous) {
                    return next;
                }
                return previous;
            });
            started.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("Gate timeout");
                }
                return reply(name);
            } finally {
                active.decrementAndGet();
            }
        })) {
            try {
                for (int i = 0; i < 8; i++) {
                    clients.add(running.request("name" + i));
                }
                assertTrue(started.await(5, TimeUnit.SECONDS));
                assertEquals(2, active.get());
                release.countDown();
                for (Socket socket : clients) {
                    Protocol.read(socket.getInputStream());
                }
                assertEquals(2, peak.get());
            } finally {
                release.countDown();
                for (Socket socket : clients) {
                    socket.close();
                }
            }
        }
    }

    @Test void failedGenerationCanBeRetried() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (Running running = new Running(1, name -> {
            if (calls.incrementAndGet() == 1) {
                throw new IOException("Injected failure");
            }
            return reply(name);
        })) {
            try (Socket first = running.request("retry")) {
                assertThrows(IOException.class, () -> Protocol.read(first.getInputStream()));
            }
            try (Socket second = running.request("retry")) {
                assertArrayEquals("key:retry".getBytes(StandardCharsets.US_ASCII),
                        Protocol.read(second.getInputStream()).privateKey());
            }
            assertEquals(2, calls.get());
        }
    }

    @Test void invalidInputDoesNotBreakServer() throws Exception {
        try (Running running = new Running(1, ServerTest::reply)) {
            for (byte[] invalid : List.of(new byte[]{0}, new byte[]{(byte) 255, 0},
                    "a".repeat(Protocol.MAX_NAME + 1).getBytes(StandardCharsets.US_ASCII))) {
                try (Socket client = running.connect()) {
                    client.getOutputStream().write(invalid);
                    assertThrows(IOException.class, () -> Protocol.read(client.getInputStream()));
                }
            }
            String longest = "a".repeat(Protocol.MAX_NAME);
            try (Socket valid = running.request(longest)) {
                Protocol.read(valid.getInputStream());
            }
        }
    }

    @Test void acceptsWorkerCountAboveHardwareThreads() throws Exception {
        try (Running running = new Running(Runtime.getRuntime().availableProcessors() + 1, ServerTest::reply);
             Socket client = running.request("oversubscribed")) {
            Protocol.read(client.getInputStream());
        }
    }

    @Test void clientWritesFilesAndSupportsDelayAndAbort(@TempDir Path temp) throws Exception {
        try (Running running = new Running(1, ServerTest::reply)) {
            Path output = temp.resolve("alice");
            long start = System.nanoTime();
            Client.run("127.0.0.1", running.server.port(), "alice", output, 1, false);
            assertTrue(System.nanoTime() - start >= TimeUnit.SECONDS.toNanos(1));
            assertEquals("key:alice", Files.readString(temp.resolve("alice.key")));
            assertEquals("crt:alice", Files.readString(temp.resolve("alice.crt")));
            Client.run("127.0.0.1", running.server.port(), "abort", temp.resolve("abort"), 0, true);
            assertFalse(Files.exists(temp.resolve("abort.key")));
        }
    }
}
