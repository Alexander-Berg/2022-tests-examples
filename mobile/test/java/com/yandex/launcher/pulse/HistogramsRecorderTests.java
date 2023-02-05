package com.yandex.launcher.pulse;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.os.Build;
import android.os.Looper;

import com.google.common.util.concurrent.MoreExecutors;
import com.yandex.launcher.common.app.threadpolicy.CommonThreadPolicyFacade;
import com.yandex.launcher.common.util.LaunchMode;
import com.yandex.launcher.common.util.Logger;
import com.yandex.launcher.common.util.ProcessUtils;
import com.yandex.launcher.common.util.ThreadUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = Build.VERSION_CODES.P)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@PrepareForTest({HistogramsRecorder.class, ThreadUtils.class, Logger.class, ProcessUtils.class})
public class HistogramsRecorderTests {

    private static final String RECORD_HISTOGRAM_METHOD = "recordHistogram";
    private static final long DEFAULT_DURATION = 0L;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private HistogramsRecorder recorder;
    private Histogram appLevelHistogram;
    private Histogram lnchrLevelFrameHistogram;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(ProcessUtils.class);
        PowerMockito.when(ProcessUtils.isMainProcess(any())).thenReturn(true);

        recorder = PowerMockito.spy(new HistogramsRecorder(RuntimeEnvironment.application));
        HistogramsRecorder.setExecutorService(MoreExecutors.newDirectExecutorService());
        HistogramsRecorder.setLaunchMode(LaunchMode.NORMAL);

        appLevelHistogram = Histogram.APPLICATION_BEFORE_ON_CREATE;
        lnchrLevelFrameHistogram = Histogram.LAUNCHER_ALLAPPS_CLOSE;

