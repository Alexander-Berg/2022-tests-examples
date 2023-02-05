package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test

class SynchronizedBlockRuleTest {

    @Test
    fun `Reports issue if synchronized function`() {
        val findings = """
           
            @Synchronized
            fun foo() {
            
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "Synchronized annotation uses this as lock. Create lock object for this"
        )
    }

    @Test
    fun `Reports issue if synchronized block uses lock inside`() {
        val findings = """
            
            fun foo() {
                synchronized(lock){
                    lock.something()
                }
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "Synchronized block uses lock inside. Create lock for this block and use it"
        )
    }

    @Test
    fun `Reports issue if synchronized block uses this as lock`() {
        val findings = """
            
            fun foo() {
                synchronized(this){
                    something()
                }
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "Synchronized block uses this as lock. Create lock object for this"
        )
    }

    @Test
    fun `Reports issue if synchronized block uses dependencies inside`() {
        val findings = """
            
            class Klass(val dep1: Dep1){
            
            private val lock = Any()
            
            fun foo() {
                synchronized(lock){
                    dep1.something()
                }
            }
            
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "You use dependencies in synchronized block. Dead-lock can be because of that"
        )
    }

    @Test
    fun `Do not reports issue if synchronized block does not use lock inside`() {
        val findings = """
            
            fun foo() {
                synchronized(lock){
                    value.something()
                }
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    private fun String.toFindings(): List<Finding> = SynchronizedBlockRule().lint(trimIndent())
}