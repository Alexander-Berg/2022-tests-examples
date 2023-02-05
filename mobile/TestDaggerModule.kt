package toxin.benchmarks.subject

import dagger.Module
import dagger.Provides

@Module
class TestDaggerModule {

    @Provides
    fun provideTestType(): TestType = TestType()
}