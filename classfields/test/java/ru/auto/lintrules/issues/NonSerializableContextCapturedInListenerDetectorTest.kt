package ru.auto.lintrules.issues

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.lintrules.lintAsKotlinFile

@RunWith(AllureRunner::class) class NonSerializableContextCapturedInListenerDetectorTest {

    @Test
    fun shouldNotReportLocalVariableAccessInBuildListener() {
        @Language("Kt")
        val fileContent = """
        package ru.auto.lintrules.issues
        class Test {
          fun foo() {
            val x = 1
            buildChooseListener {
                x + 2
            }
          }
        }
        """.trimIndent()

        fileContent.lintAsKotlinFile(NonSerializableContextCapturedInListenerIssue)
            .expectClean()
    }

    @Test
    fun shouldReportNonLocalVariableAccessInBuildListener() {
        @Language("Kt")
        val fileContent = """
        package ru.auto.lintrules.issues

        class Test {

          private val x = 1

          fun foo() {
            buildChooseListener {
                x + 2
            }
          }
        }
        """.trimIndent()

        fileContent.lintAsKotlinFile(NonSerializableContextCapturedInListenerIssue)
            .expectClean()
    }
}
