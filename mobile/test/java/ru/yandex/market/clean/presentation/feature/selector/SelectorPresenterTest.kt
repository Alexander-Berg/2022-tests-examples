package ru.yandex.market.clean.presentation.feature.selector

import io.reactivex.Maybe
import io.reactivex.Single
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.facades.SelectorAnalytics
import ru.yandex.market.clean.domain.model.categoryTestInstance
import ru.yandex.market.clean.domain.model.selector.SelectorSource
import ru.yandex.market.clean.domain.model.selector.response.ImagePost
import ru.yandex.market.clean.domain.model.selector.response.SelectorConfig
import ru.yandex.market.clean.domain.model.selector.response.SelectorEntryPoints
import ru.yandex.market.clean.domain.model.selector.collection.SelectorNode
import ru.yandex.market.clean.domain.model.selector.collection.SelectorList
import ru.yandex.market.domain.media.model.EmptyImageReference
import ru.yandex.market.presentationSchedulersMock

/*
Для всех тестов где используется рефлексия она действительно необходима, потому что необходимо протестировать
логику / аналитику в зависимости от разных текущих состояний коллекции подборщика. Перемещаться по ней в тестах,
а заодно и тестировать - является проблемачтиным. Я не нашел другого выхода как использовать ее, либо делать
текущее состояние в презентере публичным, что нарушает принцип инкапсуляции.
 */
class SelectorPresenterTest {

    //Mock data
    private val post = ImagePost("", EmptyImageReference())
    private val endNode = SelectorNode.EndNode(
        finishPost = post,
        retryButtonText = "",
        discardButtonText = ""
    )
    private val readyNode = SelectorNode.ReadyNode(
        next = endNode,
        successPost = post,
        navigateHid = NEW_CATEGORY_ID,
    )
    private val pendingNode = SelectorNode.PendingNode(
        next = mock(),
        pendingPost = post
    )
    private val flowNode = SelectorNode.FlowNode(
        chips = emptyList(),
        label = LABEL,
        next = pendingNode
    )
    private val startNode = SelectorNode.StartNode(
        label = "",
        negativeButtonText = "",
        next = flowNode,
        picture = EmptyImageReference(),
        positiveButtonText = ""
    )

    private val useCasesMock = mock<SelectorUseCases> {
        on { getStates(any(), any()) } doReturn Maybe.just(
            SelectorConfig(
                SelectorEntryPoints.default(),
                SelectorList(startNode, pendingNode),
                CMS_PAGE_ID
            )
        )
        on { getSearchResultCount(any(), any()) } doReturn Single.just(SEARCH_COUNT)
    }

    private val analyticsMock = mock<SelectorAnalytics>()

    private val spyPresenter = spy(
        SelectorPresenter(
            formatter = mock(),
            schedulers = presentationSchedulersMock(),
            useCases = useCasesMock,
            analytics = analyticsMock
        )
    )

    private val selectorStateField = SelectorPresenter::class.java.getDeclaredField("state").apply {
        isAccessible = true
    }

    @Test
    fun `test update category with different category empty`() {
        val category = categoryTestInstance(id = CATEGORY_ID, nid = CATEGORY_NID)

        spyPresenter.updateCategory(category, SelectorSource.ALL_FILTERS)

        verify(useCasesMock).getStates(category, SelectorSource.ALL_FILTERS)
        verify(spyPresenter).showState()
    }

    /*
    Конкретно в этом тесте рефлексия нужна для того, чтобы задать полю значение до самого теста, как будто оно там уже
    было. Опять же кроме рефлексии решения не нашел, ну, либо опять нарушать инкапсуляцию
     */
    @Test
    fun `test update category with same category not empty`() {
        val category = categoryTestInstance(id = CATEGORY_ID, nid = CATEGORY_NID)
        val categoryField = spyPresenter::class.java.getDeclaredField("currentCategory").apply {
            isAccessible = true
        }
        categoryField.set(spyPresenter, category)

        spyPresenter.updateCategory(category, SelectorSource.ALL_FILTERS)

        verify(useCasesMock, never()).getStates(category, SelectorSource.ALL_FILTERS)
        verify(spyPresenter).showState()
    }

    @Test
    fun `test update category with different category not empty`() {
        val category = categoryTestInstance(id = NEW_CATEGORY_ID, nid = CATEGORY_NID)

        spyPresenter.updateCategory(category, SelectorSource.ALL_FILTERS)

        verify(useCasesMock).getStates(category, SelectorSource.ALL_FILTERS)
        verify(spyPresenter).showState()
    }

    @Test
    fun `test update category error`() {
        val category = categoryTestInstance(id = CATEGORY_ID, nid = CATEGORY_NID)

        whenever(useCasesMock.getStates(category, SelectorSource.ALL_FILTERS)) doReturn Maybe.error(Throwable())
        spyPresenter.updateCategory(category, SelectorSource.ALL_FILTERS)

        verify(spyPresenter).showState()
    }

