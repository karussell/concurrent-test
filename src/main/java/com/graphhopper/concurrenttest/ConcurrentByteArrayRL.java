package com.graphhopper.concurrenttest;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Peter Karich
 */
public class ConcurrentByteArrayRL implements ConcurrentByteArray {

    private final byte[] bytes;
    private final int rowSize;
    // use row-wise locks to replace synchronized
    private final Lock[] locks;
    private final int chunkRows = 128;

    public ConcurrentByteArrayRL(int size, int rowSize) {
        this.bytes = new byte[size];

        // create a lock for a block of X rows
        this.locks = new ReentrantLock[size / rowSize / chunkRows + 1];
        for (int i = 0; i < locks.length; i++) {
            // roughly 10% slower
            // locks[i] = new ReentrantReadWriteLock(true);
            locks[i] = new ReentrantLock();
        }
        this.rowSize = rowSize;
    }

    @Override
    public void read(int pointer, byte[] tmpBytes, int length) {
        Lock lock = locks[pointer / rowSize / chunkRows];//.readLock();
        lock.lock();
        try {
            System.arraycopy(bytes, pointer, tmpBytes, 0, length);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void write(int pointer, byte[] tmpBytes) {
        Lock lock = locks[pointer / rowSize / chunkRows];//.writeLock();
        lock.lock();
        try {
            System.arraycopy(tmpBytes, 0, bytes, pointer, tmpBytes.length);
        } finally {
            lock.unlock();
        }
    }
}
