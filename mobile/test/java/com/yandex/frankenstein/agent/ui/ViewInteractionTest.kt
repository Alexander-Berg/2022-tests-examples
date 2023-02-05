package com.yandex.frankenstein.agent.ui

import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.ViewInteraction
import junit.framework.AssertionFailedError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class ViewInteractionTest {

    private val viewInteraction = mock(ViewInteraction::class.java)

    @Test
    fun testClick() {
        viewInteraction.click()

        verify(viewInteraction).perform(any(GeneralClickAction::class.java))
    }

    @Test
    fun testIsVisible() {
        val visible = viewInteraction.isVisible()

        assertThat(visible).isTrue()
        verify(viewInteraction).check(any(ViewAssertion::class.java))
    }

    @Test
    fun testIsVisibleIfNoMatchingViewException() {
        `when`(viewInteraction.check(any())).thenThrow(NoMatchingViewException::class.java)
        val visible = viewInteraction.isVisible()

        assertThat(visible).isFalse()
        verify(viewInteraction).check(any(ViewAssertion::class.java))
    }

    @Test
    fun testIsVisibleIfAssertionFailedError() {
        `when`(viewInteraction.check(any())).thenThrow(AssertionFailedError::class.java)
        val visible = viewInteraction.isVisible()

        assertThat(visible).isFalse()
        verify(viewInteraction).check(any(ViewAssertion::class.java))
    }
}
