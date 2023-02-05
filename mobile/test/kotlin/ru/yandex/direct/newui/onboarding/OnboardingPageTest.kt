package ru.yandex.direct.newui.onboarding

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class OnboardingPageTest {

    @Test
    fun featuresHighlight_shouldIncludeCurrentVersionCode() {
        val allPages = mapOf(OnboardingPage.PAYMENT to 1)
        val updatingFrom = 0
        val updatingTo = 1
        val pagesToShow = OnboardingPage.getFeaturesHighlightPages(updatingFrom, updatingTo, allPages)
        assertThat(pagesToShow).isEqualTo(listOf(OnboardingPage.PAYMENT))
    }

    @Test
    fun featuresHighlight_shouldExcludePreviousVersionCode() {
        val allPages = mapOf(OnboardingPage.PAYMENT to 1)
        val updatingFrom = 1
        val updatingTo = 2
        val pagesToShow = OnboardingPage.getFeaturesHighlightPages(updatingFrom, updatingTo, allPages)
        assertThat(pagesToShow).isEqualTo(emptyList<OnboardingPage>())
    }

    @Test
    fun featuresHighlight_shouldWorkWithEmptyMap() {
        val allPages = emptyMap<OnboardingPage, Int>()
        val updatingFrom = 0
        val updatingTo = 1
        val pagesToShow = OnboardingPage.getFeaturesHighlightPages(updatingFrom, updatingTo, allPages)
        assertThat(pagesToShow).isEqualTo(emptyList<OnboardingPage>())
    }

    @Test
    fun featuresHighlight_shouldBeEmpty_ifAppWasNotUpdated() {
        val allPages = mapOf(
            OnboardingPage.PAYMENT to 0,
            OnboardingPage.DAILY_BUDGET to 1,
            OnboardingPage.PRICE_MASTER to 2
        )
        val updatingFrom = 1
        val updatingTo = 1
        val pagesToShow = OnboardingPage.getFeaturesHighlightPages(updatingFrom, updatingTo, allPages)
        assertThat(pagesToShow).isEqualTo(emptyList<OnboardingPage>())
    }

    @Test
    fun featuresHighlight_shouldContain_noMoreThanFivePages() {
        val allPages = mapOf(
            OnboardingPage.PAYMENT to 0,
            OnboardingPage.DAILY_BUDGET to 0,
            OnboardingPage.PRICE_MASTER to 0,
            OnboardingPage.NEW_ADS_TYPES to 0,
            OnboardingPage.SETTINGS to 0,
            OnboardingPage.STATS to 0,
            OnboardingPage.SUMMARY to 0
        )

        val pagesToShow = OnboardingPage.getFeaturesHighlightPages(-1, 1, allPages)

        assertThat(pagesToShow).isEqualTo(listOf(
            OnboardingPage.PRICE_MASTER,
            OnboardingPage.NEW_ADS_TYPES,
            OnboardingPage.SETTINGS,
            OnboardingPage.STATS,
            OnboardingPage.SUMMARY
        ))
    }
}