package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class IllegalAnalyticsApiTest {

    @Test
    fun `Expected error when facade has public method without @AnalyticEvent annotation`() {
        val findings = IllegalAnalyticsApi().lint(
            """
                        import ru.yandex.market.analytics.AnalyticFacade
                        @AnalyticFacade
                        class FooAnalytics {

                            fun somePublicMethod() {}
                        }
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("You need to use @AnalyticEvent annotation for this method")
    }

    @Test
    fun `Expected error when facade has public method without @AnalyticEvent annotation of the second function`() {
        val findings = IllegalAnalyticsApi().lint(
            """
                        import ru.yandex.market.analytics.AnalyticFacade
                        @AnalyticFacade
                        class FooAnalytics {

                            @AnalyticEvent
                            fun somePublicMethod1() {}
                            
                            fun somePublicMethod2() {}
                        }
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("You need to use @AnalyticEvent annotation for this method")
    }

    @Test
    fun `Expected error when facade has public methods without @AnalyticEvent annotation`() {
        val findings = IllegalAnalyticsApi().lint(
            """
                        import ru.yandex.market.analytics.AnalyticFacade
                        @AnalyticFacade
                        class FooAnalytics {

                            fun somePublicMethod1() {}
                            
                            fun somePublicMethod2() {}
                            
                            @AnalyticEvent
                            fun somePublicMethod3() {}
                            
                            fun somePublicMethod4() {}
                        }
            """.trimIndent()
        )
        assertThat(findings).hasSize(3)
        assertThat(findings[0].message).isEqualTo("You need to use @AnalyticEvent annotation for this method")
        assertThat(findings[1].message).isEqualTo("You need to use @AnalyticEvent annotation for this method")
        assertThat(findings[2].message).isEqualTo("You need to use @AnalyticEvent annotation for this method")
    }

    @Test
    fun `Expected clean when public method in facade has @AnalyticEvent annotation`() {
        val findings = IllegalAnalyticsApi().lint(
            """
                        import ru.yandex.market.analytics.AnalyticFacade
                        import ru.yandex.market.analytics.AnalyticEvent

                        @AnalyticFacade
                        class FooAnalytics {

                            @AnalyticEvent
                            fun somePublicMethod() {}
                        }
            """.trimIndent()
        )
        assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expected error when analytic event declared out of facade`() {
        val findings = IllegalAnalyticsApi().lint(
            """
                        import ru.yandex.market.analytics.AnalyticEvent

                        class FooAnalytics {

                            @AnalyticEvent
                            fun analyticEvent() {}
                        }
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("You need to use @AnalyticFacade annotation for this class")
    }

    @Test
    fun `Expected clean when one has only function without class`() {
        val findings = IllegalAnalyticsApi().lint(
            """
                        fun somePublicMethod() {}
            """.trimIndent()
        )
        assertThat(findings).hasSize(0)
    }
}