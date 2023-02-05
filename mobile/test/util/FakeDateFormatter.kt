package ru.yandex.market.test.util

import ru.yandex.market.utils.LocaleUtils
import java.text.SimpleDateFormat
import java.util.Date

class FakeDateFormatter {

    fun formatLong(date: Date): String {
        val simpleDateFormatter = SimpleDateFormat("d MMMM, EEEE,", LocaleUtils.russian())
        return simpleDateFormatter.format(date)
    }

    fun formatShort(date: Date): String {
        val simpleDateFormatter = SimpleDateFormat("d MMMM", LocaleUtils.russian())
        return simpleDateFormatter.format(date)
    }

    fun formatShortWithNbsp(date: Date): String {
        val simpleDateFormatter = SimpleDateFormat("d\u00A0MMMM", LocaleUtils.russian())
        return simpleDateFormatter.format(date)
    }

    fun formatShortWithDay(date: Date): String {
        val simpleDateFormatter = SimpleDateFormat("E, d\u00A0MMMM", LocaleUtils.russian())
        return simpleDateFormatter.format(date)
    }

    fun formatNumericShort(date: Date): String {
        val simpleDateFormatter = SimpleDateFormat("dd.MM", LocaleUtils.russian())
        return simpleDateFormatter.format(date)
    }
}