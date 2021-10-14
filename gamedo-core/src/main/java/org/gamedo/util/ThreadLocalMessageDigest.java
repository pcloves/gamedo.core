package org.gamedo.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 线程安全的信息摘要工具类
 */
public final class ThreadLocalMessageDigest {

    public static final ThreadLocal<MessageDigest> MD5_DIGEST = createThreadLocalMessageDigest("MD5");

    private ThreadLocalMessageDigest() {
    }

    public static ThreadLocal<MessageDigest> createThreadLocalMessageDigest(String digest) {
        return ThreadLocal.withInitial(() -> {
            try {
                return MessageDigest.getInstance(digest);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("unexpected exception creating MessageDigest instance for [" + digest + ']', e);
            }
        });
    }
}
