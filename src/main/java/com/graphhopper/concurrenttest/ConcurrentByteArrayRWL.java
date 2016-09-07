package com.graphhopper.concurrenttest;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author Peter Karich
 */
public class ConcurrentByteArrayRWL implements ConcurrentByteArray {

    private final byte[] bytes;
    private final int rowSize;
    // use row-wise locks to replace synchronized
    private final ReadWriteLock[] locks;
    private final int chunkRows = 128;

    public ConcurrentByteArrayRWL(int size, int rowSize) {
        this.bytes = new byte[size];

        // create a lock for a block of X rows
        this.locks = new ReadWriteLock[size / rowSize / chunkRows + 1];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantReadWriteLock(true);
        }
        this.rowSize = rowSize;
    }

    @Override
    public void read(int pointer, byte[] tmpBytes, int length) {
        Lock lock = locks[pointer / rowSize / chunkRows].readLock();
        lock.lock();
        try {
            System.arraycopy(bytes, pointer, tmpBytes, 0, length);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void write(int pointer, byte[] tmpBytes) {
        Lock lock = locks[pointer / rowSize / chunkRows].writeLock();
        lock.lock();
        try {
            System.arraycopy(tmpBytes, 0, bytes, pointer, tmpBytes.length);
        } finally {
            lock.unlock();
        }
    }
}
