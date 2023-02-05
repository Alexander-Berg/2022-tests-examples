package com.yandex.mail.util;

import com.yandex.mail.runners.UnitTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;

import static com.yandex.mail.util.RxUtils.blockingWithFallbackToAsync;
import static com.yandex.mail.util.RxUtils.fallbackToDefaultOnTimeoutOrError;
import static io.reactivex.schedulers.Schedulers.trampoline;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(UnitTestRunner.class)
public class RxUtilsTest {

    @Test
    public void blockingWithFallbackToAsync_returnsCachedItemImmediately() throws Exception {
        Object o1 = mock(Object.class);
        Object o2 = mock(Object.class);
        final Observable<Object> observable = Observable.just(o1, o2);
        final Scheduler mockScheduler = mock(Scheduler.class);

        final Observable<Object> resultObservable = observable.compose(blockingWithFallbackToAsync(1, SECONDS, mockScheduler));
        final TestObserver<Object> testObserver = new TestObserver<>();
        resultObservable.subscribe(testObserver);

        testObserver.assertValue(o1);
        verifyNoInteractions(mockScheduler);
    }

    @Test
    public void blockingWithFallbackToAsync_propagatesErrorIfBlocking() throws Exception {
        final RuntimeException expectedException = new RuntimeException("whatever");
        final Observable<Object> observable = Observable.error(expectedException);
        final Scheduler mockScheduler = mock(Scheduler.class);

        final Observable<Object> resultObservable = observable.compose(blockingWithFallbackToAsync(1, SECONDS, mockScheduler));
        final TestObserver<Object> testObserver = new TestObserver<>();
        resultObservable.subscribe(testObserver);

        testObserver.assertNoValues();
        testObserver.assertError(expectedException);
        verifyNoInteractions(mockScheduler);
    }

    @Test
    public void blockingWithFallbackToAsync_returnsAsynchronouslyOnTimeout() throws Exception {
        Object o1 = mock(Object.class);
        Object o2 = mock(Object.class);
        final TestScheduler delayScheduler = new TestScheduler();
        final Observable<Object> observable = Observable.just(o1, o2).delay(2, SECONDS, delayScheduler);

        final Observable<Object> resultObservable = observable.compose(blockingWithFallbackToAsync(1, SECONDS, trampoline()));
        final TestObserver<Object> testObserver = new TestObserver<>();
        resultObservable.subscribe(testObserver);

        // timer is paused, no values are available at this point
        testObserver.assertNoValues();
        delayScheduler.advanceTimeBy(5, SECONDS);
        // 5 seconds > 2 seconds, the values from observable should be emitted at this point
        testObserver.assertValues(o1, o2);
    }

    @Test
    public void blockingWithFallbackToAsync_propagatesErrorIfAsynchronous() throws Exception {
        final RuntimeException expectedException = new RuntimeException("whatever");
        final TestScheduler delayScheduler = new TestScheduler();
        final Observable<Object> observable = Observable.error(expectedException).delay(2, SECONDS, delayScheduler);

        final Observable<Object> resultObservable = observable.compose(blockingWithFallbackToAsync(1, SECONDS, trampoline()));
        final TestObserver<Object> testObserver = new TestObserver<>();
        resultObservable.subscribe(testObserver);

        // timer is paused, no values are available at this point
        testObserver.assertNoValues();
        delayScheduler.advanceTimeBy(5, SECONDS);
        // 5 seconds > 2 seconds, the exception from observable should be emitted at this point
        testObserver.assertError(expectedException);
    }

    @Test
    public void fallBackToDefaultOnTimeoutOrError_returnsOriginalOnNoTimeout() {
        Object object = mock(Object.class);
        final TestScheduler emissionDelayScheduler = new TestScheduler();
        final Single<Object> singleAfter100Ms = Single.just(object).delay(100, MILLISECONDS, emissionDelayScheduler);

        final TestScheduler timerScheduler = new TestScheduler();
        final Single<Object> result = singleAfter100Ms.compose(fallbackToDefaultOnTimeoutOrError(200, MILLISECONDS, timerScheduler, new Object()));

        final TestObserver<Object> subscriber = new TestObserver<>();
        result.subscribe(subscriber);

        emissionDelayScheduler.advanceTimeBy(150, MILLISECONDS);
        timerScheduler.advanceTimeBy(300, MILLISECONDS);
        subscriber.assertValue(object);
    }

    @Test
    public void fallBackToDefaultOnTimeoutOrError_returnsFallbackOnTimeout() {
        Object object = mock(Object.class);
        Object fallback = mock(Object.class);
        final TestScheduler emissionDelayScheduler = new TestScheduler();
        final Single<Object> singleAfter100Ms = Single.just(object).delay(500, MILLISECONDS, emissionDelayScheduler);

        final TestScheduler timerScheduler = new TestScheduler();
        final Single<Object> result = singleAfter100Ms.compose(fallbackToDefaultOnTimeoutOrError(200, MILLISECONDS, timerScheduler, fallback));

        final TestObserver<Object> subscriber = new TestObserver<>();
        result.subscribe(subscriber);

        emissionDelayScheduler.advanceTimeBy(150, MILLISECONDS);
        timerScheduler.advanceTimeBy(300, MILLISECONDS);
        subscriber.assertValue(fallback);
    }

    @Test
    public void fallBackToDefaultOnTimeoutOrError_returnsFallbackOnError() {
        Object fallback = mock(Object.class);
        final TestScheduler emissionDelayScheduler = new TestScheduler();
        final Single<Object> singleAfter100Ms = Single.error(new Throwable()).delay(100, MILLISECONDS, emissionDelayScheduler);

        final TestScheduler timerScheduler = new TestScheduler();
        final Single<Object> result = singleAfter100Ms.compose(fallbackToDefaultOnTimeoutOrError(200, MILLISECONDS, timerScheduler, fallback));

        final TestObserver<Object> subscriber = new TestObserver<>();
        result.subscribe(subscriber);

        emissionDelayScheduler.advanceTimeBy(150, MILLISECONDS);
        timerScheduler.advanceTimeBy(300, MILLISECONDS);
        subscriber.assertValue(fallback);
    }
}
