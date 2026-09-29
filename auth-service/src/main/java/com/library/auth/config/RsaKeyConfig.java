package com.library.auth.config;

import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Загружает ключевую пару для подписи токенов. */
@Configuration
public class RsaKeyConfig {

    private static final String KEY_ID = "library-dev-1";

    @Bean
    public RSAKey jwtSigningKey(@Value("${auth.keys.private-key:classpath:keys/jwt-private.pem}") Resource privateKey,
                                @Value("${auth.keys.public-key:classpath:keys/jwt-public.pem}") Resource publicKey)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        RSAPrivateKey privateRsa = readPrivateKey(privateKey);
        RSAPublicKey publicRsa = readPublicKey(publicKey);
        return new RSAKey.Builder(publicRsa)
                .privateKey(privateRsa)
                .keyID(KEY_ID)
                .build();
    }

    private RSAPrivateKey readPrivateKey(Resource resource)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] der = decodePem(resource, "PRIVATE KEY");
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private RSAPublicKey readPublicKey(Resource resource)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] der = decodePem(resource, "PUBLIC KEY");
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    private byte[] decodePem(Resource resource, String type) throws IOException {
        String pem = resource.getContentAsString(StandardCharsets.UTF_8)
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(pem);
    }
}
