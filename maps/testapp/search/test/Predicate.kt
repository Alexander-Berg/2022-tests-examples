package com.yandex.maps.testapp.search.test

abstract class Predicate<T> {
    operator fun invoke(value: T?): Boolean {
        actual = value
        return check()
    }

    abstract fun errorMessage(): String
    abstract fun check(): Boolean

    protected var actual: T? = null
}

class IsNull<T>: Predicate<T>() {
    override fun check() = (actual == null)
    override fun errorMessage() = "Value IS NOT null"
}

class IsNotNull<T>: Predicate<T>() {
    override fun check() = (actual != null)
    override fun errorMessage() = "Value IS null"
}

class GreaterThan<T : Comparable<T>>(private val threshold: T): Predicate<T>() {
    override fun check() = (actual != null) && (actual!! > threshold)
    override fun errorMessage() = "$actual IS NOT GREATER than $threshold"
}

class GreaterOrEqualTo<T : Comparable<T>>(private val threshold: T): Predicate<T>() {
    override fun check() = (actual != null) && (actual!! >= threshold)
    override fun errorMessage() = "$actual IS LESS than $threshold"
}

class LessThan<T : Comparable<T>>(private val threshold: T): Predicate<T>() {
    override fun check() = (actual != null) && (actual!! < threshold)
    override fun errorMessage() = "$actual IS NOT LESS than $threshold"
}

class LessOrEqualTo<T : Comparable<T>>(private val threshold: T): Predicate<T>() {
    override fun check() = (actual != null) && (actual!! <= threshold)
    override fun errorMessage() = "$actual IS GREATER than $threshold"
}

class EqualTo<T>(private val expected: T): Predicate<T>() {
    override fun check() = (actual == expected)
    override fun errorMessage() = "$actual != $expected"
}
typealias Is<T> = EqualTo<T>

class NotEqualTo<T>(private val expected: T?): Predicate<T>() {
    override fun check() = actual != expected
    override fun errorMessage() = "$actual == $expected"
}
typealias IsNot<T> = NotEqualTo<T>

class IsNotEmptyList<T : Collection<*>>: Predicate<T>() {
    override fun check() = (actual != null) && (actual!!.count() > 0)
    override fun errorMessage() = "Empty list: $actual"
}

class IsNotEmpty: Predicate<String>() {
    override fun check() = (actual != null) && (actual!!.isNotEmpty())
    override fun errorMessage() = "Empty string: $actual"
}

class ContainsAnyOf(private vararg val strings: String): Predicate<String>() {
    override fun check() = strings.any{ actual?.contains(it) == true }
    override fun errorMessage() = "$actual DOES NOT CONTAIN ANY OF (${strings.joinToString()})"
}

class IsOneOf<T>(private vararg val expected: T): Predicate<T>() {
    override fun check() = (actual in expected)
    override fun errorMessage() = "$actual IS NOT IN (${expected.joinToString()})"
}
