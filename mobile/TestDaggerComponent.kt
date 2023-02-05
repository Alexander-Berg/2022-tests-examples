package toxin.benchmarks.subject

import dagger.Component

@Component(modules = [TestDaggerModule::class])
interface TestDaggerComponent {

    fun getTestType(): TestType

    companion object {
        val instance = DaggerTestDaggerComponent.create()
    }
}