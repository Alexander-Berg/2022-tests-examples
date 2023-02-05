package ru.yandex.market.clean.domain.usecase.filters

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.data.repository.search.SearchFiltersRepository
import ru.yandex.market.clean.domain.model.categoryTestInstance
import ru.yandex.market.clean.domain.usecase.search.model.CpaType
import ru.yandex.market.clean.domain.usecase.search.model.SearchParams
import ru.yandex.market.mockResult
import ru.yandex.market.net.category.FiltersResponse

class GetFiltersUseCaseTest {

    private val searchFiltersRepository = mock<SearchFiltersRepository>()
    private val useCase = GetFiltersUseCase(searchFiltersRepository)

    @Test
    fun `check getting filters by categories`() {
        val params = DEFAULT_PARAMS_BUILDER.category(CATEGORY).build()

        searchFiltersRepository.getCategoryFilters(params).mockResult(MOCKED_RESULT)
        searchFiltersRepository.getSearchFilters(params).mockResult(MOCKED_RESULT)

        useCase.execute(params)
            .test()
            .assertNoErrors()
            .assertResult(RESPONSE_FILTER)

        verify(searchFiltersRepository).getCategoryFilters(params)
    }

    @Test
    fun `check getting filters by search`() {
        val params = DEFAULT_PARAMS_BUILDER.category(null).build()

        searchFiltersRepository.getCategoryFilters(params).mockResult(MOCKED_RESULT)
        searchFiltersRepository.getSearchFilters(params).mockResult(MOCKED_RESULT)

        useCase.execute(params)
            .test()
            .assertNoErrors()
            .assertResult(RESPONSE_FILTER)

        verify(searchFiltersRepository).getSearchFilters(params)
    }

    private companion object {

        val CATEGORY = categoryTestInstance()
        val RESPONSE_FILTER = FiltersResponse()
        val MOCKED_RESULT = Single.just(RESPONSE_FILTER)
        val DEFAULT_PARAMS_BUILDER = SearchParams.builder()
            .cpaType(CpaType.REAL)
            .checkSpellingEnabled(true)
            .isAfterRedirect(true)
            .onStock(true)
    }
}