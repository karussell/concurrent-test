package com.graphhopper.concurrenttest;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Non-blocking and low memory variant. Seems to work perfectly.
 *
 * See http://stackoverflow.com/q/39675003/194609
 *
 * @author Peter Karich
 */
public class ConcurrentByteArrayCHM implements ConcurrentByteArray {

    private final byte[] bytes;
    // 50 should be enough elements / concurrency
    private final ConcurrentHashMap<Integer, Integer> locks = new ConcurrentHashMap<>(50);
    private final int rowSize;
    private final int chunkRows = 32;

    public ConcurrentByteArrayCHM(int size, int rowSize) {
        this.bytes = new byte[size];
        this.rowSize = rowSize;
    }

    @Override
    public void read(int pointer, byte[] tmpBytes, int length) {
        locks.compute(pointer / rowSize / chunkRows, (k, v) -> {
            System.arraycopy(bytes, pointer, tmpBytes, 0, length);
            return null;
        });
    }

    @Override
    public void write(int pointer, byte[] tmpBytes) {
        locks.compute(pointer / rowSize / chunkRows, (k, v) -> {
            System.arraycopy(tmpBytes, 0, bytes, pointer, tmpBytes.length);
            return null;
        });
    }
}
