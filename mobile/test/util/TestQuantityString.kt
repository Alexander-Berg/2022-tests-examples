package ru.yandex.market.test.util

class TestQuantityString(
    private val zero: String,
    private val one: String,
    private val few: String,
    private val many: String
) {

    fun getQuantityString(quantity: Int): String {
        if (quantity == 0) return zero
        return when (val end = quantity % 100) {
            11, 12, 13, 14 -> many
            else ->
                when (end % 10) {
                    1 -> one
                    2, 3, 4 -> few
                    else -> many
                }
        }
    }
}