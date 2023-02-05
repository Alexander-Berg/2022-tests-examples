package toxin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FactoryTest {

    private class Instance {
        val id: Int = nextInstanceId++

        companion object {
            private var nextInstanceId = 1
        }
    }

    @Test
    fun `Factory creates new instance each time`() {
        val module = module {
            factory<Instance> { Instance() }
        }
        val scope = scope("test-scope") {
            useModule(module)
        }
        val accessor = Scope.Accessor(scope)
        val calls = 100
        val instances = (0 until calls).map {
            accessor.get<Instance>()
        }
        val ids = instances.map { instance -> instance.id }.toSet()
        assertThat(ids.size).isEqualTo(instances.size).isEqualTo(calls)
    }

    @Test
    fun `Singleton creates single instance`() {
        val module = module {
            singleton<Instance> { Instance() }
        }
        val scope = scope("test-scope") {
            useModule(module)
        }
        val accessor = Scope.Accessor(scope)
        val instances = (0 until 100).map {
            accessor.get<Instance>()
        }
        val ids = instances.map { instance -> instance.id }.toSet()
        assertThat(ids.size).isEqualTo(1)
    }

    @Test
    fun `Reusable creates less instances then calls`() {
        val module = module {
            reusable<Instance> { Instance() }
        }
        val scope = scope("test-scope") {
            useModule(module)
        }
        val accessor = Scope.Accessor(scope)
        val instances = (0 until 100).map {
            accessor.get<Instance>()
        }
        val ids = instances.map { instance -> instance.id }.toSet()
        assertThat(ids.size).isLessThan(instances.size)
    }

    @Test
    fun `Singleton clears instance on scope clear`() {
        val module = module {
            singleton<Instance> { Instance() }
        }
        val scope = scope("test-scope") {
            useModule(module)
        }
        val accessor = Scope.Accessor(scope)
        val calls = 100
        val instances = (0 until calls).map {
            scope.clear()
            accessor.get<Instance>()
        }
        val ids = instances.map { instance -> instance.id }.toSet()
        assertThat(ids.size).isEqualTo(instances.size).isEqualTo(calls)
    }

    @Test
    fun `Reusable clears instance on scope clear`() {
        val module = module {
            reusable<Instance> { Instance() }
        }
        val scope = scope("test-scope") {
            useModule(module)
        }
        val accessor = Scope.Accessor(scope)
        val calls = 100
        val instances = (0 until calls).map {
            scope.clear()
            accessor.get<Instance>()
        }
        val ids = instances.map { instance -> instance.id }.toSet()
        assertThat(ids.size).isEqualTo(instances.size).isEqualTo(calls)
    }
}
