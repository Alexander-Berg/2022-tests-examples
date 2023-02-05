package ru.yandex.yandexnavi.projected.testapp.impl

import ru.yandex.yandexnavi.projected.platformkit.dependencies.settings.AudioSettingDelegate
import ru.yandex.yandexnavi.projected.testapp.R
import javax.inject.Inject

class AudioSettingDelegateImpl @Inject constructor() : AudioSettingDelegate {

    private val options = listOf(
        R.string.settings_sound_on,
        R.string.settings_sound_off,
        R.string.settings_sound_important
    )

    override val settings: List<AudioSettingDelegate.AudioSetting> =
        options.map(AudioSettingDelegate::AudioSetting)

    override var selectedSettingIndex: Int = 0
}
