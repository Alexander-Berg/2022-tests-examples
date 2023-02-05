package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test

class RxUsingIsLazyRuleTest {

    @Test
    fun `Reports issue if function returns Observable but first line is not return`() {
        val findings = """
            fun run(): Observable<Int> {
                doSomething()
                return dependency.getStream()
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The function run returns RxJava chain. You must return chain using lazy principle. Use defer() operator f.e."
        )
    }

    @Test
    fun `Reports issue if function returns Completable but first line is not return`() {
        val findings = """
            fun run(): Completable {
                doSomething()
                return dependency.getStream()
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The function run returns RxJava chain. You must return chain using lazy principle. Use defer() operator f.e."
        )
    }

    @Test
    fun `Reports issue if function returns Single but first line is not return`() {
        val findings = """
            fun run(): Single<Int> {
                doSomething()
                return dependency.getStream()
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The function run returns RxJava chain. You must return chain using lazy principle. Use defer() operator f.e."
        )
    }

    @Test
    fun `Reports issue if function returns Maybe but first line is not return`() {
        val findings = """
            fun run(): Maybe<Int> {
                doSomething()
                return dependency.getStream()
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The function run returns RxJava chain. You must return chain using lazy principle. Use defer() operator f.e."
        )
    }

    @Test
    fun `Reports issue if function returns Flowable but first line is not return`() {
        val findings = """
            fun run(): Flowable<Int> {
                doSomething()
                return dependency.getStream()
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The function run returns RxJava chain. You must return chain using lazy principle. Use defer() operator f.e."
        )
    }

    @Test
    fun `Ignore checks in start of function`() {
        val findings = """
            fun run(position: Int): Flowable {
                requireIntMoreZero(position)
                return dependency.getStream()
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    private fun String.toFindings(): List<Finding> = RxUsingIsLazyRule().lint(trimIndent())
}