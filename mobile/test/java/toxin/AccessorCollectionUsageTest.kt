package toxin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import toxin.exception.MissingCollectionFactoryException

class AccessorCollectionUsageTest {

    @Test
    fun `accessor#collect() returns non-nullable definition`() {
        val expected = "Hello, world!"
        val scope = scopeWithStringItemDefinition(expected)
        assertThat(scope.accessor.collect<String>()).isEqualTo(listOf(expected))
    }

    @Test
    fun `accessor#collectionProvider() returns non-nullable definition provider`() {
        val expected = "Hello, world!"
        val scope = scopeWithStringItemDefinition(expected)
        assertThat(scope.accessor.collectionProvider<String>().get()).isEqualTo(listOf(expected))
    }

    @Test
    fun `accessor#collection() returns non-nullable definition lazy`() {
        val expected = "Hello, world!"
        val scope = scopeWithStringItemDefinition(expected)
        assertThat(scope.accessor.collectionLazy<String>().value).isEqualTo(listOf(expected))
    }

    @Test
    fun `accessor#collectOrEmpty() returns non-nullable definition`() {
        val expected = "Hello, world!"
        val scope = scopeWithStringItemDefinition(expected)
        assertThat(scope.accessor.collectOrEmpty<String>()).isEqualTo(listOf(expected))
    }

    @Test(expected = MissingCollectionFactoryException::class)
    fun `accessor#collection() throws error if items not defined`() {
        val scope = scopeWithoutDefinitions()
        scope.accessor.collect<String>()
    }

    @Test(expected = MissingCollectionFactoryException::class)
    fun `accessor#collection() throws error if items for provider not defined`() {
        val scope = scopeWithoutDefinitions()
        scope.accessor.collectionProvider<String>().get()
    }

    @Test(expected = MissingCollectionFactoryException::class)
    fun `accessor#collection() throws error if items for lazy not defined`() {
        val scope = scopeWithoutDefinitions()
        scope.accessor.collectionLazy<String>().value
    }

    @Test
    fun `accessor#collectOrEmpty() returns empty if items not defined`() {
        val scope = scopeWithoutDefinitions()
        assertThat(scope.accessor.collectOrEmpty<String>()).isEqualTo(emptyList<String>())
    }

    private fun scopeWithoutDefinitions(): Scope {
        return scope("test-scope") {
            // no definitions
        }
    }

    private fun scopeWithStringItemDefinition(string: String): Scope {
        return scope("test-scope") {
            useModule(module {
                collect<String> { string }
            })
        }
    }
}
