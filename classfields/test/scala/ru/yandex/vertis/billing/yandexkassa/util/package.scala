package ru.yandex.vertis.billing.yandexkassa

/**
  * Test utils
  *
  * @author alesavin
  */
package object util {

  def read(path: String) = getClass.getResourceAsStream(path)

  def readAsBytes(path: String): Array[Byte] =
    scala.io.Source.fromInputStream(getClass.getResourceAsStream(path)).mkString.getBytes
}
