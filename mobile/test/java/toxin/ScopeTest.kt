package toxin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import toxin.exception.ScopeInitializationException

class ScopeTest {

    @Test
    fun `Scope provides types declared in modules`() {
        val module = module {
            factory<String> { "Hello, world!" }
        }
        val scope = scope("test-scope") {
            useModule(module)
        }
        val accessor = Scope.Accessor(scope)
        assertThat(accessor.get<String>()).isEqualTo("Hello, world!")
    }

    @Test(expected = ScopeInitializationException::class)
    fun `Scope throws initialization exception if type declared in several modules`() {
        val scope = scope("test-scope") {
            useModule(module {
                factory<String> { "Hello," }
            })
            useModule(module {
                factory<String> { "world!" }
            })
        }
    }

    @Test
    fun `Scope provides types declared in parent scope`() {
        val module = module {
            factory<String> { "Hello, world!" }
        }
        val parentScope = scope("parent-test-scope") {
            useModule(module)
        }
        val scope = scope("test-scope") {
            dependsOn(parentScope)
        }
        val accessor = Scope.Accessor(scope)
        assertThat(accessor.get<String>()).isEqualTo("Hello, world!")
    }

    @Test
    fun `Scope overrides types declared in parent scope`() {
        val parentScope = scope("parent-test-scope") {
            useModule(
                module {
                    factory<String> { "Hello, world!" }
                }
            )
        }
        val scope = scope("test-scope") {
            dependsOn(parentScope)
            useModule(
                module {
                    factory<String>(allowOverride = true) { "Hello, horde!" }
                }
            )
        }
        val accessor = Scope.Accessor(scope)
        assertThat(accessor.get<String>()).isEqualTo("Hello, horde!")
    }

    @Test
    fun `Later scope have bigger priority`() {
        val coreScope = scope("core-scope") {
            useModule(module {
                factory<Boolean> { false }
            })
        }
        val settingsScope = scope("settings-scope") {
            dependsOn(coreScope)
            useModule(module {
                factory<Boolean>(allowOverride = true) { true }
            })
        }
        val featureScope = scope("feature-scope") {
            dependsOn(coreScope)
            dependsOn(settingsScope)
        }

        class FeatureComponent : Component(featureScope) {
            fun getFlag() = auto<Boolean>()
        }

        assertThat(FeatureComponent().getFlag()).isEqualTo(true)
    }
}
