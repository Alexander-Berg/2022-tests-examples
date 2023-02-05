package ru.yandex.market.clean.data.repository.livestream

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.fapi.dto.livestreams.LiveStreamMergedDto
import ru.yandex.market.clean.data.fapi.dto.white.WhiteFrontApiLiveStreamContentDto
import ru.yandex.market.clean.data.fapi.source.live.LiveStreamsFapiClient
import ru.yandex.market.clean.data.mapper.LiveStreamMapper
import ru.yandex.market.clean.data.mapper.live.ScheduledNotificationMapper
import ru.yandex.market.clean.data.model.dto.live.GetLiveStreamContentRequest
import ru.yandex.market.clean.data.model.dto.live.ScheduledLiveTranslationDto
import ru.yandex.market.clean.data.store.ScheduledLiveTranslationDataStore
import ru.yandex.market.clean.domain.model.livestream.LiveStreamContent
import ru.yandex.market.clean.domain.usecase.GetOfferConfigUseCase
import ru.yandex.market.common.featureconfigs.managers.LiveStreamPreviewToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.models.OfferMapperConfig
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider

class LiveStreamRepositoryTest {

    private val streamContent = mock<LiveStreamContent>()
    private val request = GetLiveStreamContentRequest(DUMMY_SEMANTIC, false)
    private val streamDto = mock<WhiteFrontApiLiveStreamContentDto>()
    private val scheduledStreamDto = mock<ScheduledLiveTranslationDto>()
    private val mergedDto = LiveStreamMergedDto(
        streamContent = streamDto,
        skus = null
    )

    private val liveStreamPreviewToggleManager = mock<LiveStreamPreviewToggleManager> {
        on { get() } doReturn FeatureToggle(false)
    }
    private val apiDataSource = mock<LiveStreamsFapiClient> {
        on { getLiveStreamContent(request) } doReturn Single.just(mergedDto)
    }
    private val liveStreamMapper = mock<LiveStreamMapper>()
    private val featureConfigsProvider = mock<FeatureConfigsProvider> {
        on { liveStreamPreviewToggleManager } doReturn liveStreamPreviewToggleManager
    }
    private val scheduledTranslationMapper = mock<ScheduledNotificationMapper>()
    private val scheduledLiveTranslationDataStore = mock<ScheduledLiveTranslationDataStore> {
        on { saveScheduledTranslationData(scheduledStreamDto) } doReturn Completable.complete()
        on { removeScheduledTranslationData(DUMMY_SEMANTIC) } doReturn Completable.complete()
        on { observeScheduledTranslationData(DUMMY_SEMANTIC) } doReturn Observable.just(true)
        on { removeOutdatedTranslations() } doReturn Completable.complete()
    }

    private val offerMapperConfig = OfferMapperConfig(null, false)
    private val getOfferConfigUseCase: GetOfferConfigUseCase = mock() {
        on { execute() } doReturn Single.just(offerMapperConfig)
    }

    private val repository = LiveStreamRepository(
        apiDataSource = apiDataSource,
        liveStreamMapper = liveStreamMapper,
        featureConfigsProvider = featureConfigsProvider,
        scheduledTranslationMapper = scheduledTranslationMapper,
        scheduledLiveTranslationDataStore = scheduledLiveTranslationDataStore,
        getOfferConfigUseCase = getOfferConfigUseCase,
    )

    @Test
    fun `Should return live stream content with success`() {
        whenever(liveStreamMapper.map(mergedDto, offerMapperConfig)).thenReturn(streamContent)
        repository.getLiveStreamContent(DUMMY_SEMANTIC)
            .test()
            .assertValue(streamContent)
    }

    @Test
    fun `Should return live stream content with error when mapping failed`() {
        whenever(liveStreamMapper.map(mergedDto, offerMapperConfig)).thenReturn(null)
        repository.getLiveStreamContent(DUMMY_SEMANTIC)
            .test()
            .assertError(IllegalArgumentException::class.java)
    }

    @Test
    fun `Should save scheduled translation data`() {
        scheduledLiveTranslationDataStore.saveScheduledTranslationData(scheduledStreamDto)
            .test()
            .assertComplete()
    }

    @Test
    fun `Should remove live stream content with success`() {
        scheduledLiveTranslationDataStore.removeScheduledTranslationData(DUMMY_SEMANTIC)
            .test()
            .assertComplete()
    }

    @Test
    fun `Should return event when subscribed on translation data`() {
        scheduledLiveTranslationDataStore.observeScheduledTranslationData(DUMMY_SEMANTIC)
            .test()
            .assertValue(true)
    }

    @Test
    fun `Should remove all outdated translation data`() {
        scheduledLiveTranslationDataStore.removeOutdatedTranslations()
            .test()
            .assertComplete()
    }

    private companion object {
        const val DUMMY_SEMANTIC = "DUMMY_SEMANTIC"
    }

}
