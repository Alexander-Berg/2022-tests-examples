package ru.yandex.market.clean.data.repository

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.store.MetricaIdentifierDataStore
import ru.yandex.market.common.schedulers.TimerScheduler
import ru.yandex.market.common.schedulers.WorkerScheduler
import ru.yandex.market.data.identifiers.repository.IdentifierRepository
import ru.yandex.market.domain.auth.model.Uuid
import ru.yandex.market.identifier.MetricaIdentifiers
import ru.yandex.market.rx.RxSharingJobExecutor
import ru.yandex.market.test.extensions.asSingle

class UuidRepositoryTest {

    private val metricaStore = mock<MetricaIdentifierDataStore>()
    private val identifierRepository = mock<IdentifierRepository>()
    private val workerScheduler = WorkerScheduler(Schedulers.trampoline())

    @Before
    fun setUp() {
        RxSharingJobExecutor.setTimeProvider { 1L }
    }

    @Test
    fun `Returns value from facade if metrica is failed`() {
        val uuid = Uuid("facade-uuid")
        whenever(metricaStore.getIdentifiers()) doReturn Single.error(RuntimeException())
        whenever(identifierRepository.getUuid()) doReturn uuid
        val repository = UuidRepositoryImpl(
            metricaStore,
            identifierRepository,
            workerScheduler,
            TimerScheduler(Schedulers.trampoline())
        )

        repository.getUuid()
            .test()
            .assertValue(uuid)
    }

    @Test
    fun `Returns value from metrica if facade is failed`() {
        val uuid = Uuid("metrica-uuid")
        whenever(metricaStore.getIdentifiers()) doReturn MetricaIdentifiers(uuid, null).asSingle()
        whenever(identifierRepository.getUuid()) doThrow RuntimeException()
        val repository = UuidRepositoryImpl(
            metricaStore,
            identifierRepository,
            workerScheduler,
            TimerScheduler(Schedulers.trampoline())
        )

        repository.getUuid()
            .test()
            .assertValue(uuid)
    }

    @Test
    fun `Returns value from metrica if facade is empty`() {
        val uuid = Uuid("metrica-uuid")
        whenever(metricaStore.getIdentifiers()) doReturn MetricaIdentifiers(uuid, null).asSingle()
        whenever(identifierRepository.getUuid()) doReturn null
        val repository = UuidRepositoryImpl(
            metricaStore,
            identifierRepository,
            workerScheduler,
            TimerScheduler(Schedulers.trampoline())
        )

        repository.getUuid()
            .test()
            .assertValue(uuid)
    }

    @Test
    fun `Returns stub value if facade and metrica failed`() {
        whenever(metricaStore.getIdentifiers()) doReturn Single.error(RuntimeException())
        whenever(identifierRepository.getUuid()) doThrow RuntimeException()
        val repository = UuidRepositoryImpl(
            metricaStore,
            identifierRepository,
            workerScheduler,
            TimerScheduler(Schedulers.trampoline())
        )

        repository.getUuid()
            .test()
            .assertValue(Uuid.stub())
    }

    @Test
    fun `Caches successful metrica request`() {
        val repository = UuidRepositoryImpl(
            metricaStore,
            identifierRepository,
            workerScheduler,
            TimerScheduler(Schedulers.trampoline())
        )
        whenever(metricaStore.getIdentifiers()) doReturn MetricaIdentifiers(Uuid("uuid"), null).asSingle()

        repository.getUuid().test().assertNoErrors()
        repository.getUuid().test().assertNoErrors()

        verify(metricaStore, times(1)).getIdentifiers()
    }

    @Test
    fun `Dont caches failed metrica request`() {
        val uuid = Uuid("uuid")
        val repository = UuidRepositoryImpl(
            metricaStore,
            identifierRepository,
            workerScheduler,
            TimerScheduler(Schedulers.trampoline())
        )
        whenever(metricaStore.getIdentifiers()) doReturn Single.error(RuntimeException())

        repository.getUuid().test().assertNoErrors()
        whenever(metricaStore.getIdentifiers()) doReturn MetricaIdentifiers(uuid, null).asSingle()
        repository.getUuid().test().assertValue(uuid)

        verify(metricaStore, times(2)).getIdentifiers()
    }

    @Test
    fun `Metrica request updates facade`() {
        val uuid = Uuid("uuid")
        whenever(metricaStore.getIdentifiers()) doReturn MetricaIdentifiers(uuid, null).asSingle()
        val repository = UuidRepositoryImpl(
            metricaStore,
            identifierRepository,
            workerScheduler,
            TimerScheduler(Schedulers.trampoline())
        )

        repository.getUuid().test().assertNoErrors()

        verify(identifierRepository, times(1)).setUuid(uuid)
    }

    @Test
    fun `Observe emits value even if get is not called`() {
        val uuid = Uuid("uuid")
        whenever(metricaStore.getIdentifiers()) doReturn Single.error(RuntimeException())
        whenever(identifierRepository.getUuid()) doReturn uuid
        val repository = UuidRepositoryImpl(
            metricaStore,
            identifierRepository,
            workerScheduler,
            TimerScheduler(Schedulers.trampoline())
        )

        repository.observeUuidStream()
            .test()
            .assertValue(uuid)
    }

    @Test
    fun `Observe emits value and continues`() {
        whenever(identifierRepository.getUuid()) doReturn Uuid("uuid")
        val repository = UuidRepositoryImpl(
            metricaStore,
            identifierRepository,
            workerScheduler,
            TimerScheduler(Schedulers.trampoline())
        )

        repository.observeUuidStream()
            .test()
            .assertNotComplete()
            .assertNoErrors()
    }

}