        PowerMockito.mockStatic(ThreadUtils.class);
        PowerMockito.mockStatic(Logger.class);
    }

    @After
    public void tearDown() {
        Histograms.Recorder.resetFirstRecordFlag();
        recorder.cancelAll();
        recorder.onTerminate();
        recorder = null;
    }

    @Test
    public void shouldRecordHistogram() throws Exception {
        recorder.create(appLevelHistogram);
        recorder.finish(appLevelHistogram);

        verify(recorder, times(1)).create(appLevelHistogram);
        verify(recorder, times(1)).finish(appLevelHistogram);

        PowerMockito.verifyPrivate(recorder, times(1))
                .invoke(RECORD_HISTOGRAM_METHOD, appLevelHistogram, DEFAULT_DURATION);
    }

    @Test
    public void shouldNotRecordWithoutCreate() throws Exception {
        recorder.finish(appLevelHistogram);

        verify(recorder, times(1)).finish(appLevelHistogram);
        PowerMockito.verifyPrivate(recorder, never())
                .invoke(RECORD_HISTOGRAM_METHOD, appLevelHistogram, DEFAULT_DURATION);
    }

    @Test
    public void shouldNotDoubleRecordSameHistogramTwice() throws Exception {
        recorder.create(appLevelHistogram);
        recorder.create(appLevelHistogram);
        recorder.finish(appLevelHistogram);
        recorder.finish(appLevelHistogram);

        verify(recorder, times(2)).create(appLevelHistogram);
        verify(recorder, times(2)).finish(appLevelHistogram);
        PowerMockito.verifyPrivate(recorder, times(1))
                .invoke(RECORD_HISTOGRAM_METHOD, appLevelHistogram, DEFAULT_DURATION);
    }

    @Test
    public void shouldDoubleRecordHistogram() throws Exception {
        final Histogram secondHistogram = Histogram.APPLICATION_ON_CREATE;
        recorder.create(appLevelHistogram);
        recorder.create(secondHistogram);
        recorder.finish(appLevelHistogram);
        recorder.finish(secondHistogram);

        verify(recorder, times(1)).create(appLevelHistogram);
        verify(recorder, times(1)).create(secondHistogram);
        verify(recorder, times(1)).finish(appLevelHistogram);
        verify(recorder, times(1)).finish(secondHistogram);

        PowerMockito.verifyPrivate(recorder, times(1))
                .invoke(RECORD_HISTOGRAM_METHOD, appLevelHistogram, DEFAULT_DURATION);
        PowerMockito.verifyPrivate(recorder, times(1))
                .invoke(RECORD_HISTOGRAM_METHOD, secondHistogram, DEFAULT_DURATION);
    }

    @Test
    public void shouldRecordFrameHistogram() throws Exception {
        recorder.create(lnchrLevelFrameHistogram);
        recorder.reportDuration(lnchrLevelFrameHistogram, 10);
        awaitExecutor();
        recorder.finish(lnchrLevelFrameHistogram);

        verify(recorder, times(1)).create(lnchrLevelFrameHistogram);
        verify(recorder, times(1)).finish(lnchrLevelFrameHistogram);

        PowerMockito.verifyPrivate(recorder, atLeastOnce())
                .invoke(RECORD_HISTOGRAM_METHOD, lnchrLevelFrameHistogram, 10L);
    }

    @Test
    public void shouldReportDuration() throws Exception {
        final long testDuration = 100;
        recorder.reportDuration(appLevelHistogram, testDuration);

        PowerMockito.verifyPrivate(recorder, times(1))
                .invoke(RECORD_HISTOGRAM_METHOD, appLevelHistogram, testDuration);
    }

    @Test
    public void shouldCancelAll() throws Exception {
        recorder.create(appLevelHistogram);
        recorder.create(lnchrLevelFrameHistogram);
        recorder.cancelAll();
        recorder.finish(appLevelHistogram);
        recorder.finish(lnchrLevelFrameHistogram);

        PowerMockito.verifyPrivate(recorder, never())
                .invoke(RECORD_HISTOGRAM_METHOD, appLevelHistogram, DEFAULT_DURATION);

        PowerMockito.verifyPrivate(recorder, never())
                .invoke(RECORD_HISTOGRAM_METHOD, lnchrLevelFrameHistogram, DEFAULT_DURATION);
    }

    @Test
    public void shouldCancelLevel() throws Exception {
        recorder.create(appLevelHistogram);
        recorder.create(lnchrLevelFrameHistogram);
        recorder.cancel(Histogram.Level.LAUNCHER);
        recorder.finish(appLevelHistogram);
        recorder.finish(lnchrLevelFrameHistogram);

        PowerMockito.verifyPrivate(recorder, atLeastOnce())
                .invoke(RECORD_HISTOGRAM_METHOD, eq(appLevelHistogram), anyLong());

        PowerMockito.verifyPrivate(recorder, never())
                .invoke(RECORD_HISTOGRAM_METHOD, eq(lnchrLevelFrameHistogram), anyLong());
    }

    @Test
    public void shouldCancel() throws Exception {
        recorder.create(appLevelHistogram);
        recorder.cancel(appLevelHistogram);
        recorder.finish(appLevelHistogram);

        PowerMockito.verifyPrivate(recorder, never())
                .invoke(RECORD_HISTOGRAM_METHOD, eq(appLevelHistogram), anyLong());
    }

    @Test()
    public void shouldNotReportNegativeDuration() throws Exception {
        final long finishTimestamp = 5;
        recorder.create(appLevelHistogram, 10L);
        recorder.finish(appLevelHistogram, finishTimestamp);

        verify(recorder, times(1)).finish(appLevelHistogram, finishTimestamp);

        PowerMockito.verifyPrivate(recorder, never())
                .invoke(RECORD_HISTOGRAM_METHOD, eq(appLevelHistogram), eq(-finishTimestamp));
        PowerMockito.verifyPrivate(recorder, never())
                .invoke(RECORD_HISTOGRAM_METHOD, eq(appLevelHistogram), anyLong());
    }

    @Test
    public void shouldChangeHistogramName() {
        final Histogram first = Histogram.LAUNCHER_ALLAPPS_OPENED_BY_SWIPE;
        final Histogram second = Histogram.LAUNCHER_ALLAPPS_OPENED_BY_CLICK;
        final String postfix = "First";

        assertThat(first.getHistogramName(), containsString(postfix));
        assertThat(second.getHistogramName(), containsString(postfix));
        recorder.create(first);
        recorder.reportDuration(Histogram.LAUNCHER_ALLAPPS_OPENED_BY_SWIPE, 10);
        recorder.reportDuration(Histogram.LAUNCHER_SEARCH_OPENED_BY_CLICK, 10);
        awaitExecutor();
        recorder.finish(first);

        awaitExecutor();
        assertThat(first.getHistogramName(), not(containsString(postfix)));
        assertThat(second.getHistogramName(), not(containsString(postfix)));
    }

    private void awaitExecutor() {
        shadowOf(Looper.getMainLooper()).idle();
        try {
            CommonThreadPolicyFacade.HISTOGRAMS_EXECUTOR.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) { }
    }

    @Test
    public void shouldChangeHistogramName2() {
        final Histogram first = Histogram.LAUNCHER_SEARCH_OPENED_BY_CLICK;
        final Histogram second = Histogram.LAUNCHER_SEARCH_OPENED_BY_SWIPE;
        final String postfix = "First";

        assertThat(first.getHistogramName(), containsString(postfix));
        assertThat(second.getHistogramName(), containsString(postfix));
        recorder.create(first);

        recorder.reportDuration(Histogram.LAUNCHER_ALLAPPS_OPENED_BY_SWIPE, 10);
        recorder.reportDuration(Histogram.LAUNCHER_SEARCH_OPENED_BY_CLICK, 10);
        awaitExecutor();
        recorder.finish(first);

        awaitExecutor();
        assertThat(first.getHistogramName(), not(containsString(postfix)));
        assertThat(second.getHistogramName(), not(containsString(postfix)));
    }

    @Test
    public void shouldReportTotalDuration() throws Exception {
        final long histogramDuration = 10;
        final Set<Histogram> histogramSet = new HashSet<>(recorder.startApplicationHistogramsSet);
        final long totalDuration = histogramDuration * histogramSet.size();

        for (final Histogram histogram : histogramSet) {
            recorder.create(histogram, 0);
            recorder.finish(histogram, 10);
        }

        PowerMockito.verifyPrivate(recorder, times(1))
                .invoke(RECORD_HISTOGRAM_METHOD, Histogram.APPLICATION_LAUNCH_TIME, totalDuration);
    }
}
