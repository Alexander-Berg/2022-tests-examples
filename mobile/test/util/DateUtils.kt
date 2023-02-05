package ru.yandex.market.test.util

import java.util.Calendar

object DateUtils {

    fun getWeekDayNameNominative(number: Int): String {
        return when (number) {
            Calendar.SUNDAY -> "воскресение"
            Calendar.MONDAY -> "понедельник"
            Calendar.TUESDAY -> "вторник"
            Calendar.WEDNESDAY -> "среда"
            Calendar.THURSDAY -> "четверг"
            Calendar.FRIDAY -> "пятница"
            Calendar.SATURDAY -> "суббота"
            else -> throw IllegalArgumentException("The number is expected to be one of Calendar.SUNDAY - Calendar.SATURDAY but is $number")
        }
    }

    fun getWeekDayNameAccusative(number: Int): String {
        val weekDay = getWeekDayNameNominative(number)
        return if (weekDay.last() == 'а') {
            weekDay.substring(0, weekDay.length - 1) + 'у'
        } else {
            weekDay
        }
    }

    fun getMonthNameNominative(number: Int): String {
        return when (number) {
            Calendar.JANUARY -> "январь"
            Calendar.FEBRUARY -> "февраль"
            Calendar.MARCH -> "март"
            Calendar.APRIL -> "апрель"
            Calendar.MAY -> "май"
            Calendar.JUNE -> "июнь"
            Calendar.JULY -> "июль"
            Calendar.AUGUST -> "август"
            Calendar.SEPTEMBER -> "сентябрь"
            Calendar.OCTOBER -> "октябрь"
            Calendar.NOVEMBER -> "ноябрь"
            Calendar.DECEMBER -> "декабрь"
            else -> throw IllegalArgumentException("The number is expected to be one of Calendar.JANUARY - Calendar.DECEMBER but is $number")
        }
    }

    fun getMonthNameGenitive(number: Int): String {
        val month = getMonthNameNominative(number)
        return if (month.last() == 'т') {
            month + 'а'
        } else {
            month.substring(0, month.length - 1) + 'я'
        }
    }

}