package ru.auto.ara.core.feature.mic_promo

import ru.auto.feature.mic_promo_api.MicPromoDialogAppearance

object TestMicPromoArgsHolder {
    var isMicPromoExpEnabled = false
    var isMicrophoneGranted = false
    var needToShowGreenDialogAtAppStart = false
    var micPromoDialogAppearance = MicPromoDialogAppearance.NO_NEED

    val args = TestMicPromoDomainInteractor.Args(
        isMicPromoExpEnabled = { isMicPromoExpEnabled },
        isMicrophoneGranted = { isMicrophoneGranted },
        needToShowGreenDialogAtAppStart = { needToShowGreenDialogAtAppStart },
        micPromoDialogAppearance = { micPromoDialogAppearance },
    )

    fun setupExpDisabled() {
        isMicPromoExpEnabled = false
        isMicrophoneGranted = false
        needToShowGreenDialogAtAppStart = false
        micPromoDialogAppearance = MicPromoDialogAppearance.NO_NEED
    }

    fun setupAbleToShowSystemDialog() {
        isMicPromoExpEnabled = true
        isMicrophoneGranted = false
        micPromoDialogAppearance = MicPromoDialogAppearance.WITH_SYSTEM_DIALOG_ASKING
    }

    fun setupAbleToShowSettingsDialog() {
        isMicPromoExpEnabled = true
        isMicrophoneGranted = false
        micPromoDialogAppearance = MicPromoDialogAppearance.WITH_SETTINGS_OPENING
    }
}
