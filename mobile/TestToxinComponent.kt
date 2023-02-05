package toxin.benchmarks.subject

import toxin.Component

class TestToxinComponent : Component(testScope) {

    fun getTestType() = auto<TestType>()

    companion object {
        val instance = TestToxinComponent()
    }
}