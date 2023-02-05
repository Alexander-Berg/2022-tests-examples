package ru.yandex.yandexmaps.common.mvp

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown
import org.junit.Before
import org.junit.Test

class BasePresenterTest {
    private interface TestView

    private lateinit var presenter: BasePresenterWithViewValidation<TestView>
    private lateinit var view: TestView

    @Before
    fun beforeEachTest() {
        view = object : TestView {}
        presenter = object : BasePresenterWithViewValidation<TestView>() {}
    }

    @Test
    fun bindView_shouldAttachViewToThePresenter() {
        presenter.bind(view)
        assertThat(presenter.view()).isSameAs(view)
    }

    @Test
    fun bindView_shouldThrowIfPreviousViewIsNotUnbounded() {
        presenter.bind(view)
        try {
            presenter.bind(createView())
            failBecauseExceptionWasNotThrown(IllegalStateException::class.java)
        } catch (expected: IllegalStateException) {
            assertThat(expected).hasMessage("Previous view is not unbound! previousView = $view")
        }
    }

    @Test
    fun view_shouldThrowByDefault() {
        try {
            presenter.view()
            failBecauseExceptionWasNotThrown(NullPointerException::class.java)
        } catch (expected: IllegalStateException) {
            assertThat(expected).hasMessage("View is null. You should probably bind before accessing view!")
        }
    }

    @Test
    fun view_shouldReturnViewAfterBind() {
        presenter.bind(view)
        assertThat(presenter.view()).isSameAs(view)
    }

    @Test
    fun accessViewAfterUnbind_shouldThrow() {
        presenter.bind(view)
        presenter.unbind(view)
        try {
            presenter.view()
            failBecauseExceptionWasNotThrown(NullPointerException::class.java)
        } catch (expected: IllegalStateException) {
            assertThat(expected).hasMessage("View is null. You should probably bind before accessing view!")
        }
    }

    @Test
    fun unbindView_shouldThrowIfPreviousViewIsNotSameAsExpected() {
        presenter.bind(view)
        val unexpectedView = createView()
        try {
            presenter.unbind(unexpectedView)
            failBecauseExceptionWasNotThrown(IllegalStateException::class.java)
        } catch (expected: IllegalStateException) {
            assertThat(expected).hasMessage("Unexpected view! previousView = $view, view to unbind = $unexpectedView")
        }
    }

    private fun createView(): TestView {
        return object : TestView {}
    }
}
