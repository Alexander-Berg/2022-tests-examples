package com.yandex.mobile.realty.test

import org.junit.Test

/**
 * @author rogovalex on 17.08.2021.
 */
abstract class BaseTest {

    fun getTestRelatedFilePath(fileName: String): String {
        val stackTrace = Thread.currentThread().stackTrace
        val className = javaClass.name
        stackTrace.forEach { e ->
            if (e.className == className) {
                val methodName = e.methodName
                val method = javaClass.runCatching { getDeclaredMethod(methodName) }.getOrNull()
                if (method?.isAnnotationPresent(Test::class.java) == true) {
                    return "${javaClass.simpleName}/$methodName/$fileName"
                }
            }
        }
        throw NoSuchMethodException("Could not find test method")
    }
}
