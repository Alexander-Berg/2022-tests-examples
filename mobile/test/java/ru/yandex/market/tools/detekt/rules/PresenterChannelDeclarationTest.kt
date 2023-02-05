package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PresenterChannelDeclarationTest {

    @Test
    fun `Report warning when using channel prefix as postfix`() {
        val findings = """
            package foo
            import ru.yandex.market.mvp.moxy.BasePresenter
            
            class TestPresenter : BasePresenter() {
            
                companion object {
                    private val TEST_CHANNEL = Channel() 
                }
            }
        """.toFindings()

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo(
            "The Channel was declared incorrectly! The field must be:\n\t- start with the prefix \"CHANNEL_\"\n"
        )
    }

    @Test
    fun `Report warning when using lower case letters in channel name`() {
        val findings = """
            package foo
            import ru.yandex.market.mvp.moxy.BasePresenter
            
            class TestPresenter : BasePresenter() {
            
                companion object {
                    private val CHANNEL_Test = Channel() 
                }
            }
        """.toFindings()

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo(
            "The Channel was declared incorrectly! The field must be:\n\t- written in upper case\n"
        )
    }

    @Test
    fun `Report warning when channel field is not final`() {
        val findings = """
            class TestPresenter : BasePresenter() {
            
                companion object {
                    private var CHANNEL_TEST = Channel() 
                }
            }
        """.toFindings()

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo(
            "The Channel was declared incorrectly! The field must be:\n\t- val\n"
        )
    }

    @Test
    fun `Report warning when channel field is not private`() {
        val findings = """
            package foo
            import ru.yandex.market.mvp.moxy.BasePresenter
            class TestPresenter : BasePresenter() {
                    
                companion object {
                    val CHANNEL_TEST = Channel() 
                }
            }
        """.toFindings()

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo(
            "The Channel was declared incorrectly! The field must be:\n\t- private\n"
        )
    }

    @Test
    fun `Report multiple errors in one message`() {
        val findings = """
            package foo
            import ru.yandex.market.mvp.moxy.BasePresenter
            class TestPresenter : BasePresenter() {
                    
                companion object {
                    var TEST_CHANNEL = Channel()
                }
            }
        """.toFindings()

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo(
            "The Channel was declared incorrectly! The field must be:" +
                    "\n\t- val" +
                    "\n\t- private" +
                    "\n\t- start with the prefix \"CHANNEL_\"\n"
        )
    }

    @Test
    fun `No warnings when channel is declared appropriately`() {
        val findings = """
            package foo
            import ru.yandex.market.mvp.moxy.BasePresenter
            class TestPresenter : BasePresenter() {
                    
                companion object {
                    private val CHANNEL_TEST = Channel() 
                }
            }
        """.toFindings()

        assertThat(findings).hasSize(0)
    }

    @Test
    fun `No warnings when channel property is abstract`() {
        val findings = """
            package foo
            import ru.yandex.market.mvp.moxy.BasePresenter
            
            abstract class TestPresenter : BasePresenter() {
            
                abstract val channelId: Channel
            }
            
        """.toFindings()

        assertThat(findings).hasSize(0)
    }

    @Test
    fun `No warnings when overriding channel property from abstract parent`() {
        val findings = """
            package foo
            import ru.yandex.market.mvp.moxy.BasePresenter
            abstract class AbstractTestPresenter : BasePresenter() {
                    
                abstract val channelId: Channel
            }
            
            package foo
            import ru.yandex.market.mvp.moxy.BasePresenter
            
            class TestPresenter : AbstractTestPresenter() {
                    
                override val channelId = Channel()
            }
            """.toFindings()

        assertThat(findings).hasSize(0)
    }

    private fun String.toFindings(): List<Finding> = PresenterChannelDeclaration().lint(trimIndent())
}