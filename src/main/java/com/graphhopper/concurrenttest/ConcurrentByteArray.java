package com.graphhopper.concurrenttest;

/**
 *
 * @author Peter Karich
 */
public interface ConcurrentByteArray {

    void read(int pointer, byte[] tmpBytes, int length);

    void write(int pointer, byte[] tmpBytes);
}
