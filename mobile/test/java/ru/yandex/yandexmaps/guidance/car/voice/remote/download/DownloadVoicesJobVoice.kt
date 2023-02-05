package ru.yandex.yandexmaps.guidance.annotations.remote.download

import ru.yandex.maps.storiopurgatorium.voice.VoiceMetadata

object DownloadVoicesJobVoice {

    @JvmStatic
    fun voice(index: Int) = VoiceMetadata(
        locale = "ru",
        selected = false,
        remoteId = "ru_oksana$index",
        title = "Oksana $index",
        url = "https://yandex.ru/voices/full/oksana$index.zip",
        sampleUrl = "https://yandex.ru/voices/samples/oksana$index.zip",
        status = VoiceMetadata.Status.NOT_LOADED,
        type = VoiceMetadata.VoiceType.REMOTE,
        version = "1.0",
        path = null,
    )
}
