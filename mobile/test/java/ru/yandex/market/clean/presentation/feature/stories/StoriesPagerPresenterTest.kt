package ru.yandex.market.clean.presentation.feature.stories

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.domain.model.cms.StoryModel
import ru.yandex.market.clean.domain.model.cms.pageParamsModelTestInstance
import ru.yandex.market.clean.presentation.feature.stories.flow.StoriesArguments
import ru.yandex.market.clean.presentation.feature.stories.storiesPager.StoriesPagerPresenter
import ru.yandex.market.clean.presentation.feature.stories.storiesPager.StoriesPagerUseCases
import ru.yandex.market.clean.presentation.feature.stories.storiesPager.StoriesPagerView
import ru.yandex.market.clean.presentation.feature.stories.vo.StoriesVoFormatter
import ru.yandex.market.clean.presentation.feature.stories.vo.StoryVo
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.parcelable.stories.storiesPageParamsTestInstance
import ru.yandex.market.presentationSchedulersMock

class StoriesPagerPresenterTest {

    private val pageParamsModel = pageParamsModelTestInstance()
    private val pageParams = storiesPageParamsTestInstance()
    private val cmsStories = mock<List<StoryModel>>()
    private val storyVo = mock<StoryVo>()
    private val storiesVo = listOf(storyVo)
    private val schedulers = presentationSchedulersMock()
    private val router = mock<Router>()
    private val useCases = mock<StoriesPagerUseCases> {
        on { getStories(PAGE_ID, pageParamsModel) } doReturn Single.just(cmsStories)
    }
    private val storiesArguments = mock<StoriesArguments> {
        on { pageId } doReturn PAGE_ID
        on { defaultSelectedStoryId } doReturn STORY_ID
        on { params } doReturn pageParams
    }
    private val storiesVoFormatter = mock<StoriesVoFormatter> {
        on { map(cmsStories) } doReturn storiesVo
    }

    private val view = mock<StoriesPagerView>()

    private val presenter = StoriesPagerPresenter(
        schedulers = schedulers,
        router = router,
        useCases = useCases,
        storiesArguments = storiesArguments,
        storiesVoFormatter = storiesVoFormatter
    )

    @Test
    fun `Should show stories when view first attached`() {
        presenter.attachView(view)

        verify(view).showStories(storiesVo, 0)
    }

    @Test
    fun `Should invoke router back when back clicked `() {
        presenter.back()
        verify(router).back()
    }

    companion object {
        const val PAGE_ID = "PAGE_ID"
        const val STORY_ID = "STORY_ID"
    }
}