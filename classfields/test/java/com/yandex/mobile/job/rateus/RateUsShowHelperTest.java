package com.yandex.mobile.job.rateus;

import android.content.Context;
import android.content.SharedPreferences;

import com.yandex.mobile.job.JMockitRobolectricTestRunner;
import com.yandex.mobile.job.utils.DateUtils;
import com.yandex.mobile.job.utils.RateUsShowHelper_;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.concurrent.TimeUnit;

import mockit.Deencapsulation;
import mockit.NonStrictExpectations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexey Agapitov <agapitov@yandex-team.ru> on 08.04.2015
 */
@Config(manifest = Config.NONE)
@RunWith(JMockitRobolectricTestRunner.class)
public class RateUsShowHelperTest {
    private static final String RateUsPref = "RateUsPref",
        firstStartTimestamp = "firstStartTimestamp",
        startCounter = "startCounter",
        lastShownTimestamp = "lastShownTimestamp",
        showCounter = "showCounter",
        doNotShow = "doNotShow";

    private Context appContext;
    private SharedPreferences prefs;
    private RateUsShowHelper_ testTarget;
    private long mockedTime;

    @Before
    public void setUp() {
        appContext = Robolectric.getShadowApplication().getApplicationContext();
        prefs = appContext.getSharedPreferences(RateUsPref, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        testTarget = RateUsShowHelper_.getInstance_(appContext);
        mockedTime = 0;
    }

    private long performMinimalStartsNumber() {
        testTarget.appStarted();
        long firstStartTimestamp = System.currentTimeMillis();
        testTarget.appStarted();
        testTarget.appStarted();
        return firstStartTimestamp;
    }

    private void prepareFirstShow() {
        long firstStartTimestamp = performMinimalStartsNumber();
        //minimal timestamp after which dialog can be shown
        long firstShowTimestamp = firstStartTimestamp + TimeUnit.MILLISECONDS.convert(3L, TimeUnit.DAYS);
        mockCurrentTimeWith(firstShowTimestamp);
    }

    private long performFirstShow() {
        prepareFirstShow();
        testTarget.setWasShown();
        return mockedTime();
    }

    private void prepareSecondShow() {
        long firstShowTimestamp = performFirstShow();
        long secondShowTimestamp = firstShowTimestamp + TimeUnit.MILLISECONDS.convert(7L, TimeUnit.DAYS);
        mockCurrentTimeWith(secondShowTimestamp);
    }

    private long performSecondShow() {
        prepareSecondShow();
        testTarget.setWasShown();
        return mockedTime();
    }

    private void mockCurrentTimeWith(final long returnValue) {
        new NonStrictExpectations(DateUtils.class) {{
            Deencapsulation.invoke(DateUtils.class, "currentTime");
            result = returnValue;
        }};
        mockedTime = returnValue;
    }

    private long mockedTime() {
        return mockedTime;
    }

    @Test
    public void shouldSetFirstStartTimestamp() {
        assertFalse(prefs.contains(firstStartTimestamp));

        long startTime = System.currentTimeMillis();
        testTarget.appStarted();
        /*see shouldSetLastShowTimestamp why greater or equals*/
        assertTrue(prefs.getLong(firstStartTimestamp, 0L) >= startTime);
    }

    @Test
    public void shouldNotOverwriteFirstStartTimestamp() {
        assertFalse(prefs.contains(firstStartTimestamp));

        testTarget.appStarted();
        long firstStartTimestampVal = prefs.getLong(firstStartTimestamp, 0L);
        testTarget.appStarted();
        assertEquals(firstStartTimestampVal, prefs.getLong(firstStartTimestamp, 0L));
    }

    @Test
    public void shouldIncrementAppStartCounter() {
        assertFalse(prefs.contains(startCounter));

        testTarget.appStarted();
        assertEquals(1L, prefs.getLong(startCounter, 0L));

        testTarget.appStarted();
        assertEquals(2L, prefs.getLong(startCounter, 0L));
    }

    @Test
    public void shouldSetLastShowTimestamp() {
        assertFalse(prefs.contains(lastShownTimestamp));
        long now = System.currentTimeMillis();
        testTarget.setWasShown();
        /*for some reason sometimes two successive System.currentTimeMillis calls return same results,
        so greater or EQUAL is used for comparison*/
        assertTrue(prefs.getLong(lastShownTimestamp, 0L) >= now);
    }

    @Test
    public void shouldIncrementShowCounter() {
        assertFalse(prefs.contains(showCounter));

        testTarget.setWasShown();
        assertEquals(1, prefs.getInt(showCounter, 0));

        testTarget.setWasShown();
        assertEquals(2, prefs.getInt(showCounter, 0));
    }

    @Test
    public void shouldNotRemindAfterFirstShow() {
        assertFalse(prefs.contains(showCounter));

        assertTrue(testTarget.canRemindLater());

        performFirstShow();

        assertFalse(testTarget.canRemindLater());
    }

    @Test
    public void shouldNotShowImmediatelyAfterUpdateOfOldUsers() {
        assertTrue(prefs.getAll().isEmpty());
        testTarget.appStarted();
        assertFalse(testTarget.shouldBeShown());
    }

    @Test
    public void shouldShowFirstTimeAfter3DaysAnd3Starts() {
        assertTrue(prefs.getAll().isEmpty());
        long now = System.currentTimeMillis();//calling before prepareFirstShow
        prepareFirstShow();

        assertEquals(3L, prefs.getLong(startCounter, 0L));

        long after3Days = now + TimeUnit.MILLISECONDS.convert(3L, TimeUnit.DAYS);
        assertTrue(mockedTime() >= after3Days);

        assertTrue(testTarget.shouldBeShown());
    }

    @Test
    public void shouldShowSecondTimeAfterWeek() {
        assertTrue(prefs.getAll().isEmpty());
        long now = System.currentTimeMillis();//calling before prepareSecondShow
        prepareSecondShow();

        assertEquals(1L, prefs.getInt(showCounter, 0));

        long after7Days = now + TimeUnit.MILLISECONDS.convert(7L, TimeUnit.DAYS);
        assertTrue(mockedTime() >= after7Days);

        assertTrue(testTarget.shouldBeShown());
    }

    @Test
    public void shouldNotShowAfterSecondShow() {
        assertTrue(prefs.getAll().isEmpty());
        long secondShowTimestamp = performSecondShow();
        assertEquals(2L, prefs.getInt(showCounter, 0));

        long thirdShowTimeStampAfterWeek = secondShowTimestamp + TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS);
        mockCurrentTimeWith(thirdShowTimeStampAfterWeek);
        assertFalse(testTarget.shouldBeShown());
    }

    @Test
    public void shouldSetNotShowAfterRatingSubmitted() {
        assertFalse(prefs.contains(doNotShow));

        testTarget.setRatingSubmitted();
        assertTrue(prefs.getBoolean(doNotShow, false));
    }

    @Test
    public void shouldNotShowAfterRatingSubmitted() {
        assertTrue(prefs.getAll().isEmpty());

        long firstShowTimestamp = performFirstShow();
        testTarget.setRatingSubmitted();

        long timePassed = firstShowTimestamp + TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS);
        mockCurrentTimeWith(timePassed);
        assertFalse(testTarget.shouldBeShown());
    }
}
