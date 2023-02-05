package toxin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class InjectTest {

    @Test
    fun `Inject provides instance from scope`() {
        val scope = scope("test-scope") {
            useModule(
                module {
                    factory<String> { "Hello, world!" }
                }
            )
        }

        class MyComponent : Component(scope) {

            fun string(): String = auto()
        }

        val myComponent = MyComponent()

        class UsageCase {

            val myString by inject(myComponent::string)
            val myStringLazy by injectLazy(myComponent::string)
            val myStringProvider by injectProvider(myComponent::string)
        }

        val usageCase = UsageCase()
        val myString = usageCase.myString
        val myStringLazy = usageCase.myStringLazy
        val myStringProvider = usageCase.myStringProvider


        assertThat(myString).isEqualTo("Hello, world!")
        assertThat(myStringLazy.value).isEqualTo("Hello, world!")
        assertThat(myStringProvider.get()).isEqualTo("Hello, world!")
    }
}
