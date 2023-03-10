package io.github.kakaocup.kakao.spinner

import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import io.github.kakaocup.kakao.common.assertions.AdapterAssertions
import io.github.kakaocup.kakao.common.matchers.SpinnerAdapterSizeMatcher
import org.hamcrest.CoreMatchers.containsString

/**
 * Provides assertions for Spinner adapter
 */
interface SpinnerAdapterAssertions : AdapterAssertions {
    /**
     * Check size of Spinner
     *
     * @param size expected child count size in Spinner
     */
    fun hasSize(size: Int) {
        throw IllegalStateException(
            "Не используй этот метод, я на него потратил много времени. Если у тебя метод заработает - убери этот exception"
        )
        view.check(ViewAssertions.matches(SpinnerAdapterSizeMatcher(size)))
    }

    /**
     * Check selected text
     *
     * @param text expected selected text in Spinner
     */
    fun hasText(text: String) {
        view.check(ViewAssertions.matches(withSpinnerText(containsString(text))))
    }
}