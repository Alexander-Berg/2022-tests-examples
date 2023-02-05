package ru.yandex.market.clean.data.source.cms

import com.annimon.stream.Optional
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.fapi.source.stories.StoriesFapiClient
import ru.yandex.market.clean.data.mapper.cms.CmsLiveStreamMapper
import ru.yandex.market.clean.data.mapper.cms.CmsStoriesMapper
import ru.yandex.market.clean.data.model.dto.cms.CmsLiveStoriesMergedDto
import ru.yandex.market.clean.data.model.dto.cms.stories.CmsStoryMergedDto
import ru.yandex.market.clean.data.model.dto.stories.StoriesRequest
import ru.yandex.market.clean.data.repository.DeviceInfoRepository
import ru.yandex.market.clean.domain.model.cms.PageParamsModel
import ru.yandex.market.clean.domain.model.cms.StoryModel
import ru.yandex.market.clean.domain.model.cms.pageParamsModelTestInstance
import ru.yandex.market.clean.domain.usecase.GetOfferConfigUseCase
import ru.yandex.market.common.featureconfigs.managers.LiveStreamPreviewToggleManager
import ru.yandex.market.common.featureconfigs.managers.StoriesToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.models.OfferMapperConfig
import ru.yandex.market.common.schedulers.NetworkingScheduler
import ru.yandex.market.data.stories.datastore.StoriesViewsDataStore
import ru.yandex.market.domain.auth.model.DeviceId
import ru.yandex.market.domain.auth.repository.DeviceIdRepository

class CmsStoriesRepositoryTest {

    private val apiDataSource = mock<StoriesFapiClient>()
    private val workerScheduler = NetworkingScheduler(Schedulers.trampoline())
    private val cmsLiveStreamMapper = mock<CmsLiveStreamMapper>()

    private val cmsStoriesDataStore = mock<CmsStoriesDataStore> {
        on { saveStories(any(), any()) } doReturn Completable.complete()
    }

    private val storiesViewsDataStore = mock<StoriesViewsDataStore> {
        on { getShownStories() } doReturn Single.just(EMPTY_SHOWN_STORIES)
    }

    private val liveStreamPreviewToggleManager = mock<LiveStreamPreviewToggleManager> {
        on { getSingle() } doReturn Single.just(FeatureToggle(false))
    }

    private val storiesToggleManager = mock<StoriesToggleManager> {
        on { getSingle() } doReturn Single.just(FeatureToggle(true))
    }

    private val deviceIdRepository = mock<DeviceIdRepository> {
        on { getDeviceId() } doReturn Single.just(Optional.of(DeviceId(DUMMY_DEVICE_ID)))
    }

    private val deviceInfoRepository = mock<DeviceInfoRepository> {
        on { getGoogleAdId() } doReturn Single.just(Optional.of(DUMMY_GAID))
    }

    private val offerMapperConfig = OfferMapperConfig(null, false)
    private val getOfferConfigUseCase = mock<GetOfferConfigUseCase> {
        on { execute() } doReturn Single.just(offerMapperConfig)
    }

    //remote
    private val remoteStory = mock<StoryModel>()
    private val storiesResponseDto = mock<List<CmsStoryMergedDto>>()
    private val remoteCmsStories: List<StoryModel> = listOf(remoteStory)

    private val response = mock<CmsLiveStoriesMergedDto> {
        on { stories } doReturn storiesResponseDto
    }

    //cache
    private val localStory = mock<StoryModel>()
    private val cachedCmsStories = listOf(localStory)

    private val pageParams = pageParamsModelTestInstance()
        .copy(getDataStrategy = PageParamsModel.RequestStrategy.SET)

    private val cmsStoriesMapper = mock<CmsStoriesMapper> {
        on {
            map(
                cmsStoriesDtos = storiesResponseDto,
                shownStoryIds = EMPTY_SHOWN_STORIES,
                pageId = DUMMY_PAGE_ID,
                pageParamsModel = pageParams.copy(
                    withLiveTranslations = false,
                    supportRanking = null,
                    navigationUrl = null
                ),
                offerMapperConfig,
            )
        } doReturn remoteCmsStories
    }

    private val repository = CmsStoriesRepository(
        apiDataSource = apiDataSource,
        workerScheduler = workerScheduler,
        cmsStoriesMapper = cmsStoriesMapper,
        liveStreamPreviewToggleManager = liveStreamPreviewToggleManager,
        storiesToggleManager = storiesToggleManager,
        cmsStoriesDataStore = cmsStoriesDataStore,
        cmsLiveStreamMapper = cmsLiveStreamMapper,
        deviceIdRepository = deviceIdRepository,
        deviceInfoRepository = deviceInfoRepository,
        storiesViewsDataStore = storiesViewsDataStore,
        getOfferConfigUseCase = getOfferConfigUseCase,
    )


    @Test
    fun `Should load stories from cached when cache is not empty and use cache at first is true`() {
        val pageParams = pageParams.copy(
            withLiveTranslations = false,
            supportRanking = false,
            navigationUrl = null
        )
        whenever(cmsStoriesDataStore.getStories(DUMMY_PAGE_ID, pageParams)).thenReturn(Single.just(cachedCmsStories))

        repository.getStoriesByHostPage(
            pageId = DUMMY_PAGE_ID,
            useCacheAtFirst = true,
            pageParams = pageParams
        )
            .test()
            .assertNever(remoteCmsStories)
            .assertValue(cachedCmsStories)
    }

