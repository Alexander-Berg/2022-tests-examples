package ru.yandex.market.clean.domain.usecase.selector

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.data.repository.cms.CmsRepository
import ru.yandex.market.clean.domain.model.Category
import ru.yandex.market.clean.domain.model.selector.SelectorSource
import ru.yandex.market.clean.domain.model.selector.response.ImagePost
import ru.yandex.market.clean.domain.model.selector.response.SelectorConfig
import ru.yandex.market.clean.domain.model.selector.response.SelectorEntryPoints
import ru.yandex.market.clean.domain.model.selector.collection.SelectorNode
import ru.yandex.market.clean.domain.model.selector.collection.SelectorList
import ru.yandex.market.clean.domain.model.selector.request.SelectorContentRequestId
import ru.yandex.market.clean.domain.model.selector.response.SelectorAvailableData
import ru.yandex.market.domain.media.model.EmptyImageReference
import ru.yandex.market.exception.NotFoundException

class GetSelectorStatesUseCaseTest {

    private val cmsRepositoryMock = mock<CmsRepository> {
        on { getAvailableSelectorData() } doReturn Single.just(selectorAvailableData)
        on { getSelectorScenarioCollection(any()) } doReturn Single.just(
            SelectorConfig(
                selectorEntryPoints = selectorEntryPointsStub,
                selectorCollection = selectorListStub,
                cmsPageId = CMS_PAGE_ID
            )
        )
    }

    private val useCase = GetSelectorStatesUseCase(cmsRepositoryMock)

    @Test
    fun `test SEARCH_RESULT_TOP with available hid and nid`() {
        val category = Category.create(id = HID_STUB.toString(), nid = NID_STUB.toString())
        useCase.execute(category, SelectorSource.SEARCH_RESULT_TOP).blockingGet()

        verify(cmsRepositoryMock) {
            1 * {
                getAvailableSelectorData()
                getSelectorScenarioCollection(SelectorContentRequestId.Hid(HID_STUB))
            }
        }
    }

    @Test
    fun `test SEARCH_RESULT_TOP with unavailable hid and available nid`() {
        val category = Category.create(id = HID_ERROR_STUB.toString(), nid = NID_STUB.toString())
        useCase.execute(category, SelectorSource.SEARCH_RESULT_TOP).blockingGet()

        verify(cmsRepositoryMock) {
            1 * {
                getAvailableSelectorData()
                getSelectorScenarioCollection(SelectorContentRequestId.Nid(NID_STUB))
            }
        }
    }

    @Test
    fun `test SEARCH_RESULT_TOP with unavailable hid and nid`() {
        val category = Category.create(id = HID_ERROR_STUB.toString(), nid = NID_ERROR_STUB.toString())

        useCase.execute(category, SelectorSource.SEARCH_RESULT_TOP)
            .test()
            .assertError(NotFoundException::class.java)
    }

    @Test
    fun `test SEARCH_RESULT_EIGHTH with available hid and nid`() {
        val category = Category.create(id = HID_STUB.toString(), nid = NID_STUB.toString())
        useCase.execute(category, SelectorSource.SEARCH_RESULT_EIGHTH).blockingGet()

        verify(cmsRepositoryMock) {
            1 * {
                getAvailableSelectorData()
                getSelectorScenarioCollection(SelectorContentRequestId.Hid(HID_STUB))
            }
        }
    }

    @Test
    fun `test SEARCH_RESULT_EIGHTH with unavailable hid and available nid`() {
        val category = Category.create(id = HID_ERROR_STUB.toString(), nid = NID_STUB.toString())
        useCase.execute(category, SelectorSource.SEARCH_RESULT_EIGHTH).blockingGet()

        verify(cmsRepositoryMock) {
            1 * {
                getAvailableSelectorData()
                getSelectorScenarioCollection(SelectorContentRequestId.Nid(NID_STUB))
            }
        }
    }

    @Test
    fun `test SEARCH_RESULT_EIGHTH with unavailable hid and nid`() {
        val category = Category.create(id = HID_ERROR_STUB.toString(), nid = NID_ERROR_STUB.toString())

        useCase.execute(category, SelectorSource.SEARCH_RESULT_EIGHTH)
            .test()
            .assertError(NotFoundException::class.java)
    }

