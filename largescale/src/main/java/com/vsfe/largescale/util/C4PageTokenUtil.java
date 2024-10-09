package com.vsfe.largescale.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class C4PageTokenUtil {
    public static final String PAGE_TOKEN_FORMAT = "{}|{}"; // 첫번째 토큰 | 두번째 토큰


    public static <T, R> String encodePageToken(Pair<T, R> data) {
        return Base64.encodeBase64URLSafeString(
                C4StringUtil.format(PAGE_TOKEN_FORMAT, valueToString(data.getLeft()), valueToString(data.getRight()))
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    private static <T> String valueToString(T value) {
        if (value instanceof Instant instant) {
            return String.valueOf(instant.toEpochMilli()); // 시간인 경우 long 타입 시간으로 변환하겠다는 의미
        }

        return value.toString();
    }

    public static <T, R> Pair<T, R> decodePageToken(String pageToken, Class<T> firstType, Class<R> secondType) { // Instant와 int 타입을 받음
        var decoded = new String(Base64.decodeBase64(pageToken), StandardCharsets.UTF_8);
        var parts = decoded.split("\\|", 2); // 왼 오 추출
        Assert.isTrue(parts.length == 2, "invalid pageToken");
        return Pair.of(stringToValue(parts[0], firstType), stringToValue(parts[1], secondType));
    }

    @SuppressWarnings("unchecked")
    private static <T> T stringToValue(String data, Class<T> clazz) {
        if (clazz == String.class) {
            return (T)data;
        } else if (clazz == Integer.class) {
            return (T)Integer.valueOf(data);
        } else if (clazz == Long.class) {
            return (T)Long.valueOf(data);
        } else if (clazz == Boolean.class) {
            return (T)Boolean.valueOf(data);
        } else if (clazz == Instant.class) {
            return (T)Instant.ofEpochMilli(Long.parseLong(data));
        }

        throw new IllegalArgumentException(C4StringUtil.format("unsupported type - type:{}", clazz));
    }
}
