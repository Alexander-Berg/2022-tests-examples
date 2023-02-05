package ru.yandex.market.clean.domain.usecase.agitation

import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.data.fapi.dto.frontApiMergedShowPlaceDtoTestInstance
import ru.yandex.market.clean.data.fapi.dto.frontApiSearchResultDtoTestInstance
import ru.yandex.market.clean.data.repository.agitation.AnalogsRepository
import ru.yandex.market.clean.domain.model.agitations.AnalogsSkus
import ru.yandex.market.clean.domain.model.sku.detailedSkuTestInstance
import ru.yandex.market.domain.auth.usecase.CredentialsUseCase
import ru.yandex.market.domain.auth.model.credentialsTestInstance
import ru.yandex.market.clean.domain.usecase.sku.GetSkusUseCase
import ru.yandex.market.mockResult

class AnalogUseCaseTest {

    private companion object {

        val FRONT_API_SEARCH_RESULT_MOCK = frontApiSearchResultDtoTestInstance()
        val FRONT_API_MERGED_SHOW_PLACE_MOCK = frontApiMergedShowPlaceDtoTestInstance()
        val SKUS_LIST_MOCK = listOf(detailedSkuTestInstance())

        val RESULT = AnalogsSkus(skus = SKUS_LIST_MOCK, totalAnalogs = 0, pagesCount = 0, pageSize = 20)
    }

    private val analogsRepository = mock<AnalogsRepository>()
    private val getSkusUseCase = mock<GetSkusUseCase>()
    private val credentialsUseCase = mock<CredentialsUseCase>()

    private val useCase = AnalogUseCase(analogsRepository, getSkusUseCase, credentialsUseCase)

    @Before
    fun setUp() {

        getSkusUseCase.execute(any(), any()).mockResult(Single.just(SKUS_LIST_MOCK))
        credentialsUseCase.getCredentials().mockResult(Single.just(credentialsTestInstance()))
    }

    @Test
    fun `check getting skus page 1`() {

        val total = 5
        val page = 1

        val analogs = FRONT_API_SEARCH_RESULT_MOCK.copy(total = total) to listOf(FRONT_API_MERGED_SHOW_PLACE_MOCK)
        analogsRepository.getAnalogs(any(), any(), any(), any(), any(), any()).mockResult(Single.just(analogs))

        useCase.execute("hid", "skuid", page)
            .test()
            .assertNoErrors()
            .assertResult(RESULT.copy(totalAnalogs = total, pagesCount = page))

        verify(credentialsUseCase).getCredentials()
    }

    @Test
    fun `check getting skus page 2`() {

        val total = 40
        val page = 2

        val analogs = FRONT_API_SEARCH_RESULT_MOCK.copy(total = total) to listOf(FRONT_API_MERGED_SHOW_PLACE_MOCK)
        analogsRepository.getAnalogs(any(), any(), any(), any(), any(), any()).mockResult(Single.just(analogs))

        useCase.execute("hid", "skuid", page)
            .test()
            .assertNoErrors()
            .assertResult(RESULT.copy(totalAnalogs = total, pagesCount = page))

        verify(getSkusUseCase).execute(any(), any())
    }
}