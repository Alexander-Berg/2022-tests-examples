package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RxSubscriptionViolationTest {

    @Test
    fun `Expect warning when method without annotation return Single class Kotlin`() {
        val findings = """
            import io.reactivex.Single
            import ru.yandex.market.test.extensions.asSingle
            
            class Test {
            
                fun run() : Single<Int> {
                    return 10.asSingle()
                }
            }
        """.toFindings()

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("You need to use @CheckResult annotation for this method!")
    }

    @Test
    fun `Expect clean when method with annotation return Single class`() {
        val findings = """
            import io.reactivex.Single
            import androidx.annotation.CheckResult

            class Test {
            
                @CheckResult
                fun run(): Single<Int> {
                    return Single.just(10)
                }
            }
        """.toFindings()

        assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expect warning when method without annotation return Maybe class`() {
        val findings = """
            import io.reactivex.Maybe

            class Test {
            
                fun run(): Maybe<Int> {
                    return Maybe.just(10)
                }
            }
           """.toFindings()

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("You need to use @CheckResult annotation for this method!")
    }

    @Test
    fun `Expect warning when method without annotation return Flowable class`() {
        val findings = """
            import io.reactivex.Flowable

            class Test {
            
                fun run(): Flowable<String> {
                    return Flowablejust("Test")
                }
            }
           """.toFindings()

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("You need to use @CheckResult annotation for this method!")
    }

    @Test
    fun `Expect warning when method without annotation return Observable class`() {
        val findings = """
            import io.reactivex.Observable

            class Test {
            
                fun run(): Observable<Int> {
                    return Observable.just(10)
                }
            }
           """.toFindings()

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("You need to use @CheckResult annotation for this method!")
    }

    @Test
    fun `Expect warning when method without annotation return Completable class`() {
        val findings = """
            import io.reactivex.Completable

            class Test {
            
                fun run(): Completable {
                    return Completable.complete()
                }
            }
           """.toFindings()

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("You need to use @CheckResult annotation for this method!")
    }

    @Test
    fun `Expect clean when method without annotation return not rx class`() {
        val findings = """
            class Test {
            
                fun run(): Int {
                    return 10
                }
            }
            """.toFindings()

        assertThat(findings).hasSize(0)
    }

    private fun String.toFindings(): List<Finding> = RxSubscriptionViolation().lint(trimIndent())
}