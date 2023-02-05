package ru.yandex.yandexmaps.guidance.annotations.remote.download;

import com.annimon.stream.Stream;
import com.evernote.android.job.Job;
import com.gojuno.koptional.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import ru.yandex.maps.utils.CaptorUtils;
import ru.yandex.yandexmaps.utils.CollectionUtils;
import ru.yandex.yandexmaps.guidance.annotations.remote.RemoteVoicesRepository;
import ru.yandex.maps.storiopurgatorium.voice.VoiceMetadata;

import static com.gojuno.koptional.OptionalKt.toOptional;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.maps.utils.CaptorUtils.captor;
import static ru.yandex.yandexmaps.guidance.annotations.remote.download.DownloadVoicesJobVoice.voice;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DownloadVoicesJobTest {

    private RemoteVoicesRepository repository;
    private VoiceDownloader downloader;
    private Job.Params params;

    private DownloadVoicesJob job;

    private int failedStatusCounter;

    @Before
    public void setUp() {
        repository = mock(RemoteVoicesRepository.class);
        when(repository.nextScheduled()).thenReturn(Single.just(toOptional(null)));
        when(repository.failed()).thenReturn(Observable.just(Collections.emptyList()));
        when(repository.downloading()).thenReturn(Observable.just(Collections.emptyList()));

        downloader = mock(VoiceDownloader.class);
        params = null;

        //Inject fields
        job = new DownloadVoicesJob(it -> {
            it.repository = repository;
            it.downloader = downloader;
        });

        failedStatusCounter = 0;
    }

    @Test
    public void downloadingNotStarted_whenNoScheduledVoices_jobSuccess() {
        final Job.Result result = job.onRunJob(params);

        assertEquals(Job.Result.SUCCESS, result);

        verify(downloader, never()).download(any());
    }

    @Test
    public void failedRescheduled_jobRescheduled() {
        final List<VoiceMetadata> failed = CollectionUtils.map(voices(10), it -> it.failed(nextFailedStatus()));
        when(repository.failed()).thenReturn(Observable.just(failed));

        final Job.Result result = job.onRunJob(params);

        assertEquals(Job.Result.RESCHEDULE, result);

        final ArgumentCaptor<List<VoiceMetadata>> captor = CaptorUtils.listCaptor();
        verify(repository).update(captor.capture());

        assertListEquals(CollectionUtils.map(failed, VoiceMetadata::scheduled), captor.getValue());
    }

    @VoiceMetadata.Status.Failed
    private int nextFailedStatus() {
        switch ((failedStatusCounter++) % 4) {
            default:
                return VoiceMetadata.Status.Failed.UNKNOWN;
            case 1:
                return VoiceMetadata.Status.Failed.SERVER;
            case 2:
                return VoiceMetadata.Status.Failed.STORAGE;
            case 4:
                return VoiceMetadata.Status.Failed.NETWORK;
        }
    }

    @Test
    public void scheduledDownloads_jobSuccess() {
        final List<VoiceMetadata> scheduled = CollectionUtils.map(voices(10), VoiceMetadata::scheduled);
        when(repository.nextScheduled()).thenReturn(singleReturnsOnSequentialSubscribes(scheduled));
        when(downloader.download(any())).thenReturn(Completable.complete());
        when(repository.cancellations(any())).thenReturn(Observable.empty());

        final Job.Result result = job.onRunJob(params);

        assertEquals(Job.Result.SUCCESS, result);

        final ArgumentCaptor<VoiceMetadata> captor = captor(VoiceMetadata.class);

        verify(downloader, times(scheduled.size())).download(captor.capture());

        assertListEquals(scheduled, captor.getAllValues());
    }

    @Test
    public void cancellationEmit_leadsToUnsubscribingFromDownload_JobSuccess() throws InterruptedException {

        CountDownLatch cancelled = new CountDownLatch(1);

        final VoiceMetadata voice = voice(1);
        when(repository.nextScheduled()).thenReturn(singleReturnsOnSequentialSubscribes(Collections.singletonList(voice)));
        when(downloader.download(any())).thenReturn(Completable.create(em -> em.setCancellable(cancelled::countDown)));
        when(repository.cancellations(eq(voice))).thenReturn(Observable.just(voice.cancelled()));

        final TestObserver<Job.Result> result = Single.fromCallable(() -> job.onRunJob(params))
                .subscribeOn(Schedulers.io())
                .test();

        cancelled.await(1, TimeUnit.SECONDS);

        result.awaitTerminalEvent();
        result.assertValue(Job.Result.SUCCESS);
    }


    private Single<Optional<VoiceMetadata>> singleReturnsOnSequentialSubscribes(List<VoiceMetadata> values) {
        final Deque<VoiceMetadata> copied = new ArrayDeque<>(values);

        return Single.create(emitter -> emitter.onSuccess(toOptional(copied.isEmpty() ? null : copied.pop())));
    }

    private void assertListEquals(List<VoiceMetadata> expected, List<VoiceMetadata> actual) {
        assertEquals(expected.size(), actual.size());
        assertTrue(Stream.of(expected).allMatch(actual::contains));
        assertTrue(Stream.of(actual).allMatch(expected::contains));
    }

    private List<VoiceMetadata> voices(int count) {
        return Stream.range(0, count)
                .map(DownloadVoicesJobVoice::voice)
                .toList();
    }

}