package toxin.benchmarks.subject

import toxin.module
import toxin.scope

val testScope = scope("test-scope") {
    useModule(module {
        factory<TestType> { TestType() }
    })
}
