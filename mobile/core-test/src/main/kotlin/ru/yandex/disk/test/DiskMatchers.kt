package ru.yandex.disk.test

import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.invocation.InvocationOnMock
import ru.yandex.disk.remote.RemoteRepoOnNext

/**
 * Container for Matchers related to Disk.
 */
object DiskMatchers {

    inline fun <reified T: Any> argOfClass(tClass: Class<T>): T {
        return argThat { javaClass === tClass }
    }

    fun <T> returnAndProvide(result: T): (InvocationOnMock) -> T {
        return returnAndProvide(result, *arrayOfNulls<Any>(0))
    }

    fun <T, I> returnAndProvide(result: T, vararg items: I): (InvocationOnMock) -> T {
        return { invocation: InvocationOnMock ->
            for (arg in invocation.arguments) {
                if (arg is RemoteRepoOnNext<*, *>) {
                    val onNext = arg as RemoteRepoOnNext<I, *>
                    for (item in items) {
                        onNext.onNext(item)
                    }
                }
            }
            result
        }
    }

    fun <T, I> provide(vararg items: I): (InvocationOnMock) -> T? {
        return returnAndProvide(null, *items)
    }

    fun <I> provideUnit(vararg items: I): (InvocationOnMock) -> Unit {
        return returnAndProvide(Unit, *items)
    }

    fun <T> anyListOnNext(): RemoteRepoOnNext<List<T>, RuntimeException> {
        return any()
    }
}
