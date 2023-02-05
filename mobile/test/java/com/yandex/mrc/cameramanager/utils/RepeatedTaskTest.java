package com.yandex.mrc.cameramanager.utils;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class RepeatedTaskTest {
    private int counter = 0;
    private Runnable task = new Runnable() {
        @Override
        public void run() {
            counter++;
        }
    };

    @Test
    public void testRepeatedTask() throws Exception {

        RepeatedTask repeatedTask = new RepeatedTask(task);
        repeatedTask.run(50, 100);

        Thread.sleep(30);
        assertEquals(counter, 0);

        Thread.sleep(30);
        assertEquals(counter, 1);

        Thread.sleep(400);
        assertEquals(counter, 5);

        repeatedTask.cancel();
        counter = 0;
        repeatedTask.run(0, 50);
        Thread.sleep(120);
        assertEquals(counter, 3);
    }


    @Test(expected = IllegalStateException.class)
    public void testRepeatedTaskException() throws Exception {

        RepeatedTask repeatedTask = new RepeatedTask(task);
        repeatedTask.run(50, 100);
        Thread.sleep(100);
        repeatedTask.run(0, 100);
    }

    @Test
    public void testRepeatedTaskLastRunTime() throws Exception {

        RepeatedTask repeatedTask = new RepeatedTask(task);
        repeatedTask.run(50, 100);

        Thread.sleep(30);
        assertEquals(repeatedTask.lastRunTimeMillis(), 0);

        Thread.sleep(30);
        long passedMillis = System.currentTimeMillis() - repeatedTask.lastRunTimeMillis();
        assertTrue(passedMillis > 0 && passedMillis < 60);
    }
}
