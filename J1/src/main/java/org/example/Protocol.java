package org.example;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

final class Protocol {
    static final int MAX_NAME = 1024;
    static final int MAX_FIELD = 1024 * 1024;
    record Keys(byte[] privateKey, byte[] certificate) {}

    static byte[] request(String name) {
        if (name.isEmpty() || name.length() > MAX_NAME) {
            throw new IllegalArgumentException("Name length must be 1.." + MAX_NAME);
        }
        for (char c : name.toCharArray()) {
            if (c == 0 || c > 127) {
                throw new IllegalArgumentException("Name must be ASCII without NUL");
            }
        }
        return (name + '\0').getBytes(StandardCharsets.US_ASCII);
    }
    static byte[] success(byte[] key, byte[] certificate) {
        return ByteBuffer.allocate(1 + 4 + key.length + 4 + certificate.length)
                .put((byte) 0).putInt(key.length).put(key)
                .putInt(certificate.length).put(certificate).array();
    }
    static byte[] error(String message) {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(1 + 4 + bytes.length).put((byte) 1).putInt(bytes.length).put(bytes).array();
    }
    static Keys read(InputStream input) throws IOException {
        DataInputStream in = new DataInputStream(input);
        int status = in.readUnsignedByte();
        if (status == 1) {
            throw new IOException("Server: " + new String(field(in), StandardCharsets.UTF_8));
        }
        if (status != 0) {
            throw new IOException("Unknown response status: " + status);
        }
        return new Keys(field(in), field(in));
    }
    private static byte[] field(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > MAX_FIELD) {
            throw new IOException("Invalid response length: " + length);
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return bytes;
    }
}
