package ru.yandex.vertis.feedprocessor.util

import java.io._

import com.google.common.base.Charsets
import com.google.protobuf.Message
import org.apache.commons.io.IOUtils

import scala.reflect.ClassTag

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 16.02.17
  */
object Resources {

  def open[T](path: String)(f: InputStream => T): T = {
    using(
      Option(
        getClass.getResourceAsStream(path)
      ).getOrElse(throw new IOException(s"Resource $path not found"))
    )(f)
  }

  def toString(path: String): String = {
    open(path)(IOUtils.toString(_, Charsets.UTF_8))
  }

  def toProto[T <: Message: ClassTag](path: String): T = {
    Protobuf.fromJson[T] {
      open(path)(IOUtils.toString(_, Charsets.UTF_8))
    }
  }

  def using[T <: Closeable, R](closeable: => T)(action: T => R): R = {
    val obj = closeable
    try action(obj)
    finally IOUtils.closeQuietly(obj)
  }
}
