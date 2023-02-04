package ru.yandex.vos2.autoru.utils

import java.util

import scala.jdk.CollectionConverters._

/**
  * Класс для тестов генерирует случайные значения, которые можно узнать заранее, чтобы потом сравнить.
  *
  * @tparam T - тип нужных случайных значений
  */
abstract class AheadAwareRandomGenerator[T] {
  protected def genValue: T

  private val nxtV: util.LinkedList[T] = new util.LinkedList[T]()
  nxtV.add(genValue)

  /**
    * Метод возвращает значения, которые будут возвращены следующими при вызове generateValue.
    * При вызове nextValue больше одного раза, будет сгенерировано столько значений, сколько раз он был вызван,
    * и все эти значения будут возвращены потом при вызове generateValue в том же порядке.
    *
    * @return
    */
  def nextValue: T = {
    try {
      nxtV.getLast
    } finally {
      nxtV.add(genValue)
    }
  }

  /**
    * Возвращает следующее случайное значение
    *
    * @return
    */
  def generateValue: T = {
    if (nxtV.isEmpty) {
      nxtV.add(genValue)
    }
    try {
      nxtV.remove(0)
    } finally {
      if (nxtV.isEmpty) {
        nxtV.add(genValue)
      }
    }
  }

  def clear(): Unit = {
    nxtV.clear()
  }

  def printAll: String = {
    nxtV.asScala.mkString(", ")
  }
}
