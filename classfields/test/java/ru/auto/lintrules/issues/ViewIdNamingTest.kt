package ru.auto.lintrules.issues

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.lintrules.lintAsLayoutFile

@RunWith(AllureRunner::class) class ViewIdNamingTest {
    @Test
    fun shouldReportNothingAsVIsCorrectNaming() {
        @Language("XML")
        val fileContent = """
        <LinearLayout
            xmlns:android="http://schemas.android.com/apk/res/android"
            android:id="@+id/vLayout" >
        </LinearLayout>
        """.trimIndent()

        fileContent.lintAsLayoutFile(ViewIdNamingIssue)
            .expectClean()
    }


    @Test
    fun shouldReportIncorrectId() {
        @Language("XML")
        val fileContent = """
        <LinearLayout
            xmlns:android="http://schemas.android.com/apk/res/android"
            android:id="@+id/layout" >
        </LinearLayout>
        """.trimIndent()

        fileContent.lintAsLayoutFile(ViewIdNamingIssue)
            .expect(
                """
                |res/layout/dummy_layout.xml:3: Warning: Incorrect view id prefix [ViewIdNaming]
                |    android:id="@+id/layout" >
                |    ~~~~~~~~~~~~~~~~~~~~~~~~
                |0 errors, 1 warnings""".trimMargin()
            )
    }

    @Test
    fun shouldSuggestFixForIncorrectId() {
        @Language("XML")
        val fileContent = """
        <LinearLayout
            xmlns:android="http://schemas.android.com/apk/res/android"
            android:id="@+id/llLayout" >
        </LinearLayout>
        """.trimIndent()

        fileContent.lintAsLayoutFile(ViewIdNamingIssue)
            .verifyFixes().expectFixDiffs("""
               |Fix for res/layout/dummy_layout.xml line 3: Add `v` prefix:
               |@@ -3 +3
               |-     android:id="@+id/llLayout" >
               |+     android:id="@+id/vLlLayout >
               |Fix for res/layout/dummy_layout.xml line 3: Replace prefix with `v`:
               |@@ -3 +3
               |-     android:id="@+id/llLayout" >
               |+     android:id="@+id/vLayout >
            """.trimMargin())
    }
}
