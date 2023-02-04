package com.yandex.maps.testapp.mrc.camera;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @param <T> stored variable type
 */
public class ConcurrentVar<T> {
    private T value;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public ConcurrentVar() {
        this.value = null;
    }

    public ConcurrentVar(T value) {
        this.value = value;
    }

    /**
     * Sets the stored value to a given {@code newValue}
     */
    public void set(T newValue) {
        lock.lock();
        try {
            value = newValue;
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Waits for the value to become available,
     * returns it and resets the stored value to {@code null}.
     */
    public T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (value == null)
                condition.await();
            return readAndClear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * If the value is available returns it and resets the stored value to {@code null}.
     * Otherwise returns {@code null}
     */
    public T tryTake() {
        lock.lock();
        try {
            return readAndClear();
        } finally {
            lock.unlock();
        }
    }


    // Only call under lock
    private T readAndClear() {
        T oldValue = value;
        value = null;
        return oldValue;
    }
}
