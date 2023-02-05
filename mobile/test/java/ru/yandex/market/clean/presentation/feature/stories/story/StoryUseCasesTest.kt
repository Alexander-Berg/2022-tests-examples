package ru.yandex.market.clean.presentation.feature.stories.story

import dagger.Lazy
import io.reactivex.Completable
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.domain.usecase.stories.SetStoryShownUseCase
import ru.yandex.market.clean.domain.usecase.wishlist.AddWishItemUseCase
import ru.yandex.market.clean.domain.usecase.wishlist.DeleteWishItemUseCase
import ru.yandex.market.clean.domain.usecase.wishlist.GetWishItemUseCase
import ru.yandex.market.data.deeplinks.params.resolver.MapUrlToDeeplinkUseCase

class StoryUseCasesTest {

    private val setStoryShownUseCaseMock = mock<SetStoryShownUseCase> {
        on { execute(DUMMY_ID) } doReturn Completable.complete()
    }
    private val lazySetStoryShownUseCase = mock<Lazy<SetStoryShownUseCase>> {
        on { get() } doReturn setStoryShownUseCaseMock
    }

    private val getWishItemUseCaseMock = mock<GetWishItemUseCase>()

    private val lazyGetWishItemUseCaseMock = mock<Lazy<GetWishItemUseCase>> {
        on { get() } doReturn getWishItemUseCaseMock
    }

    private val addWishItemUseCase = mock<AddWishItemUseCase>()

    private val lazyAddWishItemUseCase = mock<Lazy<AddWishItemUseCase>> {
        on { get() } doReturn addWishItemUseCase
    }

    private val deleteWishItemUseCase = mock<DeleteWishItemUseCase>()

    private val lazyDeleteWishItemUseCase = mock<Lazy<DeleteWishItemUseCase>> {
        on { get() } doReturn deleteWishItemUseCase
    }

    private val deeplinkUseCase = mock<MapUrlToDeeplinkUseCase>()

    private val lazyDeeplinkUseCasesTest = mock<Lazy<MapUrlToDeeplinkUseCase>> {
        on { get() } doReturn deeplinkUseCase
    }
    private val useCases = StoryUseCases(
        lazySetStoryShownUseCase,
        lazyGetWishItemUseCaseMock,
        lazyAddWishItemUseCase,
        lazyDeleteWishItemUseCase,
        lazyDeeplinkUseCasesTest,
    )

    @Test
    fun `Should execute use case with success`() {
        useCases.setStoryShown(DUMMY_ID)
            .test()
            .assertComplete()
    }

    private companion object {
        const val DUMMY_ID = "DUMMY_ID"
    }
}