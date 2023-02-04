package ru.auto.ara.core.feature.mic_promo

import ru.auto.feature.mic_promo_api.IMicPromoDomainInteractor
import ru.auto.feature.mic_promo_api.MicPromoDialogAppearance
import rx.Completable
import rx.Single

class TestMicPromoDomainInteractor(
    val args: Args,
) : IMicPromoDomainInteractor {

    override fun isMicPromoExpEnabled(): Boolean = args.isMicPromoExpEnabled()

    override fun isMicrophonePermissionGranted(): Boolean = args.isMicrophoneGranted()

    override fun isMicPromoExpEnabledAndPromoRequired(): Boolean =
        isMicPromoExpEnabled() && isMicrophonePermissionGranted().not()

    override fun getMicPromoDialogAppearance(shouldShowMicPermissionRationale: Boolean): MicPromoDialogAppearance =
        args.micPromoDialogAppearance()

    override fun needToShowGreenDialogAtAppStart(): Single<Boolean> = Single.just(args.needToShowGreenDialogAtAppStart())

    override fun onGreenDialogShownAtAppStart(): Completable = Completable.complete()

    data class Args(
        val isMicPromoExpEnabled: () -> Boolean,
        val isMicrophoneGranted: () -> Boolean,
        val needToShowGreenDialogAtAppStart: () -> Boolean,
        val micPromoDialogAppearance: () -> MicPromoDialogAppearance,
    )
}
