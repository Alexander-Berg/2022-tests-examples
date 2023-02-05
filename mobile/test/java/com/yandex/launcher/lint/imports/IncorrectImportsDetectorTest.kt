package com.yandex.launcher.lint.imports

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFiles.kt
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.yandex.launcher.lint.externaltheme.FileUtil
import junit.framework.TestCase
import java.io.ByteArrayInputStream
import java.io.InputStream

@Suppress("UnstableApiUsage")
class IncorrectImportsDetectorTest : LintDetectorTest() {

    private val noErrorImports = """
            package com.yandex.launcher.app;
            import androidx.annotation.NonNull;
            public class CpuTracker {
            }"""

    private val threeErrorImports = """
            package com.yandex.launcher.app;
            import org.jetbrains.annotations.NotNull;
            import org.jetbrains.annotations.Nullable;
            import com.yandex.core.utils.Assert;
            public class CpuTracker {

                @org.jetbrains.annotations.Nullable
                public Integer getInt() {
                    return null;
                }
            }"""

    fun testPositive() {
        lint().files(
            java(noErrorImports)
        ).run().expectClean()
    }

    fun testNegative() {
        val expected =
            """
src/com/yandex/launcher/app/CpuTracker.java:3: Error: Found restricted import org.jetbrains.annotations.* [YandexLauncherIncorrectImport]
            import org.jetbrains.annotations.NotNull;
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/yandex/launcher/app/CpuTracker.java:4: Error: Found restricted import org.jetbrains.annotations.* [YandexLauncherIncorrectImport]
            import org.jetbrains.annotations.Nullable;
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/yandex/launcher/app/CpuTracker.java:5: Error: Found restricted import com.yandex.core.* [YandexLauncherIncorrectImport]
            import com.yandex.core.utils.Assert;
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/yandex/launcher/app/CpuTracker.java:9: Error: Found restricted import org.jetbrains.annotations.* [YandexLauncherIncorrectImport]
                public Integer getInt() {
                               ~~~~~~
4 errors, 0 warnings
            """.trimIndent()
        lint().files(java(threeErrorImports)).vital(false).run().expectErrorCount(4).expect(expected)
    }

    fun testNegativeKt() {
        lint()
            .files(
                kt("""
                        package com.yandex.launcher.app

                        import org.jetbrains.annotations.NotNull
                        import org.jetbrains.annotations.Nullable
                        import com.yandex.core.utils.Assert

                        class InvalidKotlinObject() {
                        }
                        """.trimIndent())
            )
            .issues(IncorrectImportsDetector.ISSUE)
            .run()
            .expectErrorCount(3)
            .expect("""
                src/com/yandex/launcher/app/InvalidKotlinObject.kt:3: Error: Found restricted import org.jetbrains.annotations.* [YandexLauncherIncorrectImport]
import org.jetbrains.annotations.NotNull
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/yandex/launcher/app/InvalidKotlinObject.kt:4: Error: Found restricted import org.jetbrains.annotations.* [YandexLauncherIncorrectImport]
import org.jetbrains.annotations.Nullable
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/yandex/launcher/app/InvalidKotlinObject.kt:5: Error: Found restricted import com.yandex.core.* [YandexLauncherIncorrectImport]
import com.yandex.core.utils.Assert
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
                    """.trimIndent())
    }

    fun testPositiveKt() {
        lint()
            .files(
                kt("""
                    package com.yandex.launcher.setup.importlayout.model
                    
                    import com.android.launcher3.ItemInfo
                    
                    data class Arrangement(
                        val workspaces: List<Workspace>,
                        val hotseat: Hotseat,
                        val workspaceSize: GridSize,
                        val hotseatSize: GridSize
                    )
                    
                    sealed class Container(val id: Long)
                    
                    //noinspection SyntheticAccessor
                    class Workspace(val position: Long, val itemInfos: List<ItemInfo>): Container(Const.CONTAINER_WORKSPACE_ID)
                    
                    //noinspection SyntheticAccessor
                    class Hotseat(val itemInfos: List<ItemInfo>): Container(Const.CONTAINER_HOTSEAT_ID)
                    
                    data class GridSize(val sizeX: Int, val sizeY: Int)
                        """.trimIndent())
            )
            .issues(IncorrectImportsDetector.ISSUE)
            .run()
            .expectErrorCount(0)
    }

    override fun getDetector(): Detector {
        return IncorrectImportsDetector()
    }

    override fun getIssues(): List<Issue> {
        return listOf(IncorrectImportsDetector.ISSUE)
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