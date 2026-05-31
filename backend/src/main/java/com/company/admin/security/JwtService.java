package com.company.admin.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes:120}") long expirationMinutes) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT secret must contain at least 32 bytes");
        }
        this.secret = secretBytes;
        this.expirationSeconds = expirationMinutes * 60;
    }

    public String generateToken(String username) {
        try {
            Instant now = Instant.now();
            String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = encodeJson(Map.of(
                    "sub", username,
                    "iat", now.getEpochSecond(),
                    "exp", now.plusSeconds(expirationSeconds).getEpochSecond()));
            String unsignedToken = header + "." + payload;

            // JWT 使用 HMAC-SHA256 对 header.payload 签名，避免 token 被客户端篡改。
            return unsignedToken + "." + sign(unsignedToken);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate JWT token", exception);
        }
    }

    public Optional<String> parseUsername(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }

            String unsignedToken = parts[0] + "." + parts[1];
            if (!signatureMatches(parts[2], sign(unsignedToken))) {
                return Optional.empty();
            }

            JsonNode payload = objectMapper.readTree(BASE64_URL_DECODER.decode(parts[1]));
            long expiresAt = payload.path("exp").asLong(0);
            if (expiresAt <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }

            String username = payload.path("sub").asText("");
            return username.isBlank() ? Optional.empty() : Optional.of(username);
        } catch (Exception exception) {
            // JWT 解析或过期校验失败统一视为无效 token，不向客户端暴露具体原因。
            return Optional.empty();
        }
    }

    private String encodeJson(Map<String, ?> values) throws Exception {
        byte[] jsonBytes = objectMapper.writeValueAsBytes(values);
        return BASE64_URL_ENCODER.encodeToString(jsonBytes);
    }

    private String sign(String unsignedToken) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
        return BASE64_URL_ENCODER.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean signatureMatches(String actualSignature, String expectedSignature) {
        return MessageDigest.isEqual(
                actualSignature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8));
    }
}
