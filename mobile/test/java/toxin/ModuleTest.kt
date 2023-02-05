package toxin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ModuleTest {

    @Test
    fun `Module don't creates factories while module instance creation`() {
        var invokeCounter = 0
        val module = module {
            invokeCounter++
        }
        assertThat(invokeCounter).isEqualTo(0)
    }

    @Test
    fun `Module registers definitions while scope creation`() {
        var invokeCounter = 0
        val module = module {
            invokeCounter++
            singleton<String> { "Hello, world!" }
        }
        val scope = scope("test-scope") {
            useModule(module)
        }
        assertThat(invokeCounter).isEqualTo(1)
    }

    @Test
    fun `Module registers new definitions for each scope`() {
        var invokeCounter = 0
        val module = module {
            invokeCounter++
            singleton<String> { "Hello, world!" }
        }
        val firstScope = scope("test-scope-1") {
            useModule(module)
        }
        val secondScope = scope("test-scope-2") {
            useModule(module)
        }
        val firstAccessor = Scope.Accessor(firstScope)
        val secondAccessor = Scope.Accessor(secondScope)
        firstAccessor.get<String>()
        secondAccessor.get<String>()
        assertThat(invokeCounter).isEqualTo(2)
    }
}
