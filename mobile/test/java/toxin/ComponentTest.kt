package toxin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import toxin.exception.MissingObjectFactoryException

class ComponentTest {

    @Test
    fun `Check method 'auto' returns definition from scope`() {
        val component = GreetingComponent()
        val text = component.text()
        val greeting = component.greeting()
        assertThat(text).isEqualTo(GREETING_TEXT)
        assertThat(greeting).isEqualTo(Greeting(GREETING_TEXT))
    }

    @Test
    fun `Check method 'assisted' returns definition from assistance lambda and allows to use accessor`() {
        val prefix = "Let's say: "
        val component = GreetingComponent()
        val greeting = component.greetingWithPrefix(prefix)
        assertThat(greeting).isEqualTo(Greeting(prefix + GREETING_TEXT))
    }

    @Test(expected = MissingObjectFactoryException::class)
    fun `Check method 'auto' throws error if definition not found`() {
        val component = GreetingComponent()
        component.illegalAutoNumber()
    }

    @Test(expected = MissingObjectFactoryException::class)
    fun `Check method 'assisted' throws error if definition not found`() {
        val component = GreetingComponent()
        component.illegalAssistedNumber()
    }

    @Test
    fun `Check method 'collection' returns defined collection`() {
        val component = FeaturesComponent()
        val features = component.features()
        assertThat(features).containsExactly("foo", "bar", "baz")
    }

    @Test
    fun `Check method 'collection' returns defined collection lazy`() {
        val component = FeaturesComponent()
        val features = component.featuresLazy().value
        assertThat(features).containsExactly("foo", "bar", "baz")
    }

    @Test
    fun `Check method 'collection' returns defined collection provider`() {
        val component = FeaturesComponent()
        val features = component.featuresProvider().get()
        assertThat(features).containsExactly("foo", "bar", "baz")
    }
}

private const val GREETING_TEXT = "Hello, world!"

private data class Greeting(val text: String)

private val greetingScope = scope("greeting-scope") {
    useModule(module {
        factory<String> { GREETING_TEXT }
        factory<Greeting> { Greeting(text = get()) }
    })
}

private class GreetingComponent : Component(greetingScope) {

    fun text() = auto<String>()

    fun greeting() = auto<Greeting>()

    fun greetingWithPrefix(prefix: String) = assisted<Greeting> {
        val text = get<String>()
        Greeting(prefix + text)
    }

    fun illegalAutoNumber() = auto<Int>()

    fun illegalAssistedNumber() = assisted { get<Int>() }
}

private val featureScope = scope("features-scope") {
    useModule(module {
        collect<String> { "foo" }
        collect<String> { "bar" }
        collect<String> { "baz" }
    })
}

private class FeaturesComponent : Component(featureScope) {

    fun features() = collection<String>()
    fun featuresLazy() = collectionLazy<String>()
    fun featuresProvider() = collectionProvider<String>()
    fun illegalCollection() = collection<Int>()
}
