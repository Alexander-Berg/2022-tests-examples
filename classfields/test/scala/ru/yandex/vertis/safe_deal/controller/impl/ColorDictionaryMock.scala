package ru.yandex.vertis.safe_deal.controller.impl

import ru.yandex.vertis.safe_deal.dictionary.ColorDictionary

class ColorDictionaryMock() extends ColorDictionary.Service {
  import ColorDictionaryMock._

  override def getColorTextByHex(hex: String): Option[String] = colors.get(hex)
}

object ColorDictionaryMock {

  private val colors = Map(
    "0000CC" -> "Синий"
  )
}
