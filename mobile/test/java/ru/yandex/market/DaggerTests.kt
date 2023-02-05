package ru.yandex.market

import dagger.Component
import dagger.Module
import dagger.Provides
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DaggerTests {

    @Test
    fun `Component override dependencies when extends another component`() {
        val greeting = DaggerDaggerTests_ComponentB.create().greeting()

        assertThat(greeting).isEqualTo("Hello Buddy!")
    }

    @Component(modules = [ModuleA::class])
    interface ComponentA {

        fun greeting(): String
    }

    @Component(modules = [ModuleB::class])
    interface ComponentB : ComponentA

    @Module
    abstract class ModuleA {

        companion object {

            @Provides
            fun provideGreeting() = "Hello World!"
        }
    }

    @Module
    abstract class ModuleB {

        companion object {

            @Provides
            fun provideGreeting() = "Hello Buddy!"
        }
    }
}