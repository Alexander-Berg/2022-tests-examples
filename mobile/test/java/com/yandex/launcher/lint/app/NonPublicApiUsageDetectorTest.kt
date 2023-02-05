package com.yandex.launcher.lint.app

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFiles.kt
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.yandex.launcher.lint.externaltheme.FileUtil
import junit.framework.TestCase
import java.io.ByteArrayInputStream
import java.io.InputStream

@Suppress("UnstableApiUsage")
class NonPublicApiUsageDetectorTest : LintDetectorTest() {

    private val noErrorImports = """
            package com.yandex.launcher.app;
            import androidx.annotation.NonNull;
            import com.yandex.launcher.api.LauncherHost;
            import com.yandex.launcher.api.SuggestToolkit;
            
            public class CpuTracker extends com.yandex.launcher.app.LauncherApplicationDelegate {
            }"""

    private val errorImportsAndParent = """
            package com.yandex.launcher.app;
            import org.jetbrains.annotations.NotNull;
            import org.jetbrains.annotations.Nullable;
            import com.yandex.core.utils.Assert;
            import com.yandex.launcher.Launcher;
            
            public class CpuTracker extends com.yandex.launcher.Launcher {
            }"""

    fun testPositive() {
        lint().files(
            java(noErrorImports)
        ).run().expectClean()
    }

    fun testNegative() {
        val expected =
            """
src/com/yandex/launcher/app/CpuTracker.java:6: Error: Don't use Launcher api which not marked as PublicInterface, it can break easily [YandexLauncherPublicApiUsage]
            import com.yandex.launcher.Launcher;
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/yandex/launcher/app/CpuTracker.java:8: Error: Don't use Launcher api which not marked as PublicInterface, it can break easily [YandexLauncherPublicApiUsage]
            public class CpuTracker extends com.yandex.launcher.Launcher {
                         ~~~~~~~~~~
2 errors, 0 warnings
            """.trimIndent()
        lint()
            .files(java(errorImportsAndParent))
            .vital(false)
            .run()
            .expectErrorCount(2)
            .expect(expected)
    }

    fun testNegativeKt() {
        lint()
            .files(
                kt("""
                        package com.yandex.launcher.app

                        import org.jetbrains.annotations.NotNull
                        import org.jetbrains.annotations.Nullable
                        import com.yandex.launcher.Launcher;

                        class InvalidKotlinObject() {
                        }
                        """.trimIndent())
            )
            .issues(NonPublicApiUsageDetector.ISSUE)
            .run()
            .expectErrorCount(1)
            .expect("""
src/com/yandex/launcher/app/InvalidKotlinObject.kt:5: Error: Don't use Launcher api which not marked as PublicInterface, it can break easily [YandexLauncherPublicApiUsage]
import com.yandex.launcher.Launcher;
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
                    """.trimIndent())
    }

    fun testPositiveKt() {
        lint()
            .files(
                kt("""
                    package com.yandex.launcher.setup.importlayout.model
                    
                    import com.yandex.launcher.api.LauncherHost
                    
                    data class Arrangement(
                        val hotseatSize: Int
                    )
                        """.trimIndent())
            )
            .issues(NonPublicApiUsageDetector.ISSUE)
            .run()
            .expectErrorCount(0)
    }

    override fun getDetector(): Detector {
        return NonPublicApiUsageDetector()
    }

    override fun getIssues(): List<Issue> {
        return listOf(NonPublicApiUsageDetector.ISSUE)
    }

    override fun getTestResource(relativePath: String, expectExists: Boolean): InputStream {
        try {
            return FileUtil.getResource(relativePath, javaClass.protectionDomain.codeSource)
        } catch (e: Exception) {
            TestCase.fail(e.message)
        }

        return ByteArrayInputStream("".toByteArray())
    }
}