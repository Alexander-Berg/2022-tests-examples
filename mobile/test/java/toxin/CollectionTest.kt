package toxin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import toxin.exception.MissingCollectionFactoryException

class CollectionTest {

    interface BaseType

    @Test
    fun `Collection contains all declared items`() {
        val firstInstance = object : BaseType {}
        val secondInstance = object : BaseType {}
        val module = module {
            collect<BaseType> {
                firstInstance
            }
            collect<BaseType> {
                secondInstance
            }
        }
        val scope = scope("test-scope") {
            useModule(module)
        }
        val accessor = Scope.Accessor(scope)
        val collection = accessor.collect<BaseType>()
        assertThat(collection).contains(firstInstance, secondInstance)
    }

    @Test
    fun `Instances of same final type can be collected`() {
        val module = module {
            collect<String> { "foo" }
            collect<String> { "bar" }
            collect<String> { "baz" }
        }
        val scope = scope("test-scope") {
            useModule(module)
        }
        val accessor = Scope.Accessor(scope)
        val collection = accessor.collect<String>()
        assertThat(collection).contains("foo", "bar", "baz")
    }

    @Test(expected = MissingCollectionFactoryException::class)
    fun `Throw an exception if items are not declared`() {
        val module = module {
            // no declarations
        }
        val scope = scope("test-scope") {
            useModule(module)
        }
        val accessor = Scope.Accessor(scope)
        accessor.collect<Any>()
    }

    @Test
    fun `Repeating dependency don't lead to item duplication`() {
        val coreScope = scope("core-scope") {
            useModule(module {
                collect<String> { "foo" }
            })
        }
        val settingsScope = scope("settings-scope") {
            dependsOn(coreScope)
            useModule(module {
                collect<String> { "bar" }
            })
        }
        val featureScope = scope("feature-scope") {
            dependsOn(coreScope)
            dependsOn(settingsScope)
        }

        class FeatureComponent : Component(featureScope) {
            fun getStrings() = collection<String>()
        }

        assertThat(FeatureComponent().getStrings()).containsExactlyInAnyOrder("foo", "bar")
    }

    @Test
    fun `Order of items corresponds to order of dependencies`() {
        val fooScope = scope("foo-scope") {
            useModule(module {
                collect<String> { "foo" }
            })
        }
        val barScope = scope("bar-scope") {
            useModule(module {
                collect<String> { "bar" }
            })
        }
        val featureScope = scope("feature-scope") {
            dependsOn(fooScope)
            dependsOn(barScope)
        }

        class FeatureComponent : Component(featureScope) {
            fun getStrings() = collection<String>()
        }

        assertThat(FeatureComponent().getStrings()).containsExactly("foo", "bar")
    }

    @Test
    fun `Items of top scope are placed to the end of collection`() {
        val fooScope = scope("foo-scope") {
            useModule(module {
                collect<String> { "foo" }
            })
        }
        val featureScope = scope("feature-scope") {
            dependsOn(fooScope)
            useModule(module {
                collect<String> { "bar" }
            })
        }

        class FeatureComponent : Component(featureScope) {
            fun getStrings() = collection<String>()
        }

        assertThat(FeatureComponent().getStrings()).containsExactly("foo", "bar")
    }
}
