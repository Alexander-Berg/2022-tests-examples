package ru.yandex.vertis.caching.base.impl

import java.nio.ByteBuffer

import ru.yandex.vertis.caching.base.layout.Layout

import scala.util.Try

/**
  * @author korvit
  */
class StringIntLayout
  extends Layout[String, Int] {

  override def key(key: String): String = key

  override def encode(value: Int): Array[Byte] = ByteBuffer.allocate(4).putInt(value).array()

  override def decode(bytes: Array[Byte]): Try[Int] = Try(ByteBuffer.wrap(bytes).getInt)
}
