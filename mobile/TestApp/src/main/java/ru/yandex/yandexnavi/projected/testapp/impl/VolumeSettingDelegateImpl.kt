package ru.yandex.yandexnavi.projected.testapp.impl

import ru.yandex.yandexnavi.projected.platformkit.dependencies.settings.VolumeSettingDelegate
import ru.yandex.yandexnavi.projected.testapp.R
import javax.inject.Inject

class VolumeSettingDelegateImpl @Inject constructor() : VolumeSettingDelegate {

    private val options = listOf(
        R.string.settings_volume_min,
        R.string.settings_volume_middle,
        R.string.settings_volume_max
    )

    override val settings: List<VolumeSettingDelegate.VolumeSetting> =
        options.map(VolumeSettingDelegate::VolumeSetting)

    override var selectedSettingIndex: Int = 0
}
