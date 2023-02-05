package ru.yandex.market.test.util

import android.util.Log

fun Any.error(message: String, throwable: Throwable? = null) = log(Log.ERROR, message, throwable)

fun Any.warn(message: String, throwable: Throwable? = null) = log(Log.WARN, message, throwable)

fun Any.info(message: String, throwable: Throwable? = null) = log(Log.INFO, message, throwable)

fun Any.verbose(message: String, throwable: Throwable? = null) =
    log(Log.VERBOSE, message, throwable)

fun Any.debug(message: String, throwable: Throwable? = null) = log(Log.DEBUG, message, throwable)

private fun Any.log(level: Int, message: String, throwable: Throwable?) {
    Log.println(level, javaClass.simpleName, "$message\n${Log.getStackTraceString(throwable)}")

}
