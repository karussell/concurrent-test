/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.graphhopper.concurrenttest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.openjdk.jmh.annotations.Benchmark;

public class MyBenchmark {

    // DO NOT execute this method for performance comparison instead use the JMH method e.g. via 
    // java -jar target/benchmarks.jar -i 10 -wi 10 -f 1
    public static void main(String[] args) {
        new MyBenchmark().runThreads(true, new ConcurrentByteArrayRWL(MAX, ROW_SIZE));
    }

    static final int ROW_SIZE = 16;
    static final int MAX = 16 * 512 * 1024 * ROW_SIZE;

    @Benchmark
    public void testReadWriteLock() {
        runThreads(false, new ConcurrentByteArrayRWL(MAX, ROW_SIZE));
    }

    @Benchmark
    public void testObjectLock() {
        runThreads(false, new ConcurrentByteArrayOL(MAX, ROW_SIZE));
    }

    public void runThreads(final boolean log, final ConcurrentByteArray array) {
        try {
            final int expectedSum = ROW_SIZE * (ROW_SIZE - 1) / 2;
            int cores = 2;
            int readingThreads = cores * 2;
            final Random rand = new Random(0);

            // simulate one sequential write
            Thread writeThread = new Thread() {
                @Override
                public void run() {

                    byte[] bytes1 = new byte[ROW_SIZE];
                    for (int i = 0; i < ROW_SIZE; i++) {
                        bytes1[i] = (byte) i;
                    }
                    byte[] bytes2 = new byte[ROW_SIZE];
                    for (int i = 0; i < ROW_SIZE; i++) {
                        bytes2[i] = (byte) (ROW_SIZE - i - 1);
                    }

                    if (log) {
                        System.out.println("start writer");
                    }

                    // operate a bit longer to really overlap with reading threads
                    for (int i = 0; i < 4; i++) {
                        for (int pointer = 0; pointer < MAX; pointer += ROW_SIZE) {
                            array.write(pointer, bytes1);
                        }
                        // now overwrite in decreasing order, the expected sum of one row is unchanged
                        for (int pointer = ROW_SIZE; pointer < MAX; pointer += 2 * ROW_SIZE) {
                            array.write(pointer, bytes2);
                        }
                    }

                    if (log) {
                        System.out.println("finished writer \t\t " + new Date());
                    }
                }
            };

            // simulate random reads            
            ExecutorService service = Executors.newFixedThreadPool(cores * 2);
            final AtomicInteger finished = new AtomicInteger(0);
            final List<String> errors = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < readingThreads; i++) {
                final int c = i;
                service.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (log) {
                                System.out.println("started reader " + c);
                            }
                            long sum = 0;
                            int tmpMax = MAX / ROW_SIZE;
                            byte[] tmpBytes = new byte[ROW_SIZE];
                            for (int i = 0; i < tmpMax; i++) {
                                int pointer = rand.nextInt(MAX / ROW_SIZE) * ROW_SIZE;
                                array.read(pointer, tmpBytes, ROW_SIZE);
                                int tmpSum = 0;
                                for (int j = 0; j < tmpBytes.length; j++) {
                                    tmpSum += tmpBytes[j];
                                }
                                // only 0 and expected sum is allowed, otherwise there are threading issues!
//                                if (tmpSum != expectedSum && tmpSum != 0) {
//                                    throw new IllegalStateException("Threading issue sum should be " + expectedSum + " or 0 but was " + tmpSum);
//                                }
                                sum += tmpSum;
                                // System.out.println(i + " reader" + c);
                            }
                            finished.addAndGet(1);
                            if (log) {
                                System.out.println("reader " + c + " ends with " + sum + " \t " + new Date());
                            }
                        } catch (Exception ex) {
                            errors.add(ex.getMessage());
                        }
                    }
                });
            }

            writeThread.start();
            writeThread.join();

            service.shutdown();
            service.awaitTermination(60, TimeUnit.SECONDS);

            if (finished.get() != readingThreads) {
                throw new RuntimeException("Still running read threads!?" + finished.get() + " vs " + readingThreads);
            }

            if (log) {
                System.out.println("ended with " + finished.get() + " finished readers \t " + new Date());
                System.out.println("errors: " + errors);
            } else if (!errors.isEmpty()) {
                throw new RuntimeException(errors.toString());
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
