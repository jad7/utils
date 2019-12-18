package com.github.jad.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class StringLocker {

    private ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    public StringLocker.Unlock lock(String stlLock) {
        final ReentrantLock lock = lockMap.computeIfAbsent(stlLock, k -> new ReentrantLock());
        lock.lock();
        return new Unlock(lock, stlLock);
    }

    public class Unlock {

        private final ReentrantLock lock;
        private final String stlLock;

        public Unlock(ReentrantLock lock, String stlLock) {
            this.lock = lock;
            this.stlLock = stlLock;
        }

        public void unlock() {
            lockMap.compute(stlLock, (k, v) -> {
                if (v == null && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    return null;
                }
                if (v != lock && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    return v;
                }
                if (v == lock && lock.isHeldByCurrentThread() && lock.hasQueuedThreads()) {
                    lock.unlock();
                    return lock;
                }
                if (v == lock && lock.isHeldByCurrentThread() && !lock.hasQueuedThreads()) {
                    lock.unlock();
                    return null;
                }
                return v;
            });
        }
    }


}
