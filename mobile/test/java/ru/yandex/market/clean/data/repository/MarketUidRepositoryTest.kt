package ru.yandex.market.clean.data.repository

import com.annimon.stream.Optional
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
import ru.yandex.market.clean.data.fapi.source.muid.MuidFapiClient
import ru.yandex.market.common.schedulers.WorkerScheduler
import ru.yandex.market.data.identifiers.repository.IdentifierRepository
import ru.yandex.market.domain.auth.model.MarketUid
import ru.yandex.market.domain.auth.model.Uuid
import ru.yandex.market.rx.RxSharingJobExecutor

class MarketUidRepositoryTest {

    private val identifierRepository = mock<IdentifierRepository> {
        on { getMarketUid() } doReturn null
    }
    private val muidFapiClient = mock<MuidFapiClient>()
    private val workerScheduler = WorkerScheduler(Schedulers.trampoline())

    @Before
    fun setUp() {
        RxSharingJobExecutor.setTimeProvider { 1L }
    }

    @Test
    fun `Returns value from http client if facade is empty`() {
        val serverMuid = MarketUid("server-market-uid")
        whenever(muidFapiClient.getMarketUid()) doReturn Single.just(serverMuid.value)
        val repository = MarketUidRepositoryImpl(identifierRepository, muidFapiClient, workerScheduler, mock())

        repository.getMarketUid(Uuid("uuid"))
            .test()
            .assertValue(Optional.of(serverMuid))
    }

    @Test
    fun `Returns value from http client if facade failed`() {
        val serverMuid = MarketUid("server-market-uid")
        whenever(identifierRepository.getMarketUid()) doThrow RuntimeException()
        whenever(muidFapiClient.getMarketUid()) doReturn Single.just(serverMuid.value)
        val repository = MarketUidRepositoryImpl(identifierRepository, muidFapiClient, workerScheduler, mock())

        repository.getMarketUid(Uuid("uuid"))
            .test()
            .assertValue(Optional.of(serverMuid))
    }

    @Test
    fun `Returns value from facade if http request failed`() {
        val localMuid = MarketUid("local-market-uid")
        whenever(identifierRepository.getMarketUid()) doReturn localMuid
        whenever(muidFapiClient.getMarketUid()) doThrow RuntimeException()
        val repository = MarketUidRepositoryImpl(identifierRepository, muidFapiClient, workerScheduler, mock())

        repository.getMarketUid(Uuid("uuid"))
            .test()
            .assertValue(Optional.of(localMuid))
    }

    @Test
    fun `Returns empty value is http request and facade failed`() {
        whenever(identifierRepository.getMarketUid()) doThrow RuntimeException()
        whenever(muidFapiClient.getMarketUid()) doThrow RuntimeException()
        val repository = MarketUidRepositoryImpl(identifierRepository, muidFapiClient, workerScheduler, mock())

        repository.getMarketUid(Uuid("uuid"))
            .test()
            .assertValue(Optional.empty())
    }

    @Test
    fun `Returns empty value if uuid is stub`() {
        val uuid = Uuid.stub()
        val repository = MarketUidRepositoryImpl(identifierRepository, muidFapiClient, workerScheduler, mock())

        repository.getMarketUid(uuid)
            .test()
            .assertValue(Optional.empty())
    }

    @Test
    fun `Caches successful http request`() {
        val firstUuid = Uuid("first-uuid")
        val secondUuid = Uuid("second-uuid")
        val firstMuid = MarketUid("first-muid")
        val secondMuid = MarketUid("second-muid")
        whenever(muidFapiClient.getMarketUid()) doReturn Single.just(firstMuid.value)
        val repository = MarketUidRepositoryImpl(identifierRepository, muidFapiClient, workerScheduler, mock())

        repository.getMarketUid(firstUuid)
            .test()
            .assertValue(Optional.of(firstMuid))

        whenever(muidFapiClient.getMarketUid()) doReturn Single.just(secondMuid.value)
        repository.getMarketUid(secondUuid)
            .test()
            .assertValue(Optional.of(secondMuid))
    }

    @Test
    fun `Dont caches failed http request`() {
        val uuid = Uuid("uuid")
        val muid = MarketUid("muid")
        val repository = MarketUidRepositoryImpl(identifierRepository, muidFapiClient, workerScheduler, mock())

        whenever(muidFapiClient.getMarketUid()) doReturn Single.error(RuntimeException())
        repository.getMarketUid(uuid)
            .test()
            .assertNoErrors()

        whenever(muidFapiClient.getMarketUid()) doReturn Single.just(muid.value)
        repository.getMarketUid(uuid)
            .test()
            .assertValue(Optional.of(muid))

        verify(muidFapiClient, times(2)).getMarketUid()
    }

    @Test
    fun `Returns cached http request result if uuid not changed`() {
        val uuid = Uuid("uuid")
        val firstMuid = MarketUid("first-muid")
        val secondMuid = MarketUid("second-muid")
        val repository = MarketUidRepositoryImpl(identifierRepository, muidFapiClient, workerScheduler, mock())

        whenever(muidFapiClient.getMarketUid()).doReturn(Single.just(firstMuid.value))
        repository.getMarketUid(uuid).test().assertValue(Optional.of(firstMuid))
        whenever(muidFapiClient.getMarketUid()).doReturn(Single.just(secondMuid.value))
        repository.getMarketUid(uuid).test().assertValue(Optional.of(firstMuid))

        verify(muidFapiClient, times(1)).getMarketUid()
    }

    @Test
    fun `Http request updates facade`() {
        val muid = MarketUid("muid")
        whenever(muidFapiClient.getMarketUid()) doReturn Single.just(muid.value)
        val repository = MarketUidRepositoryImpl(identifierRepository, muidFapiClient, workerScheduler, mock())

        repository.getMarketUid(Uuid("uuid")).test().assertNoErrors()

        verify(identifierRepository, times(1)).setMarketUid(muid)
    }

    @Test
    fun `Observe emits value even if get is not called`() {
        val muid = MarketUid("market-uid")
        whenever(identifierRepository.getMarketUid()) doReturn muid
        val repository = MarketUidRepositoryImpl(identifierRepository, muidFapiClient, workerScheduler, mock())

        repository.observeMarketUidStream(Uuid("uuid")).test().assertValue(Optional.of(muid))
    }

    @Test
    fun `Observe emits value and continues`() {
        whenever(identifierRepository.getMarketUid()) doReturn MarketUid("market-uid")
        val repository = MarketUidRepositoryImpl(identifierRepository, muidFapiClient, workerScheduler, mock())

        repository.observeMarketUidStream(Uuid("uuid"))
            .test()
            .assertNotComplete()
            .assertNoErrors()
    }

}
