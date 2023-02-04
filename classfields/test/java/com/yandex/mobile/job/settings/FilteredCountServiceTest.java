package com.yandex.mobile.job.settings;

import com.yandex.mobile.job.JMockitRobolectricTestRunner;
import com.yandex.mobile.job.model.SystemPref_;
import com.yandex.mobile.job.service.FilterCountService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.concurrent.TimeUnit;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Tested;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

import static mockit.Deencapsulation.invoke;
import static mockit.Deencapsulation.setField;

/**
 * @author ironbcc on 15.04.2015.
 */
@Config(manifest = Config.NONE)
@RunWith(JMockitRobolectricTestRunner.class)
public class FilteredCountServiceTest {
    private SystemPref_ systemPref;
    @Tested @Mocked("observeServerFilteredCount") FilterCountService filterCountService;

    @Before
    public void before() {
        systemPref = new SystemPref_(Robolectric.getShadowApplication().getApplicationContext());
        systemPref.filteredJobsCount().put(0);
        new NonStrictExpectations(filterCountService) {{
            setField(filterCountService, "systemPref", systemPref);
            invoke(filterCountService, "observeServerFilteredCount");
            result = Observable.<Integer>just(123);
        }};
    }

    @Test
    public void directTest() throws Exception {
        systemPref.filteredJobsCount().put(0);
        Observable<Integer> observeCount = filterCountService.observeCount();
        Subscription subscribe = observeCount.subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                Assert.assertEquals(Integer.valueOf(0), integer);
            }
        });
        subscribe.unsubscribe();
        filterCountService.onFilterChanged();
        TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        observeCount.subscribe(subscriber);
        subscriber.awaitTerminalEvent(2, TimeUnit.SECONDS);
        observeCount.subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                Assert.assertEquals(Integer.valueOf(123), integer);
            }
        });
        Assert.assertEquals(123, (int) systemPref.filteredJobsCount().get());
    }

    @Test
    public void loadingTest() {
        Assert.assertFalse(filterCountService.isLoading());
        filterCountService.onFilterChanged();
        Assert.assertTrue(filterCountService.isLoading());
        TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        filterCountService.observeCount().subscribe(subscriber);
        subscriber.awaitTerminalEvent(2, TimeUnit.SECONDS);
        Assert.assertFalse(filterCountService.isLoading());
    }
}
