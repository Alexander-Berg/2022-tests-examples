package ru.yandex.market.checkout.data.repository

import dagger.Lazy
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.repository.DeliveryRepository
import ru.yandex.market.common.schedulers.WorkerScheduler
import ru.yandex.market.data.passport.Address
import ru.yandex.market.data.passport.Profile
import ru.yandex.market.db.PassportFacade
import ru.yandex.market.test.extensions.checkArg

@RunWith(MockitoJUnitRunner::class)
class ProfilesRepositoryTest {

    private val passportFacade = mock<PassportFacade>()
    private val deliveryRepository = mock<DeliveryRepository>()
    private val workerScheduler = WorkerScheduler(Schedulers.trampoline())
    private val repository = ProfilesRepository(
        passportFacade,
        Lazy { deliveryRepository },
        workerScheduler
    )

    @Test
    fun `Force save profile makes copy of current profile with zero ids and sync dirty flag`() {
        whenever(passportFacade.saveProfile(any())).thenReturn(Profile())

        val address = Address.testBuilder()
            .profileId(1)
            .build()
        val profile = Profile.testBuilder()
            .id(1)
            .serverId("some-id")
            .isSyncDirty(false)
            .address(address)
            .build()
        repository.forceSaveProfile(profile).subscribe()

        verify(passportFacade).saveProfile(checkArg {
            assertThat(it).isNotSameAs(profile)
            val modifiedProfile = profile.toBuilder()
                .id(0)
                .serverId(null)
                .isSyncDirty(true)
                .address(
                    address.toBuilder()
                        .profileId(0)
                        .build()
                )
                .build()
            assertThat(it).isEqualTo(modifiedProfile)
        })
    }

    @Test
    fun `Update or save profile copying profile with sync dirty flag and pass profile to facade`() {
        whenever(passportFacade.saveProfile(any())).thenReturn(Profile())

        val profile = Profile.testBuilder()
            .isSyncDirty(false)
            .build()
        repository.updateOrSaveProfile(profile).subscribe()

        verify(passportFacade).saveProfile(checkArg {
            assertThat(it).isNotSameAs(profile)
            val modifiedProfile = profile.toBuilder()
                .isSyncDirty(true)
                .build()
            assertThat(it).isEqualTo(modifiedProfile)
        })
    }

    @Test
    fun `removeProfile do nothing when there is no profile with passed id in database`() {
        whenever(passportFacade.get(any())).thenReturn(null)

        repository.removeProfile(42)
            .test()
            .assertComplete()

        verify(passportFacade, never()).saveProfile(any())
    }

    @Test
    fun `removeProfile saves copy of existing profile with deleted flag`() {
        val profile = Profile.testBuilder()
            .isDeleted(false)
            .build()
        whenever(passportFacade.get(any())).thenReturn(profile)
        whenever(passportFacade.saveProfile(any())).thenReturn(Profile.testBuilder().build())

        repository.removeProfile(42)
            .test()
            .assertComplete()

        verify(passportFacade).saveProfile(checkArg {
            val modifiedProfile = profile.toBuilder()
                .isDeleted(true)
                .build()
            assertThat(it).isNotSameAs(profile)
            assertThat(it).isEqualTo(modifiedProfile)
        })
    }
}