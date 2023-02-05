package ru.yandex.market.test.extensions

import com.annimon.stream.Exceptional
import com.annimon.stream.OptionalBoolean
import com.annimon.stream.OptionalLong
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.BaseTestConsumer
import io.reactivex.schedulers.TestScheduler
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Assert
import org.mockito.invocation.InvocationOnMock
import ru.yandex.market.test.matchers.hasFeature
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

fun <T> errorExceptional(): Exceptional<T> = Exceptional.of<T>(RuntimeException())

fun <T> instanceOf(clazz: KClass<*>): Matcher<T> = Matchers.instanceOf(clazz.java)

inline fun <reified T> instanceOf(): Matcher<out T> = Matchers.instanceOf(T::class.java)

val Long.asOptional: OptionalLong
    get() = OptionalLong.of(this)

val Long?.asOptional: OptionalLong
    get() = OptionalLong.ofNullable(this)

val Boolean?.asOptional: OptionalBoolean
    get() = OptionalBoolean.ofNullable(this)

fun <T : Any> T.asObservable(): Observable<T> = Observable.just(this)

fun <T : Any> T.asSingle(): Single<T> = Single.just(this)

fun <T : Any> T.asStream(): Observable<T> = Observable.never<T>().startWith(this)

fun <T : Any> Iterable<T>.flattenAsStream(): Observable<T> = Observable.never<T>().startWith(this)

inline fun <reified T : Any> checkArg(noinline predicate: (T) -> Unit) =
    org.mockito.kotlin.check(predicate)

operator fun <T> Matcher<T>.not(): Matcher<T> = Matchers.not(this)

inline fun <reified T> InvocationOnMock.arg(index: Int = 0): T {
    return arguments[index] as T
}

fun TestScheduler.triggerAndAdvanceTimeTo(delayTime: Long, unit: TimeUnit) {
    triggerActions()
    advanceTimeTo(delayTime, unit)
}

fun TestScheduler.triggerAndAdvanceTimeBy(delayTime: Long, unit: TimeUnit) {
    triggerActions()
    advanceTimeBy(delayTime, unit)
}

fun <T, U : BaseTestConsumer<T, U>> BaseTestConsumer<T, U>.assertValue(matcher: Matcher<T>): U {
    return assertValueCount(1).apply { Assert.assertThat(values().first(), matcher) }
}

fun <T, R, U : BaseTestConsumer<T, U>> BaseTestConsumer<T, U>.assertValue(
    getter: T.() -> R,
    matcher: Matcher<R>,
    description: String = ""
): U = assertValue(hasFeature(getter, matcher, description))

fun <T, U : BaseTestConsumer<T, U>> BaseTestConsumer<T, U>.assertValue(
    expectation: String = "",
    predicate: T.() -> Boolean
): U = assertValue(hasFeature(expectation, predicate))

/**
 * Нужно потому что вывод типов в Kotlin 1.4.10 иногда тупит и пытается скастить null к какому-нибудь
 * неправильному типу в параметрических тестах
 */
val nullAny: Any? get() = null