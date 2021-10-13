package org.gamedo.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * hash算法，参考：<a href=https://github.com/bootsrc/flycache/blob/632bbd824f/flycache-core/src/main/java/io/github/flylib/flycache/hash/HashAlgorithm.java>flycache</a>
 */
public enum Hashing {

    NATIVE_HASH,
    KETAMA_HASH;

    public long hash(String key) {

        long hash = 0;
        switch (this) {
            case NATIVE_HASH: {
                hash = key.hashCode();
            }
            break;
            case KETAMA_HASH: {
                byte[] bKey = computeMd5(key);
                hash = (long) (bKey[3] & 0xFF) << 24 | (long) (bKey[2] & 0xFF) << 16
                        | (long) (bKey[1] & 0xFF) << 8 | bKey[0] & 0xFF;
            }
            break;
        }

        return hash;
    }

    public static byte[] computeMd5(String k) {
        MessageDigest md5;
        md5 = ThreadLocalMessageDigest.MD5_DIGEST.get();
        md5.reset();
        md5.update(k.getBytes(Charset.defaultCharset()));
        return md5.digest();
    }
}
