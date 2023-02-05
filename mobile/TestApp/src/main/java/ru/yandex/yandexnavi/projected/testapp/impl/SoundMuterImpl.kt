package ru.yandex.yandexnavi.projected.testapp.impl

import com.yandex.navikit.guidance.SoundMuter

class SoundMuterImpl : SoundMuter {
    private var muted = false

    override fun isMuted() = muted

    override fun setMuted(muted: Boolean) {
        this.muted = muted
    }
}
