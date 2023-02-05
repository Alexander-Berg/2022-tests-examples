package ru.yandex.yandexmaps.launch

import android.app.Application
import com.gojuno.koptional.Optional
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.pushtorefresh.storio3.sqlite.operations.delete.DeleteResult
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.yandex.maps.storiopurgatorium.voice.VoiceMetadata
import ru.yandex.yandexmaps.app.VoiceUpdater
import ru.yandex.yandexmaps.common.locale.LocaleUtil
import ru.yandex.yandexmaps.guidance.annotations.remote.RemoteVoicesRepository
import ru.yandex.yandexmaps.guidance.annotations.remote.download.DownloadVoicesService
import ru.yandex.yandexmaps.multiplatform.core.utils.extensions.ConnectivityStatus
import java.util.concurrent.TimeUnit

class VoiceUpdaterTest {

    private lateinit var localeUtil: MockedStatic<LocaleUtil>
    private lateinit var downloadVoicesService: DownloadVoicesService
    private lateinit var voiceUpdater: VoiceUpdater
    private lateinit var remoteVoicesRepository: RemoteVoicesRepository

    @Before
    fun setup() {
        val application = mock(Application::class.java)
        remoteVoicesRepository = mock(RemoteVoicesRepository::class.java)
        downloadVoicesService = mock(DownloadVoicesService::class.java)

        voiceUpdater = spy(VoiceUpdater(application, remoteVoicesRepository, downloadVoicesService, Schedulers.trampoline()))

        localeUtil = mockStatic(LocaleUtil::class.java)
    }

    @Test
    fun `ukranian language selected, uk_male voice selected, remote has uk_male voice, voice will delete, then download`() {
        localeUtil.`when`<Any> { LocaleUtil.currentLang }.thenReturn(LocaleUtil.LANG_UKRAINIAN)

        `when`(remoteVoicesRepository.voiceById("uk_male")).thenReturn(Observable.just(Optional.toOptional(UK_MALE_LOCAL_SELECTED_VOICE_METADATA)))
        `when`(remoteVoicesRepository.voices()).thenReturn(Observable.just(REMOTE_VOICES))
        `when`(remoteVoicesRepository.delete(any())).thenReturn(Single.just(DeleteResult.newInstance(1, "test", "test")))

        doReturn(Observable.just(ConnectivityStatus.CONNECTED)).`when`(voiceUpdater).connectivityChanges()

        voiceUpdater.update()

        verify(downloadVoicesService).selectRemoteVoiceById("uk_male")
    }

    @Test
    fun `ukranian language selected, uk_male voice not selected, remote has uk_male voice, voice will delete`() {
        localeUtil.`when`<Any> { LocaleUtil.currentLang }.thenReturn(LocaleUtil.LANG_UKRAINIAN)

        `when`(remoteVoicesRepository.voiceById("uk_male")).thenReturn(Observable.just(Optional.toOptional(UK_MALE_LOCAL_NOT_SELECTED_VOICE_METADATA)))
        `when`(remoteVoicesRepository.voices()).thenReturn(Observable.just(REMOTE_VOICES))

        doReturn(Observable.just(ConnectivityStatus.CONNECTED)).`when`(voiceUpdater).connectivityChanges()

        voiceUpdater.update()

        verify(remoteVoicesRepository).delete(UK_MALE_LOCAL_NOT_SELECTED_VOICE_METADATA)
        verify(downloadVoicesService, never()).selectRemoteVoiceById("uk_male")
    }

    @Test
    fun `interenet appeared after 2 sec, ukranian language selected, uk_male voice selected, remote has uk_male voice, voice will download`() {
        localeUtil.`when`<Any> { LocaleUtil.currentLang }.thenReturn(LocaleUtil.LANG_UKRAINIAN)

        `when`(remoteVoicesRepository.voiceById("uk_male")).thenReturn(Observable.just(Optional.toOptional(UK_MALE_LOCAL_SELECTED_VOICE_METADATA)))
        `when`(remoteVoicesRepository.voices()).thenReturn(Observable.just(REMOTE_VOICES))
        `when`(remoteVoicesRepository.delete(any())).thenReturn(Single.just(DeleteResult.newInstance(1, "test", "test")))

        doReturn(
            Observable.just(ConnectivityStatus.NOT_CONNECTED, ConnectivityStatus.CONNECTED)
                .zipWith(Observable.interval(1, TimeUnit.SECONDS, Schedulers.trampoline())) { status, _ -> status }
        ).`when`(voiceUpdater).connectivityChanges()

        voiceUpdater.update()

        verify(downloadVoicesService).selectRemoteVoiceById("uk_male")
    }

    @After
    fun release() {
        localeUtil.close()
    }

    companion object {

        private val DEFAULT_VOICE_BUILDER = VoiceMetadata(
            title = "",
            url = "",
            version = "1.0",
            type = VoiceMetadata.VoiceType.REMOTE,
            locale = "",
            remoteId = "",
            path = "sounds/",
            sampleUrl = null,
        )

        private val REMOTE_VOICES = listOf(
            DEFAULT_VOICE_BUILDER.copy(locale = "uk", remoteId = "uk_male"),
            DEFAULT_VOICE_BUILDER.copy(locale = "tr", remoteId = "tr_male"),
            DEFAULT_VOICE_BUILDER.copy(locale = "ru", remoteId = "ru_male"),
        )

        private val UK_MALE_LOCAL_SELECTED_VOICE_METADATA = DEFAULT_VOICE_BUILDER.copy(
            locale = "uk",
            remoteId = "uk_male",
            selected = true,
            type = VoiceMetadata.VoiceType.ASSET
        )

        private val UK_MALE_LOCAL_NOT_SELECTED_VOICE_METADATA = UK_MALE_LOCAL_SELECTED_VOICE_METADATA.withSelected(false)
    }
}
