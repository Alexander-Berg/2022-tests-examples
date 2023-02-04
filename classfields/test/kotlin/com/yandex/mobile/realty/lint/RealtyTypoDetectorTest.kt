package com.yandex.mobile.realty.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.yandex.mobile.realty.lint.issues.RealtyTypoDetector
import org.junit.Test

/**
 * @author merionkov on 16.09.2021.
 */
@Suppress("UnstableApiUsage", "MaxLineLength")
class RealtyTypoDetectorTest {

    @Test
    fun shouldDetectTypoCorrectly() {
        val fileToCheck = xml(
            "res/values/strings.xml",
            """
                <resources>
                    <string name="correct_string_1">Просто строка</string>
                    <string name="correct_string_2">Строка с корректным "ещё"</string>
                    <string name="incorrect_string_1">Строка с некорректным "еще"</string>
                    <string name="incorrect_string_2">Еще в начале текста</string>
                    <string name="incorrect_string_3">В конце текста еще</string>
                    <string name="incorrect_string_4">Некорректное написание ЕщЕ строчными и заглавными</string>
                    <string name="correct_string_3">Корректное написание ЕщЁ строчными и заглавными</string>
                    <string name="correct_string_4">Написание внутри слова "перемЕЩЕние"</string>
                    <string name="incorrect_string_5">Несколько еще внутри ещё одной еще строки</string>
                </resources>
                """,
        )
        val expectedError = """
            res/values/strings.xml:4: Error: Last character "е" in word "еще" should be replaced by "ё". Use "ё" whenever possible. [RealtyTypo]
                <string name="incorrect_string_1">Строка с некорректным "еще"</string>
                                                                         ^
            res/values/strings.xml:5: Error: Last character "е" in word "Еще" should be replaced by "ё". Use "ё" whenever possible. [RealtyTypo]
                <string name="incorrect_string_2">Еще в начале текста</string>
                                                  ^
            res/values/strings.xml:6: Error: Last character "е" in word "еще" should be replaced by "ё". Use "ё" whenever possible. [RealtyTypo]
                <string name="incorrect_string_3">В конце текста еще</string>
                                                                 ^
            res/values/strings.xml:7: Error: Last character "Е" in word "ЕщЕ" should be replaced by "Ё". Use "Ё" whenever possible. [RealtyTypo]
                <string name="incorrect_string_4">Некорректное написание ЕщЕ строчными и заглавными</string>
                                                                         ^
            res/values/strings.xml:10: Error: Last character "е" in word "еще" should be replaced by "ё". Use "ё" whenever possible. [RealtyTypo]
                <string name="incorrect_string_5">Несколько еще внутри ещё одной еще строки</string>
                                                            ^
            res/values/strings.xml:10: Error: Last character "е" in word "еще" should be replaced by "ё". Use "ё" whenever possible. [RealtyTypo]
                <string name="incorrect_string_5">Несколько еще внутри ещё одной еще строки</string>
                                                                                 ^
            6 errors, 0 warnings
        """
        val expectedFix = """
            Fix for res/values/strings.xml line 4: Replace last appearance of "е" by "ё".:
            @@ -4 +4
            -     <string name="incorrect_string_1">Строка с некорректным "еще"</string>
            +     <string name="incorrect_string_1">Строка с некорректным "ещё"</string>
            Fix for res/values/strings.xml line 5: Replace last appearance of "е" by "ё".:
            @@ -5 +5
            -     <string name="incorrect_string_2">Еще в начале текста</string>
            +     <string name="incorrect_string_2">Ещё в начале текста</string>
            Fix for res/values/strings.xml line 6: Replace last appearance of "е" by "ё".:
            @@ -6 +6
            -     <string name="incorrect_string_3">В конце текста еще</string>
            +     <string name="incorrect_string_3">В конце текста ещё</string>
            Fix for res/values/strings.xml line 7: Replace last appearance of "Е" by "Ё".:
            @@ -7 +7
            -     <string name="incorrect_string_4">Некорректное написание ЕщЕ строчными и заглавными</string>
            +     <string name="incorrect_string_4">Некорректное написание ЕщЁ строчными и заглавными</string>
            Fix for res/values/strings.xml line 10: Replace last appearance of "е" by "ё".:
            @@ -10 +10
            -     <string name="incorrect_string_5">Несколько еще внутри ещё одной еще строки</string>
            +     <string name="incorrect_string_5">Несколько ещё внутри ещё одной еще строки</string>
            Fix for res/values/strings.xml line 10: Replace last appearance of "е" by "ё".:
            @@ -10 +10
            -     <string name="incorrect_string_5">Несколько еще внутри ещё одной еще строки</string>
            +     <string name="incorrect_string_5">Несколько еще внутри ещё одной ещё строки</string>
        """
        lint().files(fileToCheck.indented())
            .issues(RealtyTypoDetector.ISSUE)
            .run()
            .expect(expectedError)
            .expectFixDiffs(expectedFix)
    }
}
