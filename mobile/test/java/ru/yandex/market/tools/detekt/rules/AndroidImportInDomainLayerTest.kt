package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AndroidImportInDomainLayerTest {

    @Test
    fun `Report warning for class in domain package with android import`() {
        val findings = AndroidImportInDomainLayer().lint(
            """
                    package ru.yandex.market.clean.domain
                    import android.Parcerable
                    class TestClass
                """.trimIndent()
        )
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("Android imports should not be in the domain layer!")
    }

    @Test
    fun `Expect clean for class in domain package with not android import`() {
        val findings = AndroidImportInDomainLayer().lint(
            """
                    package ru.yandex.market.clean.domain
                    import kotlin.collections.*
                    class TestClass
                """.trimIndent()
        )
        assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expect clean for class in not domain package with android import`() {
        val findings = AndroidImportInDomainLayer().lint(
            """
                    package ru.yandex.market.clean
                    import android.Parcerable
                    class TestClass
                """.trimIndent()
        )
        assertThat(findings).hasSize(0)
    }

    @Test
    fun `Report warning for class in domain package with android import when we have more than one import`() {
        val findings = AndroidImportInDomainLayer().lint(
            """
                    package ru.yandex.market.clean.domain
                    import kotlin.collections.*
                    import android.*
                    class TestClass
                """.trimIndent()
        )
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("Android imports should not be in the domain layer!")
    }

    @Test
    fun `Report warning for class in domain package with android import when we have more than one android import`() {
        val findings = AndroidImportInDomainLayer().lint(
            """
                    package ru.yandex.market.clean.domain
                    import android.test.*
                    import android.*
                    class TestClass
                """.trimIndent()
        )
        assertThat(findings).hasSize(2)
        assertThat(findings[0].message).isEqualTo("Android imports should not be in the domain layer!")
        assertThat(findings[1].message).isEqualTo("Android imports should not be in the domain layer!")
    }

    @Test
    fun `Expect clean for class in domain package with androidx annotation import`() {
        val findings = AndroidImportInDomainLayer().lint(
            """
                    package ru.yandex.market.clean.domain
                    import androidx.annotation.CheckResult
                    class TestClass
                """.trimIndent()
        )
        assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expect clean for class in domain package with android annotation import`() {
        val findings = AndroidImportInDomainLayer().lint(
            """
                    package ru.yandex.market.clean.domain
                    import android.annotation.*
                    class TestClass
                """.trimIndent()
        )
        assertThat(findings).hasSize(0)
    }
}