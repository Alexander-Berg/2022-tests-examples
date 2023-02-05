package com.yandex.frankenstein.agent.ui

import androidx.test.espresso.IdlingResource
import androidx.test.espresso.ViewFinder
import android.view.View
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matcher
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class IsDisplayedIdlingResourceTest {

    @Mock private lateinit var view: View
    @Mock private lateinit var viewFinder: ViewFinder
    @Mock private lateinit var matcher: Matcher<View>
    @Mock private lateinit var resourceCallback: IdlingResource.ResourceCallback

    private val isDisplayedIdlingResource: IsDisplayedIdlingResource

    init {
        MockitoAnnotations.initMocks(this)

        `when`(viewFinder.view).thenReturn(view)
        `when`(view.getGlobalVisibleRect(any())).thenReturn(true)
        `when`(view.parent).thenReturn(null)

        isDisplayedIdlingResource = IsDisplayedIdlingResource(viewFinder, matcher)
        isDisplayedIdlingResource.registerIdleTransitionCallback(resourceCallback)
    }

    @Test
    fun testIsIdleNow() {
        `when`(view.visibility).thenReturn(View.VISIBLE)

        assertThat(isDisplayedIdlingResource.isIdleNow).isTrue()
        verify(resourceCallback).onTransitionToIdle()
    }

    @Test
    fun testIsIdleNowIfNotVisible() {
        `when`(view.visibility).thenReturn(View.GONE)

        assertThat(isDisplayedIdlingResource.isIdleNow).isFalse()
        verify(resourceCallback, never()).onTransitionToIdle()
    }
}
