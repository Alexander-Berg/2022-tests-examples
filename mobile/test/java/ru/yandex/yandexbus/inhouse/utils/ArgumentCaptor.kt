@file:Suppress("unused")

package ru.yandex.yandexbus.inhouse.utils

import org.mockito.ArgumentCaptor
import kotlin.reflect.KClass

/*
 * Copied from Mockito-Kotlin library
 * changes: returns (null as T) instead of "createInstance()" call
 */


/**
 * Creates a [KArgumentCaptor] for given type.
 */
inline fun <reified T : Any> argumentCaptor(): KArgumentCaptor<T> {
    return KArgumentCaptor(ArgumentCaptor.forClass(T::class.java), T::class)
}

/**
 * Creates a [KArgumentCaptor] for given type, taking in a lambda to allow fast verification.
 */
inline fun <reified T : Any> argumentCaptor(f: KArgumentCaptor<T>.() -> Unit): KArgumentCaptor<T> {
    return argumentCaptor<T>().apply(f)
}

/**
 * Creates a [KArgumentCaptor] for given nullable type.
 */
inline fun <reified T : Any> nullableArgumentCaptor(): KArgumentCaptor<T?> {
    return KArgumentCaptor(ArgumentCaptor.forClass(T::class.java), T::class)
}

/**
 * Creates a [KArgumentCaptor] for given nullable type, taking in a lambda to allow fast verification.
 */
inline fun <reified T : Any> nullableArgumentCaptor(f: KArgumentCaptor<T?>.() -> Unit): KArgumentCaptor<T?> {
    return nullableArgumentCaptor<T>().apply(f)
}

class KArgumentCaptor<out T : Any?>(
    private val captor: ArgumentCaptor<T>,
    private val tClass: KClass<*>
) {

    /**
     * The first captured value of the argument.
     * @throws IndexOutOfBoundsException if the value is not available.
     */
    val firstValue: T
        get() = captor.firstValue

    /**
     * The second captured value of the argument.
     * @throws IndexOutOfBoundsException if the value is not available.
     */
    val secondValue: T
        get() = captor.secondValue

    /**
     * The third captured value of the argument.
     * @throws IndexOutOfBoundsException if the value is not available.
     */
    val thirdValue: T
        get() = captor.thirdValue

    /**
     * The last captured value of the argument.
     * @throws IndexOutOfBoundsException if the value is not available.
     */
    val lastValue: T
        get() = captor.lastValue

    val allValues: List<T>
        get() = captor.allValues

    @Suppress("UNCHECKED_CAST")
    fun capture(): T {
        return captor.capture() ?: /*createInstance(tClass)*/ null as T
    }
}

val <T> ArgumentCaptor<T>.firstValue: T
    get() = allValues[0]

val <T> ArgumentCaptor<T>.secondValue: T
    get() = allValues[1]

val <T> ArgumentCaptor<T>.thirdValue: T
    get() = allValues[2]

val <T> ArgumentCaptor<T>.lastValue: T
    get() = allValues.last()