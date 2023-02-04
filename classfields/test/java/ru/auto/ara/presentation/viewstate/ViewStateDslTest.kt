package ru.auto.ara.presentation.viewstate

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import ru.auto.ara.presentation.view.BaseView

/**
 * @author themishkun on 24/07/2018.
 */
@RunWith(AllureRunner::class) class ViewStateDslTest {

    private interface TestView : BaseView {
        fun test(parameter: String)

        fun oneTest(parameter: String)
    }

    private class TestViewState : BaseViewState<TestView>(), TestView{
        override fun test(parameter: String) {
            state[TestView::test](parameter)
        }

        override fun oneTest(parameter: String) {
            oneShot[TestView::oneTest](parameter)
        }

    }

    private val PARAMETER = "PARAMETER"
    private val PARAMETER2 = "PARAMETER2"

    private val view1 : TestView = mock()

    private val view2 : TestView = mock()

    @Test
    fun `given view and set state it should call it`() {
        val viewState = TestViewState()

        with(viewState) {
            bindView(view1)
            test(PARAMETER)
            test(PARAMETER2)
        }

        verify(view1).test(eq(PARAMETER))
        verify(view1).test(eq(PARAMETER2))
    }

    @Test
    fun `given view and set state twice it should restore it once with the latest parameter`() {
        val viewState = TestViewState()

        with(viewState) {
            bindView(view1)
            test(PARAMETER)
            test(PARAMETER2)

            bindView(view2)
            restore()
        }

        verify(view2, times(1)).test(any())
        verify(view2).test(eq(PARAMETER2))
    }

    @Test
    fun `fired oneShot in absence of view it should restore it`() {
        val viewState = TestViewState()

        with(viewState){
            oneTest(PARAMETER)
            bindView(view1)
            restore()
        }

        verify(view1).oneTest(eq(PARAMETER))
    }

    @Test
    fun `given view and fired oneShot it should fire and not restore it`() {
        val viewState = TestViewState()

        with(viewState) {
            bindView(view1)
            oneTest(PARAMETER)

            bindView(view2)
            restore()
        }

        verify(view1).oneTest(eq(PARAMETER))
        verifyZeroInteractions(view2)
    }

    @Test
    fun `fired oneShot twice in absence of view it should restore it once with latest parameters`() {
        val viewState = TestViewState()

        with(viewState){
            oneTest(PARAMETER)
            oneTest(PARAMETER2)
            bindView(view1)
            restore()
        }

        verify(view1, times(1)).oneTest(any())
        verify(view1).oneTest(eq(PARAMETER2))
    }
}
