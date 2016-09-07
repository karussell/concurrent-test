package com.graphhopper.concurrenttest;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author Peter Karich
 */
public interface ConcurrentByteArray {

    void read(int pointer, byte[] tmpBytes, int length);

    void write(int pointer, byte[] tmpBytes);
}
