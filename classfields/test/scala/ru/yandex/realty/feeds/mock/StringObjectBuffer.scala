package ru.yandex.realty.feeds.mock

import ru.yandex.realty.feeds.services.ObjectBuffer

class StringObjectBuffer extends ObjectBuffer[String] {
  private val buffer = new StringBuilder

  override def println(s: String): Unit = {
    if (s.nonEmpty) {
      buffer.append(s)
      buffer.append("\n")
    }
  }

  override def get(): String = buffer.toString()
}
