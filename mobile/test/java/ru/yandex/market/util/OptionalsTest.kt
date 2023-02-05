package ru.yandex.market.util

import com.annimon.stream.Exceptional
import com.annimon.stream.Optional
import com.annimon.stream.test.hamcrest.OptionalIntMatcher
import com.annimon.stream.test.hamcrest.OptionalIntMatcher.hasValue
import com.annimon.stream.test.hamcrest.OptionalMatcher
import com.annimon.stream.test.hamcrest.OptionalMatcher.hasValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.HamcrestCondition
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.internal.function.ThrowableToIntFunction

@RunWith(MockitoJUnitRunner::class)
class OptionalsTest {

    @Test
    fun `Flat map throwable return empty optional when mapper throws exception`() {
        val optional = Optional.of("value")
            .custom<Optional<String>>(Optionals.safeFlatMap { throw RuntimeException() })

        assertThat(optional).`is`(HamcrestCondition(OptionalMatcher.isEmpty()))
    }

    @Test
    fun `Flat map throwable return present optional when there is no exceptions`() {
        val value = "value"

        val optional = Optional.of(value)
            .custom<Optional<String>>(Optionals.safeFlatMap { Optional.of(it) })

        assertThat(optional).has(HamcrestCondition(hasValue(value)))
    }

    @Test
    fun `Flat map throwable returns empty optional when upstream optional is empty`() {
        val optional = Optional.empty<String>()
            .custom(Optionals.safeFlatMap<String, String> { Optional.of(it) })

        assertThat(optional).`is`(HamcrestCondition(OptionalMatcher.isEmpty()))
    }

    @Test
    fun `Safe map to int throwable return present OptionalInt when mapping is successful`() {
        val value = 1

        val optionalInt = Optional.of("insignificant")
            .custom(
                Optionals.safeMapToInt(
                    object : ThrowableToIntFunction<String> {
                        override fun applyAsInt(s: String): Int {
                            return value
                        }
                    })
            )

        assertThat(optionalInt).has(HamcrestCondition(hasValue(value)))
    }

    @Test
    fun `Safe map to int throwable return empty OptionalInt when mapping throws exception`() {
        val optionalInt = Optional.of("insignificant")
            .custom(
                Optionals.safeMapToInt(
                    object : ThrowableToIntFunction<String> {
                        override fun applyAsInt(value: String): Int {
                            throw RuntimeException()
                        }
                    })
            )

        assertThat(optionalInt).`is`(HamcrestCondition(OptionalIntMatcher.isEmpty()))
    }

    @Test
    fun `Safe map to int throwable return present OptionalInt when upstream optional is empty`() {
        val optionalInt = Optional.empty<Any>()
            .custom(
                Optionals.safeMapToInt(
                    object : ThrowableToIntFunction<Any> {
                        override fun applyAsInt(value: Any): Int {
                            return 1
                        }
                    })
            )

        assertThat(optionalInt).`is`(HamcrestCondition(OptionalIntMatcher.isEmpty()))
    }

    @Test
    fun `Skips exceptions return empty optional when exceptional contains error`() {
        val testOptional = Optional.of(Exceptional.of<String>(RuntimeException()))
            .custom(Optionals.errorToEmpty())

        assertThat(testOptional).`is`(HamcrestCondition(OptionalMatcher.isEmpty()))
    }

    @Test
    fun `Skips exceptions return present optional when exceptional contains value`() {
        val value = "value"

        val testOptional = Optional.of(Exceptional.of { value })
            .custom(Optionals.errorToEmpty())

        assertThat(testOptional).has(HamcrestCondition(hasValue(value)))
    }

    @Test
    fun `Error to empty return empty optional for error`() {
        val errorOptional = Optional.of(Exceptional.of<Any>(RuntimeException()))
            .custom(Optionals.errorToEmpty())

        assertThat(errorOptional).`is`(HamcrestCondition(OptionalMatcher.isEmpty()))
    }

    @Test
    fun `Error to empty return present optional when no errors`() {
        val errorOptional = Optional.of(Exceptional.of { Object() })
            .custom(Optionals.errorToEmpty())

        assertThat(errorOptional).`is`(HamcrestCondition(OptionalMatcher.isPresent()))
    }

    @Test
    fun `ifAbsent executes passed action when optional is empty`() {
        val action = mock<Runnable>()

        Optional.empty<String>().custom(Optionals.ifAbsent(action))

        verify(action).run()
    }

    @Test
    fun `ifAbsent don't executes passed action when optional is present`() {
        val action = mock<Runnable>()

        Optional.of("").custom(Optionals.ifAbsent(action))

        verify(action, never()).run()
    }

    @Test
    fun `ifAbsent returns Unit`() {
        val result = Optional.empty<String>().custom(Optionals.ifAbsent {})

        assertThat(result).isEqualTo(Unit)
    }
}