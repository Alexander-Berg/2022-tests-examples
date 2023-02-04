package ru.yandex.vertis.subscriptions.storage

import com.google.common.base.Charsets
import com.google.common.primitives.Longs

import scala.util.Try

trait KeyValueTemplateConverters {

  implicit val LongKeyValueConvertible = new Format[Long] {

    override def wrap(value: Long): Array[Byte] =
      Longs.toByteArray(value)

    override def unwrap(value: Array[Byte]): Try[Long] = Try {
      Longs.fromByteArray(value)
    }

  }

  implicit val StringKeyValueConvertible = new Format[String] {

    override def wrap(value: String): Array[Byte] =
      value.getBytes(Charsets.UTF_8)

    override def unwrap(value: Array[Byte]): Try[String] = Try {
      new String(value, Charsets.UTF_8)
    }

  }

}
