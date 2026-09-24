package org.example;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

final class Server implements AutoCloseable {
    @FunctionalInterface
    interface Generator {
        byte[] generate(String name) throws Exception;
    }

    private static class Entry {
        byte[] response;
        final Set<Connection> waiting = new HashSet<>();
    }
    private static class Connection {
        final ByteBuffer input = ByteBuffer.allocate(Protocol.MAX_NAME + 1);
        final SelectionKey key;
        Entry pending;
        ByteBuffer output;
        boolean requested;
        Connection(SelectionKey key) {
            this.key = key;
        }
    }
    private record Completion(String name, Entry entry, byte[] response, boolean failed) {}

    private final Selector selector;
    private final ServerSocketChannel listener;
    private final ExecutorService workers;
    private final Generator generator;
    private final Map<String, Entry> cache = new HashMap<>();
    private final Queue<Completion> completed = new ConcurrentLinkedQueue<>();
    private volatile boolean running = true;

    Server(InetSocketAddress address, int workerCount, Generator generator) throws IOException {
        if (workerCount < 1) {
            throw new IllegalArgumentException("workers must be positive");
        }
        this.generator = generator;
        selector = Selector.open();
        listener = ServerSocketChannel.open();
        try {
            listener.configureBlocking(false);
            listener.bind(address, 256);
            listener.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException | RuntimeException e) {
            listener.close();
            selector.close();
            throw e;
        }
        workers = Executors.newFixedThreadPool(workerCount);
    }

    int port() throws IOException {
        return ((InetSocketAddress) listener.getLocalAddress()).getPort();
    }

    void run() throws IOException {
        while (running) {
            drainCompletions();
            selector.select();
            drainCompletions();
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();
                if (!key.isValid()) {
                    continue;
                }
                if (key.isAcceptable()) {
                    accept();
                    continue;
                }
                Connection connection = (Connection) key.attachment();
                try {
                    if (key.isReadable()) {
                        read(connection);
                    }
                    if (key.isValid() && key.isWritable()) {
                        write(connection);
                    }
                } catch (IOException | CancelledKeyException e) {
                    disconnect(connection);
                }
            }
        }
    }

    private void accept() throws IOException {
        for (int i = 0; i < 64; i++) {
            SocketChannel channel = listener.accept();
            if (channel == null) {
                return;
            }
            try {
                channel.configureBlocking(false);
                SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
                key.attach(new Connection(key));
            } catch (IOException e) {
                channel.close();
            }
        }
    }

    private void read(Connection connection) throws IOException {
        SocketChannel channel = (SocketChannel) connection.key.channel();
        int before = connection.input.position();
        int n = channel.read(connection.input);
        if (n < 0) {
            if (!connection.requested) {
                disconnect(connection);
            } else {
                connection.key.interestOps(connection.output == null ? 0 : SelectionKey.OP_WRITE);
            }
            return;
        }
        if (n == 0) {
            return;
        }
        if (connection.requested) {
            disconnect(connection);
            return;
        }
        for (int i = before; i < connection.input.position(); i++) {
            int value = connection.input.get(i) & 0xff;
            if (value > 127) {
                respond(connection, Protocol.error("Name must be ASCII"));
                return;
            }
            if (value == 0) {
                if (i == 0) {
                    respond(connection, Protocol.error("Name must not be empty"));
                    return;
                }
                String name = new String(connection.input.array(), 0, i, StandardCharsets.US_ASCII);
                connection.input.clear();
                connection.requested = true;
                request(connection, name);
                return;
            }
        }
        if (!connection.input.hasRemaining()) {
            respond(connection, Protocol.error("Name too long"));
        }
    }

    private void request(Connection connection, String name) {
        Entry entry = cache.get(name);
        if (entry != null && entry.response != null) {
            respond(connection, entry.response);
            return;
        }
        boolean fresh = entry == null;
        if (fresh) {
            entry = new Entry();
            cache.put(name, entry);
        }
        entry.waiting.add(connection);
        connection.pending = entry;
        if (!fresh) {
            return;
        }
        Entry submitted = entry;
        workers.execute(() -> {
            generateKeys(name, submitted);
        });
    }

    private void generateKeys(String name, Entry entry) {
        byte[] response;
        boolean failed = false;
        try {
            response = generator.generate(name);
        } catch (Exception e) {
            System.err.println("Generation failed: " + e);
            response = Protocol.error("Key generation failed; retry the request");
            failed = true;
        }
        completed.add(new Completion(name, entry, response, failed));
        selector.wakeup();
    }

    private void drainCompletions() {
        Completion completion = completed.poll();
        while (completion != null) {
            Entry entry = completion.entry();
            if (completion.failed()) {
                cache.remove(completion.name(), entry);
            } else {
                entry.response = completion.response();
            }
            for (Connection connection : entry.waiting) {
                connection.pending = null;
                if (connection.key.isValid()) {
                    respond(connection, completion.response());
                }
            }
            entry.waiting.clear();
            completion = completed.poll();
        }
    }

    private void respond(Connection connection, byte[] response) {
        connection.output = ByteBuffer.wrap(response).asReadOnlyBuffer();
        connection.key.interestOps(SelectionKey.OP_WRITE);
    }

    private void write(Connection connection) throws IOException {
        ((SocketChannel) connection.key.channel()).write(connection.output);
        if (!connection.output.hasRemaining()) {
            disconnect(connection);
        }
    }

    private void disconnect(Connection connection) {
        if (connection.pending != null) {
            connection.pending.waiting.remove(connection);
            connection.pending = null;
        }
        connection.key.cancel();
        try {
            connection.key.channel().close();
        } catch (IOException ignored) {
        }
    }

    void stop() {
        running = false;
        selector.wakeup();
    }

    @Override public void close() throws IOException {
        stop();
        workers.shutdownNow();
        if (selector.isOpen()) {
            for (SelectionKey key : Set.copyOf(selector.keys())) {
                try {
                    key.channel().close();
                } catch (IOException ignored) {
                }
            }
            selector.close();
        }
    }
}
