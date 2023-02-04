package ru.auto.testextension

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Condition
import org.assertj.core.presentation.PredicateDescription
import java.util.function.Predicate
import kotlin.reflect.KProperty1

@Suppress("NewApi") // this is a test code
fun <T> condition(description: String, predicate: (T) -> Boolean) =
    Condition(Predicate<T> { predicate(it) }, description)

fun <SELF, T> withPropertyValue(prop: KProperty1<SELF, T>, value: T) =
    condition<SELF>("Property ${prop.name} equals value: $value") {
        prop.get(it) == value
    }

inline fun <reified T> AbstractAssert<*, T?>.matchesNotNullWithRepresentation(
    predicateDescription: String?,
    crossinline representationMessageBuilder: (T) -> String,
    crossinline predicate: T.() -> Boolean,
) = apply { withRepresentationSafeType(representationMessageBuilder).matchesNotNull(predicateDescription, predicate) }

inline fun <reified T> AbstractAssert<*, T?>.withRepresentationSafeType(
    crossinline message: (T) -> String,
) = apply {
    withRepresentation { item ->
        when (item) {
            is T -> message(item)
            is PredicateDescription -> item.description
            else -> item.toString()
        }
    }
}

inline fun <T> AbstractAssert<*, T?>.matchesNotNull(
    message: String? = null,
    crossinline predicate: T.() -> Boolean,
) = message?.let { matches({ item -> item?.let(predicate) ?: false }, it) }
    ?: matches { item -> item?.let(predicate) ?: false }