    @Test
    fun `Should load remote stories when cache is not empty and use cache at first is false`() {
        whenever(
            apiDataSource.getStories(
                StoriesRequest(
                    pageId = DUMMY_PAGE_ID.toLong(),
                    isLive = false,
                    preview = false,
                    isRanking = null,
                    gaid = null,
                    deviceId = null
                )
            )
        ).thenReturn(Single.just(response))

        repository.getStoriesByHostPage(
            pageId = DUMMY_PAGE_ID,
            useCacheAtFirst = false,
            pageParams = pageParams.copy(
                withLiveTranslations = false,
                supportRanking = null,
                navigationUrl = null
            )
        )
            .test()
            .assertNever(cachedCmsStories)
            .assertValue(remoteCmsStories)
    }

    @Test
    fun `Should load remote stories when tries load cached stories and cache is empty`() {
        val pageParams = pageParams.copy(
            withLiveTranslations = false,
            supportRanking = null,
            navigationUrl = null
        )
        whenever(cmsStoriesDataStore.getStories(DUMMY_PAGE_ID, pageParams)).thenReturn(Single.just(emptyList()))
        whenever(
            apiDataSource.getStories(
                StoriesRequest(
                    pageId = DUMMY_PAGE_ID.toLong(),
                    isLive = false,
                    preview = false,
                    isRanking = null,
                    gaid = null,
                    deviceId = null
                )
            )
        ).thenReturn(Single.just(response))

        repository.getStoriesByHostPage(
            pageId = DUMMY_PAGE_ID,
            useCacheAtFirst = true,
            pageParams = pageParams
        )
            .test()
            .assertNever(cachedCmsStories)
            .assertValue(remoteCmsStories)
    }

    @Test
    fun `Should return empty list if stories toggle is disabled`() {
        whenever(
            apiDataSource.getStories(
                StoriesRequest(
                    pageId = DUMMY_PAGE_ID.toLong(),
                    isLive = false,
                    preview = false,
                    isRanking = null,
                    gaid = null,
                    deviceId = null
                )
            )
        ).thenReturn(Single.just(response))
        whenever(storiesToggleManager.getSingle()).thenReturn(Single.just(FeatureToggle(false)))

        repository.getStoriesByHostPage(
            pageId = DUMMY_PAGE_ID,
            useCacheAtFirst = false,
            pageParams = pageParams.copy(
                withLiveTranslations = false,
                supportRanking = null,
                navigationUrl = null
            )
        )
            .test()
            .assertNever(cachedCmsStories)
            .assertNever(remoteCmsStories)
            .assertValue(emptyList())
    }

    @Test
    fun `Should send gaid and deviceId if ranking is true`() {
        whenever(
            cmsStoriesMapper.map(
                cmsStoriesDtos = storiesResponseDto,
                shownStoryIds = EMPTY_SHOWN_STORIES,
                pageId = DUMMY_PAGE_ID,
                pageParamsModel = pageParams.copy(
                    withLiveTranslations = false,
                    supportRanking = true,
                    navigationUrl = null
                ),
                offerMapperConfig,
            )
        ).thenReturn(remoteCmsStories)
        whenever(
            apiDataSource.getStories(
                StoriesRequest(
                    pageId = DUMMY_PAGE_ID.toLong(),
                    isLive = false,
                    preview = false,
                    isRanking = true,
                    gaid = DUMMY_GAID,
                    deviceId = DUMMY_DEVICE_ID
                )
            )
        ).thenReturn(Single.just(response))

        repository.getStoriesByHostPage(
            pageId = DUMMY_PAGE_ID,
            useCacheAtFirst = false,
            pageParams = pageParams.copy(
                withLiveTranslations = false,
                supportRanking = true,
                navigationUrl = null
            )
        )
            .test()
            .assertComplete()
    }

    @Test
    fun `Should not send gaid and deviceId if ranking is false`() {
        whenever(
            cmsStoriesMapper.map(
                cmsStoriesDtos = storiesResponseDto,
                shownStoryIds = EMPTY_SHOWN_STORIES,
                pageId = DUMMY_PAGE_ID,
                pageParamsModel = pageParams.copy(
                    withLiveTranslations = false,
                    supportRanking = false,
                    navigationUrl = null
                ),
                offerMapperConfig,
            )
        ).thenReturn(remoteCmsStories)
        whenever(
            apiDataSource.getStories(
                StoriesRequest(
                    pageId = DUMMY_PAGE_ID.toLong(),
                    isLive = false,
                    preview = false,
                    isRanking = false,
                    gaid = null,
                    deviceId = null
                )
            )
        ).thenReturn(Single.just(response))

        repository.getStoriesByHostPage(
            pageId = DUMMY_PAGE_ID,
            useCacheAtFirst = false,
            pageParams = pageParams.copy(
                withLiveTranslations = false,
                supportRanking = false,
                navigationUrl = null
            )
        )
            .test()
            .assertComplete()
    }

    @Test
    fun `Should return shown stories ids`() {
        val given = mapOf(DUMMY_STORY_ID to DUMMY_STORY_READ_TIME)
        whenever(storiesViewsDataStore.getShownStories()).thenReturn(Single.just(given))
        storiesViewsDataStore.getShownStories()
            .test()
            .assertValue(given)
    }

    @Test
    fun `Should set story as shown`() {
        whenever(repository.setStoryShown(DUMMY_STORY_ID)).thenReturn(Completable.complete())
        repository.setStoryShown(DUMMY_STORY_ID)
            .test()
            .assertComplete()
    }

    private companion object {
        const val DUMMY_PAGE_ID = "0"
        const val DUMMY_STORY_ID = "DUMMY_STORY_ID"
        const val DUMMY_STORY_READ_TIME = 0L
        val EMPTY_SHOWN_STORIES = emptyMap<String, Long>()
        const val DUMMY_GAID = "DUMMY_GAID"
        const val DUMMY_DEVICE_ID = "DUMMY_DEVICEID"
    }
}
