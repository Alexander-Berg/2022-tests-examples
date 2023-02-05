package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test

class SerializedSubjectRuleTest {

    @Test
    fun `Reports issue if if property is not serialized`() {
        val findings = """
            
@Singleton
class SyncApplicationLifecycleObserver @Inject constructor(
    private val syncCartServiceMediator: SyncCartServiceMediator
) : LifecycleObserver {

    private val syncCartSubject = PublishSubject.create<Any>()
    private var firstTime = true // скипаем синк на первом старте приложения, т.к там мы уже грузим корзину

}
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "Subject must be serialized. Use toSerialized()"
        )
    }

    @Test
    fun `Do not Reports issue if property is serialized`() {
        val findings = """
            
@Singleton
class SyncApplicationLifecycleObserver @Inject constructor(
    private val syncCartServiceMediator: SyncCartServiceMediator
) : LifecycleObserver {

    private val syncCartSubject = PublishSubject.create<Any>().toSerialized()
    private var firstTime = true // скипаем синк на первом старте приложения, т.к там мы уже грузим корзину

}
           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Dd not Reports issue if class is test`() {
        val findings = """
            
class SyncApplicationLifecycleObserverTest @Inject constructor(
    private val syncCartServiceMediator: SyncCartServiceMediator
) : LifecycleObserver {

    private val syncCartSubject = PublishSubject.create<Any>()
    private var firstTime = true // скипаем синк на первом старте приложения, т.к там мы уже грузим корзину

}
           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    private fun String.toFindings(): List<Finding> = SerializedSubjectRule().lint(trimIndent())
}