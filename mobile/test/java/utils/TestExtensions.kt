package utils

import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.stubbing.OngoingStubbing

inline fun <reified T : Any> mock(): T = Mockito.mock(T::class.java)

inline fun <reified T : Any> mock(defaultAnswer: Answer<*>): T = Mockito.mock(T::class.java, defaultAnswer)

fun <T> T?.shouldReturn(any: T): OngoingStubbing<T?> = Mockito.`when`(this).thenReturn(any)

fun <T> T.shouldReturn(function: (InvocationOnMock) -> Any): OngoingStubbing<T> = Mockito.`when`(this).then(function)
