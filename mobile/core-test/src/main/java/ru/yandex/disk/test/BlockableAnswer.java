package ru.yandex.disk.test;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ru.yandex.disk.util.Exceptions;

import java.util.concurrent.atomic.AtomicBoolean;

public class BlockableAnswer<T> implements Answer<T> {
    private static final int TIMEOUT = Barrier.TIMEOUT_IN_MILLIS;

    private final Answer<T> backing;
    private final Barrier barrier;
    private final AtomicBoolean called;
    private Thread answeringThread;

    public BlockableAnswer(Answer<T> backing) {
        this.backing = backing;
        barrier = new Barrier();
        called = new AtomicBoolean();
    }

    public void answerInThread(Runnable runnable) {
        answeringThread = new Thread(runnable);
        answeringThread.start();
        waitCall();
    }

    @Override
    public T answer(InvocationOnMock invocation) throws Throwable {
        markCalled();
        barrier.block();
        return backing != null ? backing.answer(invocation) : null;
    }

    private void markCalled() {
        synchronized (called) {
            called.set(true);
            called.notify();
        }
    }

    public void unblock() throws InterruptedException {
        barrier.unblock();
        called.set(false);
        try {
            answeringThread.join(TIMEOUT);
        } catch (InterruptedException e) {
            Exceptions.crash(e);
        }
    }

    public void waitCall() {
        while (!called.get()) {
            synchronized (called) {
                try {
                    called.wait(TIMEOUT);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
