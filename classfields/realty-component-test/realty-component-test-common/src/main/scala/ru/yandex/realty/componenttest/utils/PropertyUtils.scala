package ru.yandex.realty.componenttest.utils

object PropertyUtils {

  def setSystemPropertyIfAbsent(key: String, value: String): Unit =
    if (System.getProperty(key) == null) {
      System.setProperty(key, value)
    }

}
