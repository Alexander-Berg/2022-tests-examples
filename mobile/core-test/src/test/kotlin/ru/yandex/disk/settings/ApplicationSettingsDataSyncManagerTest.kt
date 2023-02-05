package ru.yandex.disk.settings

import org.hamcrest.Matchers.*
import org.junit.Test
import org.mockito.Mockito.*
import ru.yandex.disk.feed.BetterRecord
import ru.yandex.disk.feed.DiskDataSyncManager
import ru.yandex.disk.feed.MockCollectionBuilder
import ru.yandex.disk.test.TestCase2
import rx.observers.TestSubscriber

private const val FEATURE_GIFT_LINK = "gift_link"

class ApplicationSettingsDataSyncManagerTest : TestCase2() {
    private val mockCollectionBuilder = MockCollectionBuilder()

    private val dataSyncManager = mock(DiskDataSyncManager::class.java)!!.apply {
        `when`(requestRemoteCollection(anyString())).thenAnswer({
            mockCollectionBuilder.buildObservable()
        })
    }

    private val applicationSettingsRemoteRepo = ApplicationSettingsDataSyncManager(dataSyncManager)

    @Test
    fun `should return value from dataSync`() {
        val testSubscriber = TestSubscriber<BetterRecord>()

        mockCollectionBuilder.addRecord(ServerConstants.RECORD_FEATURES)
                .setField(FEATURE_GIFT_LINK, "true")

        applicationSettingsRemoteRepo
                .requestFeatureState()
                .subscribe(testSubscriber)

        assertThat(testSubscriber.onErrorEvents, empty())
        assertThat(testSubscriber.onNextEvents[0].getString(FEATURE_GIFT_LINK), equalTo("true"))
    }

    @Test
    fun `should be ready for empty database`() {
        val testSubscriber = TestSubscriber<SettingsDataSyncRecords>()

        applicationSettingsRemoteRepo
                .requestSettingsRecords()
                .subscribe(testSubscriber)

        assertThat(testSubscriber.onErrorEvents, empty())
        assertThat(testSubscriber.valueCount, equalTo(1))
        assertThat(testSubscriber.onNextEvents[0].features, nullValue())
        assertThat(testSubscriber.onNextEvents[0].settings, nullValue())
    }

}