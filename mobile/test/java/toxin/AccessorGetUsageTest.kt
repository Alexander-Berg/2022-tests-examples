package toxin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import toxin.exception.DefinitionNullabilityException
import toxin.exception.MissingObjectFactoryException

class AccessorGetUsageTest {

    @Test
    fun `accessor#get() returns non-nullable definition`() {
        val expected = "Hello, world!"
        val scope = scopeWithStringDefinition(expected)
        assertThat(scope.accessor.get<String>()).isEqualTo(expected)
        assertThat(scope.accessor.opt<String>()).isEqualTo(expected)
    }

    @Test
    fun `accessor#getProvider() returns non-nullable definition provider`() {
        val expected = "Hello, world!"
        val scope = scopeWithStringDefinition(expected)
        assertThat(scope.accessor.getProvider<String>().get()).isEqualTo(expected)
        assertThat(scope.accessor.optProvider<String>().get()).isEqualTo(expected)
    }

    @Test
    fun `accessor#getLazy() returns non-nullable definition lazy`() {
        val expected = "Hello, world!"
        val scope = scopeWithStringDefinition(expected)
        assertThat(scope.accessor.getLazy<String>().value).isEqualTo(expected)
        assertThat(scope.accessor.optLazy<String>().value).isEqualTo(expected)
    }

    @Test
    fun `accessor#get() returns nullable definition`() {
        val expected = "Hello, world!"
        val scope = scopeWithNullableStringDefinition(expected)
        assertThat(scope.accessor.get<String>()).isEqualTo(expected)
        assertThat(scope.accessor.opt<String>()).isEqualTo(expected)
    }

    @Test
    fun `accessor#getProvider() returns nullable definition provider`() {
        val expected = "Hello, world!"
        val scope = scopeWithNullableStringDefinition(expected)
        assertThat(scope.accessor.getProvider<String>().get()).isEqualTo(expected)
        assertThat(scope.accessor.optProvider<String>().get()).isEqualTo(expected)
    }

    @Test
    fun `accessor#getLazy() returns nullable definition lazy`() {
        val expected = "Hello, world!"
        val scope = scopeWithNullableStringDefinition(expected)
        assertThat(scope.accessor.getLazy<String>().value).isEqualTo(expected)
        assertThat(scope.accessor.optLazy<String>().value).isEqualTo(expected)
    }

    @Test
    fun `accessor#opt() returns null for nullable definition if nullable requested`() {
        val scope = scopeWithNullableStringDefinition(null)
        assertThat(scope.accessor.opt<String>()).isEqualTo(null)
    }

    @Test
    fun `accessor#optProvider() returns null provider for nullable definition if nullable requested`() {
        val scope = scopeWithNullableStringDefinition(null)
        assertThat(scope.accessor.optProvider<String>().get()).isEqualTo(null)
    }

    @Test
    fun `accessor#optLazy() returns null lazy for nullable definition if nullable requested`() {
        val scope = scopeWithNullableStringDefinition(null)
        assertThat(scope.accessor.optLazy<String>().value).isEqualTo(null)
    }

    @Test
    fun `accessor#getOrNull() returns null for nullable definition if nullable requested`() {
        val scope = scopeWithNullableStringDefinition(null)
        assertThat(scope.accessor.opt<String>()).isEqualTo(null)
    }

    @Test
    fun `accessor#getOrNull() returns null provider for nullable definition if nullable requested`() {
        val scope = scopeWithNullableStringDefinition(null)
        assertThat(scope.accessor.optProvider<String>().get()).isEqualTo(null)
    }

    @Test
    fun `accessor#getOrNull() returns null lazy for nullable definition if nullable requested`() {
        val scope = scopeWithNullableStringDefinition(null)
        assertThat(scope.accessor.optLazy<String>().value).isEqualTo(null)
    }

    @Test(expected = DefinitionNullabilityException::class)
    fun `accessor#get() throws error for nullable definition if non-nullable requested`() {
        val scope = scopeWithNullableStringDefinition(null)
        scope.accessor.get<String>()
    }

    @Test(expected = DefinitionNullabilityException::class)
    fun `accessor#getProvider() throws error for nullable definition if non-nullable provider requested`() {
        val scope = scopeWithNullableStringDefinition(null)
        scope.accessor.getProvider<String>().get()
    }

    @Test(expected = DefinitionNullabilityException::class)
    fun `accessor#getLazy() throws error for nullable definition if non-nullable lazy requested`() {
        val scope = scopeWithNullableStringDefinition(null)
        scope.accessor.getLazy<String>().value
    }

    @Test
    fun `accessor#getOrNull() returns null for nullable definition if non-nullable requested`() {
        val scope = scopeWithNullableStringDefinition(null)
        assertThat(scope.accessor.opt<String>()).isEqualTo(null)
    }

    @Test
    fun `accessor#optProvider() returns null for nullable definition if non-nullable provider requested`() {
        val scope = scopeWithNullableStringDefinition(null)
        assertThat(scope.accessor.optProvider<String>().get()).isEqualTo(null)
    }

    @Test
    fun `accessor#optLazy() returns null for nullable definition if non-nullable lazy requested`() {
        val scope = scopeWithNullableStringDefinition(null)
        assertThat(scope.accessor.optLazy<String>().value).isEqualTo(null)
    }

    @Test(expected = MissingObjectFactoryException::class)
    fun `accessor#get() throws error if definition not found`() {
        val scope = scopeWithoutDefinitions()
        scope.accessor.get<String>()
    }

    @Test(expected = MissingObjectFactoryException::class)
    fun `accessor#getProvider() throws error if definition for provider not found`() {
        val scope = scopeWithoutDefinitions()
        scope.accessor.getProvider<String>().get()
    }

    @Test(expected = MissingObjectFactoryException::class)
    fun `accessor#getLazy() throws error if definition for lazy not found`() {
        val scope = scopeWithoutDefinitions()
        scope.accessor.getLazy<String>().value
    }

    @Test
    fun `accessor#opt() returns null if definition not found`() {
        val scope = scopeWithoutDefinitions()
        assertThat(scope.accessor.opt<String>()).isEqualTo(null)
    }

    @Test
    fun `accessor#optProvider() returns provider with null if definition for provider not found`() {
        val scope = scopeWithoutDefinitions()
        assertThat(scope.accessor.optProvider<String>().get()).isEqualTo(null)
    }

    @Test(expected = MissingObjectFactoryException::class)
    fun `accessor#optLazy() returns lazy with null if definition for lazy not found`() {
        val scope = scopeWithoutDefinitions()
        assertThat(scope.accessor.getLazy<String>().value).isEqualTo(null)
    }

    private fun scopeWithoutDefinitions(): Scope {
        return scope("test-scope") {
            // no definitions
        }
    }

    private fun scopeWithStringDefinition(string: String): Scope {
        return scope("test-scope") {
            useModule(module {
                factory<String> { string }
            })
        }
    }

    private fun scopeWithNullableStringDefinition(string: String?): Scope {
        return scope("test-scope") {
            useModule(module {
                factory<String?> { string }
            })
        }
    }
}
