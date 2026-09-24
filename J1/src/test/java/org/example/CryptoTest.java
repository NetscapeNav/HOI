package org.example;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.util.io.pem.PemReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
import java.security.interfaces.*;
import java.security.spec.*;
import static org.junit.jupiter.api.Assertions.*;

class CryptoTest {
    @Test @Timeout(300)
    void actual8192BitKeyAndCertificate(@TempDir Path temp) throws Exception {
        Path caPath = temp.resolve("ca.key");
        Crypto.createSigningKey(caPath);
        assertThrows(FileAlreadyExistsException.class, () -> Crypto.createSigningKey(caPath));
        String name = "alice,OU=not-an-injected-field";
        Crypto crypto = new Crypto(caPath, "CN=Test CA");
        Protocol.Keys result = Protocol.read(new ByteArrayInputStream(crypto.generate(name)));
        RSAPrivateCrtKey key = (RSAPrivateCrtKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(pem(result.privateKey())));
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(result.certificate()));
        RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();
        assertEquals(8192, key.getModulus().bitLength());
        assertEquals(key.getModulus(), publicKey.getModulus());
        assertEquals(key.getPublicExponent(), publicKey.getPublicExponent());
        certificate.checkValidity();
        assertEquals("CN=Test CA", certificate.getIssuerX500Principal().getName());
        X500Name subject = X500Name.getInstance(certificate.getSubjectX500Principal().getEncoded());
        assertEquals(1, subject.getRDNs().length);
        assertEquals(name, subject.getRDNs(BCStyle.CN)[0].getFirst().getValue().toString());
        RSAPrivateCrtKey caKey = (RSAPrivateCrtKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(pem(Files.readAllBytes(caPath))));
        PublicKey caPublic = KeyFactory.getInstance("RSA").generatePublic(
                new RSAPublicKeySpec(caKey.getModulus(), caKey.getPublicExponent()));
        certificate.verify(caPublic);
    }

    private byte[] pem(byte[] bytes) throws IOException {
        try (PemReader reader = new PemReader(new StringReader(new String(bytes, StandardCharsets.US_ASCII)))) {
            return reader.readPemObject().getContent();
        }
    }
}