    @Test
    fun `test ALL_FILTERS with available hid and nid`() {
        val category = Category.create(id = HID_STUB.toString(), nid = NID_STUB.toString())
        useCase.execute(category, SelectorSource.ALL_FILTERS).blockingGet()

        verify(cmsRepositoryMock) {
            1 * {
                getAvailableSelectorData()
                getSelectorScenarioCollection(SelectorContentRequestId.Hid(HID_STUB))
            }
        }
    }

    @Test
    fun `test ALL_FILTERS with unavailable hid and available nid`() {
        val category = Category.create(id = HID_ERROR_STUB.toString(), nid = NID_STUB.toString())
        useCase.execute(category, SelectorSource.ALL_FILTERS).blockingGet()

        verify(cmsRepositoryMock) {
            1 * {
                getAvailableSelectorData()
                getSelectorScenarioCollection(SelectorContentRequestId.Nid(NID_STUB))
            }
        }
    }

    @Test
    fun `test ALL_FILTERS with unavailable hid and nid`() {
        val category = Category.create(id = HID_ERROR_STUB.toString(), nid = NID_ERROR_STUB.toString())

        useCase.execute(category, SelectorSource.ALL_FILTERS)
            .test()
            .assertError(NotFoundException::class.java)
    }

    @Test
    fun `test CATALOG with available hid`() {
        val category = Category.create(id = HID_STUB.toString(), nid = NID_STUB.toString())
        useCase.execute(category, SelectorSource.CATALOG).blockingGet()

        verify(cmsRepositoryMock) {
            1 * {
                getAvailableSelectorData()
                getSelectorScenarioCollection(SelectorContentRequestId.Hid(HID_STUB))
            }
        }
    }

    @Test
    fun `test CATALOG with unavailable hid and available nid`() {
        val category = Category.create(id = HID_ERROR_STUB.toString(), nid = NID_STUB.toString())
        useCase.execute(category, SelectorSource.CATALOG).blockingGet()

        verify(cmsRepositoryMock) {
            1 * {
                getAvailableSelectorData()
                getSelectorScenarioCollection(SelectorContentRequestId.Nid(NID_STUB))
            }
        }
    }

    @Test
    fun `test CATALOG with unavailable hid and nid`() {
        val category = Category.create(id = HID_ERROR_STUB.toString(), nid = NID_ERROR_STUB.toString())

        useCase.execute(category, SelectorSource.CATALOG)
            .test()
            .assertError(NotFoundException::class.java)
    }

    @Test
    fun `test DEPARTMENT with available nid`() {
        val category = Category.create(id = HID_STUB.toString(), nid = NID_STUB.toString())
        useCase.execute(category, SelectorSource.DEPARTMENT).blockingGet()

        verify(cmsRepositoryMock) {
            1 * {
                getAvailableSelectorData()
                getSelectorScenarioCollection(SelectorContentRequestId.Nid(NID_STUB))
            }
        }
    }

    @Test
    fun `test DEPARTMENT with unavailable nid`() {
        val category = Category.create(id = HID_STUB.toString(), nid = NID_ERROR_STUB.toString())

        useCase.execute(category, SelectorSource.DEPARTMENT)
            .test()
            .assertError(NotFoundException::class.java)
    }

    private companion object {
        const val HID_STUB = 1L
        const val NID_STUB = 2L
        const val HID_ERROR_STUB = 3L
        const val NID_ERROR_STUB = 4L
        const val CMS_PAGE_ID = 111L

        val selectorAvailableData = SelectorAvailableData(
            availableHids = listOf(SelectorContentRequestId.Hid(HID_STUB)),
            availableNids = listOf(SelectorContentRequestId.Nid(NID_STUB))
        )

        val postStub = ImagePost(
            image = EmptyImageReference(),
            label = ""
        )
        val selectorListStub = SelectorList(
            startNode = SelectorNode.StartNode(
                label = "",
                negativeButtonText = "",
                next = SelectorNode.FlowNode(
                    chips = emptyList(),
                    label = "",
                    next = null
                ),
                picture = EmptyImageReference(),
                positiveButtonText = ""
            ),
            pendingNode = SelectorNode.PendingNode(
                next = SelectorNode.ReadyNode(
                    next = SelectorNode.EndNode(
                        discardButtonText = "",
                        finishPost = postStub,
                        retryButtonText = ""
                    ),
                    successPost = postStub,
                    navigateHid = ""
                ),
                pendingPost = postStub
            )
        )

        val selectorEntryPointsStub = SelectorEntryPoints.default()
    }
}