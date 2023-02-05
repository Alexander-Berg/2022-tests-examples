package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test

class KsonAnnotationTest {

    @Test
    fun `Kson incompatible in kotlin`() {
        val findings = KsonAnnotation().lint(
            """
                    import dev.afanasev.kson.annotation.Kson
                    @Kson
                    data class Model(val value: String): Response()
            """.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Kson не может быть применен к классам имеющим суперкласс")
    }

    @Test
    fun `Kson incompatible in kotlin long name`() {
        val findings = KsonAnnotation().lint(
            """
                    @dev.afanasev.kson.annotation.Kson
                    data class Model(val value: String): Response()
            """.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Kson не может быть применен к классам имеющим суперкласс")
    }

    @Test
    fun `Kson in kotlin`() {
        val findings = KsonAnnotation().lint(
            """
                    import dev.afanasev.kson.annotation.Kson
                    @Kson
                    data class Model(val value: String)
            """.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Kson in kotlin serializable`() {
        val findings = KsonAnnotation().lint(
            """
                    import dev.afanasev.kson.annotation.Kson
                    import java.io
                    @Kson
                    class Model: Serializable {
                    }
            """.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(0)
    }
}