package com.graphhopper.concurrenttest;

import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Peter Karich
 */
public class ConcurrentByteArrayTest {

    @Test
    public void testReadWrite() {
        int rowSize = 16;
        ConcurrentByteArray[] impls = new ConcurrentByteArray[]{new ConcurrentByteArrayOL(100, rowSize), new ConcurrentByteArrayRL(100, rowSize)};
        for (ConcurrentByteArray arr : impls) {
            byte[] bytes = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
            arr.write(0, bytes);

            byte[] bytes2 = new byte[8];
            arr.read(2, bytes2, bytes2.length);
            assertEquals(8, bytes2.length);
            assertEquals(Arrays.toString(new byte[]{2, 3, 4, 5, 6, 7, 8, 9}), Arrays.toString(bytes2));
        }
    }
}
