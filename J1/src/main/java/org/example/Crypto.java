package org.example;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

final class Crypto {
    private final PrivateKey signingKey;
    private final X500Name issuer;

    Crypto(Path keyFile, String issuer) throws Exception {
        this.issuer = new X500Name(issuer);
        try (PemReader reader = new PemReader(Files.newBufferedReader(keyFile, StandardCharsets.US_ASCII))) {
            PemObject pem = reader.readPemObject();
            if (pem == null || !pem.getType().equals("PRIVATE KEY")) {
                throw new IllegalArgumentException("CA key must be an unencrypted PKCS#8 PEM PRIVATE KEY");
            }
            signingKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pem.getContent()));
        }
    }

    static void createSigningKey(Path path) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);
        Files.write(path, pem("PRIVATE KEY", generator.generateKeyPair().getPrivate().getEncoded()),
                StandardOpenOption.CREATE_NEW);
    }

    byte[] generate(String name) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(8192);
        KeyPair pair = generator.generateKeyPair();
        X500Name subject = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, name).build();
        Instant now = Instant.now();
        BigInteger serial = new BigInteger(159, new SecureRandom()).add(BigInteger.ONE);
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuer, serial,
                Date.from(now.minus(5, ChronoUnit.MINUTES)), Date.from(now.plus(365, ChronoUnit.DAYS)),
                subject, pair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        X509CertificateHolder certificate = builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(signingKey));
        return Protocol.success(pem("PRIVATE KEY", pair.getPrivate().getEncoded()),
                pem("CERTIFICATE", certificate.getEncoded()));
    }

    private static byte[] pem(String type, byte[] bytes) throws IOException {
        StringWriter text = new StringWriter();
        try (PemWriter writer = new PemWriter(text)) {
            writer.writeObject(new PemObject(type, bytes));
        }
        return text.toString().getBytes(StandardCharsets.US_ASCII);
    }
}