    @Test
    fun `test on negative button clicked end node`() {

        `update selector with default category, change current state and invoke function`(
            nodeToReplaceWith = endNode,
            selectorFunction = SelectorPresenter::onNegativeButtonClicked
        )

        verify(analyticsMock).reportResultsDrop(
            SelectorAnalytics.Params(
                source = SelectorSource.ALL_FILTERS,
                hid = CATEGORY_ID,
                nid = CATEGORY_NID,
                pageId = CMS_PAGE_ID
            )
        )
    }

    @Test
    fun `test on negative button clicked`() {

        `update selector with default category, change current state and invoke function`(
            nodeToReplaceWith = startNode,
            selectorFunction = SelectorPresenter::onNegativeButtonClicked
        )

        val state = selectorStateField.get(spyPresenter)
        Assertions.assertThat(state == null)
    }

    @Test
    fun `test show state start node`() {
        spyPresenter.updateCategory(
            categoryTestInstance(id = CATEGORY_ID, nid = CATEGORY_NID), SelectorSource.ALL_FILTERS
        )

        verify(analyticsMock).reportVisible(
            SelectorAnalytics.Params(
                source = SelectorSource.ALL_FILTERS,
                hid = CATEGORY_ID,
                nid = CATEGORY_NID,
                pageId = CMS_PAGE_ID
            )
        )
    }

    @Test
    fun `test show state end node`() {

        `update selector with default category, change current state and invoke function`(
            nodeToReplaceWith = endNode,
            selectorFunction = SelectorPresenter::showState
        )

        verify(analyticsMock).reportResultsVisible(
            SelectorAnalytics.Params(
                source = SelectorSource.ALL_FILTERS,
                hid = CATEGORY_ID,
                nid = CATEGORY_NID,
                pageId = CMS_PAGE_ID
            )
        )
    }

    @Test
    fun `test show state flow node`() {

        `update selector with default category, change current state and invoke function`(
            nodeToReplaceWith = flowNode,
            selectorFunction = SelectorPresenter::showState
        )

        verify(analyticsMock) {
            1 * {
                reportVisible(any())
                reportQuestionVisible(
                    SelectorAnalytics.Params(
                        source = SelectorSource.ALL_FILTERS,
                        hid = CATEGORY_ID,
                        nid = CATEGORY_NID,
                        pageId = CMS_PAGE_ID,
                        question = LABEL
                    )
                )
            }
        }
    }

    @Test
    fun `test confirm button click ready node`() {

        `update selector with default category, change current state and invoke function`(
            nodeToReplaceWith = readyNode,
            SelectorPresenter::onConfirmButtonClicked
        )

        verify(analyticsMock).reportResultsNavigate(
            SelectorAnalytics.Params(
                source = SelectorSource.ALL_FILTERS,
                hid = CATEGORY_ID,
                nid = CATEGORY_NID,
                pageId = CMS_PAGE_ID
            )
        )
    }

    @Test
    fun `test confirm button click end node`() {

        `update selector with default category, change current state and invoke function`(
            nodeToReplaceWith = endNode,
            selectorFunction = SelectorPresenter::onConfirmButtonClicked
        )

        verify(analyticsMock).reportRepeat(
            SelectorAnalytics.Params(
                source = SelectorSource.ALL_FILTERS,
                hid = CATEGORY_ID,
                nid = CATEGORY_NID,
                pageId = CMS_PAGE_ID
            )
        )
    }

    @Test
    fun `test confirm button click flow node`() {

        `update selector with default category, change current state and invoke function`(
            nodeToReplaceWith = flowNode,
            selectorFunction = SelectorPresenter::onConfirmButtonClicked
        )

        verify(analyticsMock).reportQuestionAnswerNavigate(
            SelectorAnalytics.Params(
                source = SelectorSource.ALL_FILTERS,
                hid = CATEGORY_ID,
                nid = CATEGORY_NID,
                pageId = CMS_PAGE_ID,
                question = LABEL,
                answers = emptyList()
            )
        )
    }

    @Test
    fun `test confirm button click start node`() {

        `update selector with default category, change current state and invoke function`(
            nodeToReplaceWith = startNode,
            selectorFunction = SelectorPresenter::onConfirmButtonClicked
        )

        verify(analyticsMock).reportStartButtonNavigate(
            SelectorAnalytics.Params(
                source = SelectorSource.ALL_FILTERS,
                hid = CATEGORY_ID,
                nid = CATEGORY_NID,
                pageId = CMS_PAGE_ID
            )
        )
    }

    private fun `update selector with default category, change current state and invoke function`(
        nodeToReplaceWith: SelectorNode,
        selectorFunction: SelectorPresenter.() -> Unit
    ) {
        with(spyPresenter) {
            updateCategory(categoryTestInstance(id = CATEGORY_ID, nid = CATEGORY_NID), SelectorSource.ALL_FILTERS)
            selectorStateField.set(this, nodeToReplaceWith)
            selectorFunction()
        }
    }

    private companion object {
        const val CATEGORY_ID = "100"
        const val CATEGORY_NID = "9999"
        const val CMS_PAGE_ID = 1111L
        const val NEW_CATEGORY_ID = "200"
        const val SEARCH_COUNT = 42
        const val LABEL = "some label"
    }
}
