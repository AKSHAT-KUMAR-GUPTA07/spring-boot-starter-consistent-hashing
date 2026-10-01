package io.github.akshatkumargupta07.consistenthashing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Hasher implements Hasher{

    // One MessageDigest PER THREAD, created once and reused — not a fresh one on every hash()
    // call. MessageDigest is stateful and not thread-safe, so it can't be a single shared
    // instance; ThreadLocal gives each thread its own private copy instead.
    // digest() resets the instance's internal state automatically after each call, so the same
    // object is safe to reuse immediately on the next call from that same thread.
    private static final ThreadLocal<MessageDigest> MD5 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    });

    @Override
    public long hash(String input) {
        MessageDigest md = MD5.get();
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        long h=0;
        for(int i=0 ; i<8; i++){
            h = (h << 8) | (digest[i] & 0xFF );
        }
        return h;
    }
}
