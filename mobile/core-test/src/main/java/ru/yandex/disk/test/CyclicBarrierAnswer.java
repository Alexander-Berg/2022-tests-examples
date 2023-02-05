package ru.yandex.disk.test;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ru.yandex.disk.util.Exceptions;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public class CyclicBarrierAnswer<T> implements Answer<T> {

    private final CyclicBarrier waitAsker;
    private final CyclicBarrier releaseBarrier;
    private boolean askerWaited;

    private final T returnValue;

    public CyclicBarrierAnswer() {
        this(null);
    }

    public CyclicBarrierAnswer(T returnValue) {
        this.returnValue = returnValue;
        waitAsker = new CyclicBarrier(2);
        releaseBarrier = new CyclicBarrier(2);
    }

    @Override
    public T answer(InvocationOnMock invocation) throws Throwable {
        await(waitAsker);
        await(releaseBarrier);
        return returnValue;
    }

    public void waitAsker() {
        askerWaited = true;
        await(waitAsker);
    }

    public void releaseAsker() {
        if (!askerWaited) {
            await(waitAsker);
        }
        await(releaseBarrier);
    }

    private void await(CyclicBarrier barrier) {
        try {
            barrier.await(Barrier.TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Exceptions.crash(e);
        }
    }


}
