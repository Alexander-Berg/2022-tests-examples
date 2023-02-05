package ru.yandex.market.clean.domain.usecase.checkout

import com.annimon.stream.Optional
import io.reactivex.Completable
import io.reactivex.Single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.checkout.domain.usecase.UserProfilesUseCase
import ru.yandex.market.clean.domain.usecase.SyncProfilesUseCase
import ru.yandex.market.data.passport.Profile
import ru.yandex.market.data.passport.Recipient
import ru.yandex.market.domain.delivery.model.DeliveryType
import ru.yandex.market.test.extensions.asSingle
import ru.yandex.market.test.extensions.checkArg
import ru.yandex.market.utils.asOptional

@RunWith(MockitoJUnitRunner::class)
class RememberSelectedDeliveryProfileUseCaseTest {

    private val userProfilesUseCase = mock<UserProfilesUseCase>()
    private val syncProfilesUseCase = mock<SyncProfilesUseCase>()
    private val useCase = RememberSelectedDeliveryProfileUseCase(userProfilesUseCase, syncProfilesUseCase)

    @Test
    fun `When pickup profile recipient differs from existing profile recipient, copy existing profile with new recipient and save it`() {
        val existingProfile = Profile().apply {
            recipient = Recipient.create("Joe", "", "")
        }
        whenever(userProfilesUseCase.getProfile(any()))
            .thenReturn(existingProfile.asOptional().asSingle())
        whenever(userProfilesUseCase.forceSaveProfile(any())).thenReturn(Completable.complete())

        val profile = Profile().apply {
            recipient = Recipient.create("Ann", "", "")
        }
        useCase.rememberSelectedProfile(profile, DeliveryType.PICKUP)
            .subscribe()

        verify(userProfilesUseCase).forceSaveProfile(checkArg {
            assertThat(it.recipient).isEqualTo(profile.recipient)
        })
    }

    @Test
    fun `Do nothing when pickup profile recipient same as existing profile recipient`() {
        val recipient = Recipient.create("Joe", "", "")
        val existingProfile = Profile().apply {
            this.recipient = recipient
        }
        whenever(userProfilesUseCase.getProfile(any()))
            .thenReturn(existingProfile.asOptional().asSingle())

        val profile = Profile().apply {
            this.recipient = recipient
        }
        useCase.rememberSelectedProfile(profile, DeliveryType.PICKUP)
            .subscribe()

        verify(userProfilesUseCase, never()).forceSaveProfile(any())
        verify(userProfilesUseCase, never()).updateOrSaveProfile(any())
    }

    @Test
    fun `When pickup profile is not existing, save it`() {
        whenever(userProfilesUseCase.getProfile(any()))
            .thenReturn(Optional.empty<Profile>().asSingle())
        whenever(userProfilesUseCase.updateOrSaveProfile(any())).thenReturn(Single.just(Profile()))

        val profile = Profile()
        useCase.rememberSelectedProfile(profile, DeliveryType.PICKUP)
            .subscribe()

        verify(userProfilesUseCase).updateOrSaveProfile(checkArg {
            assertThat(it).isSameAs(profile)
        })
    }

    @Test
    fun `Simply save delivery profile`() {
        whenever(userProfilesUseCase.updateOrSaveProfile(any())).thenReturn(Single.just(Profile()))
        whenever(syncProfilesUseCase.execute()).thenReturn(Completable.complete())

        val profile = Profile()
        useCase.rememberSelectedProfile(profile, DeliveryType.DELIVERY)
            .subscribe()

        verify(userProfilesUseCase).updateOrSaveProfile(checkArg {
            assertThat(it).isSameAs(profile)
        })
    }

}
