package ru.yandex.market.processor.testinstance

import dagger.BindsInstance
import dagger.Component
import dagger.MembersInjector
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Singleton

@Component(modules = [ProcessorModule::class])
@Singleton
interface ProcessorComponent : MembersInjector<TestInstanceProcessor> {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun processingEnvironment(value: ProcessingEnvironment): Builder

        fun build(): ProcessorComponent
    }
}