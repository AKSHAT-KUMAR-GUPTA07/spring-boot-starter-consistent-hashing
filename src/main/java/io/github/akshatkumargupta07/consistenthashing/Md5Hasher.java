package io.github.akshatkumargupta07.consistenthashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Hasher implements Hasher{
    @Override
    public long hash(String input) {
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            long h=0;
            for(int i=0 ; i<8; i++){
                h = (h << 8) | (digest[i] & 0xFF );
            }
            return h;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}
