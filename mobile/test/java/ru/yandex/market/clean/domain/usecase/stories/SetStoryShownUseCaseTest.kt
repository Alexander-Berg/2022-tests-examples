package ru.yandex.market.clean.domain.usecase.stories

import io.reactivex.Completable
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.source.cms.CmsStoriesRepository

class SetStoryShownUseCaseTest {
    private val repository = mock<CmsStoriesRepository>()
    private val useCase = SetStoryShownUseCase(repository)

    @Test
    fun `Should set story shown`() {
        whenever(repository.setStoryShown(DUMMY_ID)).thenReturn(Completable.complete())
        useCase.execute(DUMMY_ID)
            .test()
            .assertComplete()
    }

    private companion object {
        const val DUMMY_ID = "DUMMY_ID"
    }
}