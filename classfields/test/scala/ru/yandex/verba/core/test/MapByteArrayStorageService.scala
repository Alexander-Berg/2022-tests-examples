package ru.yandex.verba.core.test

import ru.yandex.common.services.fs.ByteArrayStorageService

import scala.annotation.nowarn
import scala.collection.mutable

/**
  * TODO
  */
class MapByteArrayStorageService extends ByteArrayStorageService {
  private val map = mutable.HashMap.empty[String, Array[Byte]]

  def throwNotFound(key: String) = throw new NoSuchElementException(key)

  def put(key: String, bytes: Array[Byte]): Unit = {
    map.put(key, bytes): @nowarn("cat=w-flag-value-discard")
  }

  def get(key: String): Array[Byte] = map.getOrElse(key, throwNotFound(key))

  def get(key: String, retryIfNotFound: Boolean): Array[Byte] = get(key)

  def exists(key: String): Boolean = map.contains(key)

  def delete(key: String): Boolean = map.remove(key).isDefined
}
