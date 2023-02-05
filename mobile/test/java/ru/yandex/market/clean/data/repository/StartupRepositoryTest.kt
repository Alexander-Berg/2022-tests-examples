package ru.yandex.market.clean.data.repository

import com.annimon.stream.OptionalInt
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Test
import ru.yandex.market.clean.data.store.ApplicationInformationDataStore

class StartupRepositoryTest {

    private val appInfoDataStore = mock<ApplicationInformationDataStore>()

    private val preferencesRepository = mock<PreferencesRepository>()

    private val repository = StartupRepository(appInfoDataStore, preferencesRepository)

    @Test
    fun `Is first launch after update return true when saved application version differs from current`() {
        whenever(appInfoDataStore.version).thenReturn(Single.just(42))
        whenever(preferencesRepository.getApplicationVersion())
            .thenReturn(Single.just(OptionalInt.of(0)))
        whenever(preferencesRepository.setApplicationVersion(any()))
            .thenReturn(Completable.complete())
        whenever(preferencesRepository.setGdprNotificationShown(any()))
            .thenReturn(Completable.complete())

        repository.isFirstLaunchAfterUpdate
            .test()
            .assertValue(true)
    }

    @Test
    fun `Is first launch after update return true when saved application version is empty`() {
        whenever(appInfoDataStore.version).thenReturn(Single.just(42))
        whenever(preferencesRepository.getApplicationVersion())
            .thenReturn(Single.just(OptionalInt.empty()))
        whenever(preferencesRepository.setApplicationVersion(any()))
            .thenReturn(Completable.complete())
        whenever(preferencesRepository.setGdprNotificationShown(any()))
            .thenReturn(Completable.complete())

        repository.isFirstLaunchAfterUpdate
            .test()
            .assertValue(true)
    }

    @Test
    fun `Is first launch after update return false when saved application version same as current`() {
        whenever(appInfoDataStore.version).thenReturn(Single.just(42))
        whenever(preferencesRepository.getApplicationVersion())
            .thenReturn(Single.just(OptionalInt.of(42)))

        repository.isFirstLaunchAfterUpdate
            .test()
            .assertValue(false)
    }

    @Test
    fun `Is first launch after update caches result value after first call`() {
        whenever(appInfoDataStore.version).thenReturn(Single.just(42))
        whenever(preferencesRepository.getApplicationVersion())
            .thenReturn(Single.just(OptionalInt.of(42)))

        repository.isFirstLaunchAfterUpdate
            .test()
            .assertValue(false)

        repository.isFirstLaunchAfterUpdate
            .test()
            .assertValue(false)

        verify(appInfoDataStore).version
        verify(preferencesRepository).getApplicationVersion()
    }

    @Test
    fun `Is first launch after update saves current value when saved value differs from current`() {
        val currentVersion = 42
        whenever(appInfoDataStore.version).thenReturn(Single.just(currentVersion))
        whenever(preferencesRepository.getApplicationVersion())
            .thenReturn(Single.just(OptionalInt.of(0)))
        whenever(preferencesRepository.setApplicationVersion(any()))
            .thenReturn(Completable.complete())
        whenever(preferencesRepository.setGdprNotificationShown(any()))
            .thenReturn(Completable.complete())

        repository.isFirstLaunchAfterUpdate.subscribe()

        verify(preferencesRepository).setApplicationVersion(currentVersion)
    }

    @Test
    fun `Is first launch after update saves current value when saved value is empty`() {
        val currentVersion = 42
        whenever(appInfoDataStore.version).thenReturn(Single.just(currentVersion))
        whenever(preferencesRepository.getApplicationVersion())
            .thenReturn(Single.just(OptionalInt.empty()))
        whenever(preferencesRepository.setApplicationVersion(any()))
            .thenReturn(Completable.complete())
        whenever(preferencesRepository.setGdprNotificationShown(any()))
            .thenReturn(Completable.complete())

        repository.isFirstLaunchAfterUpdate.subscribe()

        verify(preferencesRepository).setApplicationVersion(currentVersion)
    }
}