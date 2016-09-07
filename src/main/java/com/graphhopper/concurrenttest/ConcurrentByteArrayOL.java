package com.graphhopper.concurrenttest;

/**
 *
 * @author Peter Karich
 */
public class ConcurrentByteArrayOL implements ConcurrentByteArray {

    private final byte[] bytes;
    private final int rowSize;
    // use row-wise locks later on to replace synchronized
    private final Object[] locks;
    private final int chunkRows = 128;

    public ConcurrentByteArrayOL(int size, int rowSize) {
        this.bytes = new byte[size];

        // create a lock for a block of X rows
        this.locks = new Object[size / rowSize / chunkRows + 1];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
        this.rowSize = rowSize;
    }

    @Override
    public void read(int pointer, byte[] tmpBytes, int length) {
        Object lock = locks[pointer / rowSize / chunkRows];
        synchronized (lock) {
            System.arraycopy(bytes, pointer, tmpBytes, 0, length);
        }
    }

    @Override
    public void write(int pointer, byte[] tmpBytes) {
        Object lock = locks[pointer / rowSize / chunkRows];
        synchronized (lock) {
            System.arraycopy(tmpBytes, 0, bytes, pointer, tmpBytes.length);
        }
    }
}
