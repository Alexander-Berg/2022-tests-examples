package com.yandex.mobile.realty.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.yandex.mobile.realty.lint.issues.MaterialTextInputDetector
import org.junit.Test

/**
 * @author merionkov on 20.06.2022.
 */
@Suppress("UnstableApiUsage", "MaxLineLength")
class MaterialTextInputDetectorTest {

    @Test
    fun shouldDetectMaterialTextInputsUsageCorrectly() {
        val fileToCheck = xml(
            "res/layout/sample.xml",
            """
                <com.google.android.material.textfield.TextInputLayout
                    xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content">

                    <com.google.android.material.textfield.TextInputEditText
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content" />

                </com.google.android.material.textfield.TextInputLayout>
                """,
        )
        val expectedError = """
            res/layout/sample.xml:1: Error: Use com.yandex.mobile.realty.widget.TextInputLayout instead of com.google.android.material.textfield.TextInputLayout. [RealtyInput]
            <com.google.android.material.textfield.TextInputLayout
             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            res/layout/sample.xml:6: Error: Use com.yandex.mobile.realty.widget.TextInputEditText instead of com.google.android.material.textfield.TextInputEditText. [RealtyInput]
                <com.google.android.material.textfield.TextInputEditText
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            2 errors, 0 warnings
        """
        val expectedFix = """
            Fix for res/layout/sample.xml line 1: Replace com.google.android.material.textfield.TextInputLayout by com.yandex.mobile.realty.widget.TextInputLayout.:
            @@ -1 +1
            - <com.google.android.material.textfield.TextInputLayout
            + <com.yandex.mobile.realty.widget.TextInputLayout
            Fix for res/layout/sample.xml line 6: Replace com.google.android.material.textfield.TextInputEditText by com.yandex.mobile.realty.widget.TextInputEditText.:
            @@ -6 +6
            -     <com.google.android.material.textfield.TextInputEditText
            +     <com.yandex.mobile.realty.widget.TextInputEditText
        """
        lint().files(fileToCheck.indented())
            .issues(MaterialTextInputDetector.ISSUE)
            .run()
            .expect(expectedError)
            .expectFixDiffs(expectedFix)
    }
}
