package ru.yandex.supercheck.domain.init

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.supercheck.analytics.Reporter
import ru.yandex.supercheck.core.scheduler.RxSchedulers
import ru.yandex.supercheck.data.network.interceptors.deviceinfo.DeviceInfoWrapper
import ru.yandex.supercheck.domain.address.AddressRefreshInteractor
import ru.yandex.supercheck.domain.auth.Authorization
import ru.yandex.supercheck.domain.config.ConfigRepository
import ru.yandex.supercheck.domain.notification.NotificationRepository
import ru.yandex.supercheck.domain.region.UserPreferencesRepository
import ru.yandex.supercheck.domain.session.SessionInteractor
import ru.yandex.supercheck.domain.shops.GeoLocationRepository
import ru.yandex.supercheck.model.domain.init.StartupResult
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(MockitoJUnitRunner::class)
class InitializationInteractorTest {

    companion object {
        const val TEST_UUID = "123"
        const val TEST_DEVICE_ID = "321"
        const val TEST_DEVICE_ID_HASH = "abcde"
    }

    private val startupRepository = mock<StartupRepository>()
    private val idRepository = mock<IdRepository>()
    private val rxSchedulers = mock<RxSchedulers>()
    private val notificaRepository = mock<NotificationRepository>()
    private val deviceInfo = mock<DeviceInfoWrapper>()
    private val configRepository = mock<ConfigRepository>()
    private val locationRepository = mock<GeoLocationRepository>()
    // test scheduler for managing execution time
    private val testScheduler = TestScheduler()
    private val fabricRepository = mock<FabricRepository>()
    private val userPreferencesRepository = mock<UserPreferencesRepository>()
    private val authorization = mock<Authorization>()
    private val sessionInteractor = mock<SessionInteractor>()
    private val addressRefreshInteractor = mock<AddressRefreshInteractor>()
    private val repoter = mock<Reporter>()

    private lateinit var initializationInteractor: InitializationInteractor

    @Before
    fun setUp() {
        initializationInteractor =
            InitializationInteractor(
                startupRepository,
                idRepository,
                rxSchedulers,
                notificaRepository,
                deviceInfo,
                locationRepository,
                fabricRepository,
                configRepository,
                userPreferencesRepository,
                authorization,
                addressRefreshInteractor,
                sessionInteractor,
                repoter
            )

        whenever(rxSchedulers.io).thenReturn(testScheduler)
        whenever(locationRepository.getLastGeoLocation()).thenReturn(Maybe.empty())
        whenever(authorization.refreshAuthorization()).thenReturn(Completable.complete())
        whenever(sessionInteractor.saveOpening()).thenReturn(Completable.complete())
        whenever(addressRefreshInteractor.refreshState()).thenReturn(Completable.complete())
    }

    @Test
    fun testStartInitializationSuccessfulFinish() {
        // make StartupRepository return stub StartupResult
        val startupResult = StartupResult(TEST_UUID, TEST_DEVICE_ID, TEST_DEVICE_ID_HASH)
        whenever(startupRepository.sendStartup()).thenReturn(Single.just(startupResult))

        // invoking testing method
        val testObserver = initializationInteractor.startInitialization().test()

        // advancing time forward to 2 seconds to be sure that start delay time expired
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)

        // init should be completed and ids should be saved
        testObserver.assertComplete()
        verify(idRepository).deviceId = TEST_DEVICE_ID
        verify(idRepository).uuid = TEST_UUID
    }

    @Test
    fun testStartInitializationIdsFetchedBeforeStartDelay() {
        // make StartupRepository return stub StartupResult
        val startupResult = StartupResult(TEST_UUID, TEST_DEVICE_ID, TEST_DEVICE_ID_HASH)
        whenever(startupRepository.sendStartup()).thenReturn(Single.just(startupResult))

        // invoking testing method
        val testObserver = initializationInteractor.startInitialization().test()

        // init should not be completed but ids should be already saved
        testObserver.assertNotComplete()
        verify(idRepository).deviceId = TEST_DEVICE_ID
        verify(idRepository).uuid = TEST_UUID
    }

    @Test
    fun testStartInitializationFinishStartDelayExpiredBeforeIdsFetched() {
        // make StartupRepository never return any result
        whenever(startupRepository.sendStartup()).thenReturn(Single.never())

        // invoking testing method
        val testObserver = initializationInteractor.startInitialization().test()

        // advancing time forward to 2 seconds to be sure that start delay time expired
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)

        // TimeoutException should be fetched and no ids should be saved
        testObserver.assertError(TimeoutException::class.java)
        verify(idRepository, never()).deviceId = TEST_DEVICE_ID
        verify(idRepository, never()).uuid = TEST_UUID
    }

    @Test
    fun testStartInitializationErrorWhileFetchingIdsStartDelayNotExpired() {
        // make StartupRepository return some error
        whenever(startupRepository.sendStartup()).thenReturn(Single.error(Exception()))

        // invoking testing method
        val testObserver = initializationInteractor.startInitialization().test()

        // Despite the error it shouldn't be fetched before start delay expired
        testObserver.assertNotComplete()
        testObserver.assertNoErrors()
        verify(idRepository, never()).deviceId = TEST_DEVICE_ID
        verify(idRepository, never()).uuid = TEST_UUID
    }

    @Test
    fun testStartInitializationErrorWhileFetchingIdsStartDelayExpired() {
        // make StartupRepository return some error
        whenever(startupRepository.sendStartup()).thenReturn(Single.error(Exception()))

        // invoking testing method
        val testObserver = initializationInteractor.startInitialization().test()

        // advancing time forward to 2 seconds to be sure that start delay time expired
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)

        // Error should be fetched after start delay expired
        testObserver.assertComplete()
        verify(idRepository, never()).deviceId = TEST_DEVICE_ID
        verify(idRepository, never()).uuid = TEST_UUID
    }

    @Test
    fun testUpdateIds() {
        // make StartupRepository return stub StartupResult
        val startupResult = StartupResult(TEST_UUID, TEST_DEVICE_ID, TEST_DEVICE_ID_HASH)
        whenever(startupRepository.sendStartup()).thenReturn(Single.just(startupResult))

        // invoking testing method
        val testObserver = initializationInteractor.updateIds().test()

        // ids should be successfully updated
        testObserver.assertComplete()
        verify(idRepository).deviceId = TEST_DEVICE_ID
        verify(idRepository).uuid = TEST_UUID
    }
}