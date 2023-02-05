package ru.yandex.disk.settings

import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.yandex.disk.feed.BetterRecord
import ru.yandex.disk.feed.MockCollectionBuilder
import ru.yandex.disk.settings.ServerConstants.CAMERA_ALBUM_PATH_REGEXPS
import ru.yandex.disk.test.TestObjectsFactory
import rx.Observable

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class FetchApplicationSettingsCommandTest {

    private val collectionBuilder = MockCollectionBuilder().also {
        it.addRecord("features")
    }
    private val settingsRecBuilder = collectionBuilder.addRecord("settings")

    private var response: Observable<SettingsDataSyncRecords>? = null

    private val settingsDataSync = mock<ApplicationSettingsDataSyncManager> {
        on { requestSettingsRecords() } doAnswer {
            val observable: Observable<SettingsDataSyncRecords> = if (response != null) {
                response!!
            } else {
                Observable.just(SettingsDataSyncRecords(
                    BetterRecord("test", collectionBuilder.build().getRecord("features")),
                    BetterRecord("test", collectionBuilder.build().getRecord("settings"))))
            }
            observable
        }
    }
    private val applicationSettings =
            TestObjectsFactory.createApplicationSettings(RuntimeEnvironment.application)

    private val command = FetchApplicationSettingsCommand(settingsDataSync, applicationSettings, mock())

    @Test
    fun `should concat multiple patterns`() {
        settingsRecBuilder.setField(CAMERA_ALBUM_PATH_REGEXPS, listOf("/1", "/2"))

        command.execute(FetchApplicationSettingsCommandRequest())

        val regex = applicationSettings.cameraAlbumPathRegex!!.toRegex()

        assertThat("/1".contains(regex), equalTo(true))
        assertThat("/2".contains(regex), equalTo(true))
        assertThat("/3".contains(regex), equalTo(false))
    }

    @Test
    fun `should refuse malformed pattern`() {
        settingsRecBuilder.setField(CAMERA_ALBUM_PATH_REGEXPS, listOf("))"))

        command.execute(FetchApplicationSettingsCommandRequest())

        assertThat(applicationSettings.cameraAlbumPathRegex, nullValue())
    }
}
