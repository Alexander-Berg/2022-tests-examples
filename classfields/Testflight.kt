package ru.yandex.mobile.realty.utils

object Testflight {

    private const val INTERNAL_TESTING_TEMPLATE = "https://testflight.apple.com/v1/app/1020247568"

    fun url(): String {
        return INTERNAL_TESTING_TEMPLATE
    }
}
