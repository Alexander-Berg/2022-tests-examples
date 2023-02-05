package ru.yandex.yandexmaps.multiplatform.uitesting.api

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status.STABLE
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status.UNSTABLE
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases.*

public class TestSuiteProvider {
    public companion object {

        public enum class Platform {
            ANDROID,
            IOS,
        }

        private val allTest = listOf(
            MapsMobileTesting1830(),
            MapsMobileTesting2418(),
            MapsMobileTesting2691(),
            MapsMobileTesting2692(),
            MapsMobileTesting2695(),
            MapsMobileTesting2696(),
            MapsMobileTesting2805(),
            MapsMobileTesting2806(),
            MapsMobileTesting2807(),
            MapsMobileTesting2808(),
            MapsMobileTesting2809(),
            MapsMobileTesting2810(),
            MapsMobileTesting2811(),
            MapsMobileTesting2812(),
            MapsMobileTesting2813(),
            MapsMobileTesting2814(),
            MapsMobileTesting2819(),
            MapsMobileTesting2820(),
            MapsMobileTesting2821(),
            MapsMobileTesting2822(),
            MapsMobileTesting2842(),
            MapsMobileTesting2843(),
            MapsMobileTesting2844(),
            MapsMobileTesting2845(),
            MapsMobileTesting2846(),
            MapsMobileTesting2847(),
            MapsMobileTesting2848(),
            MapsMobileTesting2849(),
            MapsMobileTesting2865(),
            MapsMobileTesting2879(),
            MapsMobileTesting4105(),
            MapsMobileTesting4949(),
            MapsMobileTesting4950(),
            MapsMobileTesting4951(),
            MapsMobileTesting4952(),
            MapsMobileTesting6267(),
            MapsMobileTesting6697(),
            MapsMobileTesting6702(),
            MapsMobileTesting6784(),
            MapsMobileTesting6785(),
        )

        public fun getDefaultSuite(platform: Platform): TestSuite {
            return TestSuite(
                "Default ${platform.name} Suite",
                allTest.filter { it.status() == STABLE }
            )
        }

        public fun getQuarantineSuite(platform: Platform): TestSuite {
            return TestSuite(
                "Quarantine ${platform.name} Suite",
                allTest.filter { it.status() == UNSTABLE }
            )
        }

        public fun getNightlySuite(platform: Platform): TestSuite {
            return TestSuite(
                "Nightly ${platform.name} Suite",
                allTest.filter { it.status() == STABLE && it.scopes().contains(Scope.NIGHT) }
            )
        }

        public fun getPrSuite(platform: Platform): TestSuite {
            return TestSuite(
                "Pr ${platform.name} Suite",
                allTest.filter { it.status() == STABLE && it.scopes().contains(Scope.PR) }
            )
        }

        public fun getReleaseSuite(platform: Platform): TestSuite {
            return TestSuite(
                "Release ${platform.name} Suite",
                allTest.filter { it.status() == STABLE && it.scopes().contains(Scope.RELEASE) }
            )
        }
    }
}

public data class TestSuite(public val id: String, public val tests: List<Test>)
