package ru.auto.salesman.api.v1

import java.io.Closeable

import org.apache.commons.io.IOUtils

/** Copy-paste from public-api */
object IO {

  def using[T <: Closeable, R](closeable: => T)(action: T => R): R = {
    val obj = closeable
    try action(obj)
    finally IOUtils.closeQuietly(obj)
  }
}
