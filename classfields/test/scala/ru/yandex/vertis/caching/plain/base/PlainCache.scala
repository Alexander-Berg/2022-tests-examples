package ru.yandex.vertis.caching.plain.base

import scala.concurrent.duration.Duration

/**
  * @author korvit
  */
trait PlainCache[K, V] {

  def get(key: K): Option[V]

  def multiGet(keys: Set[K]): Map[K, V]

  def set(key: K,
          value: V,
          expire: Duration): Unit

  def multiSet(entries: Map[K, V],
               expire: Duration): Unit

  def delete(key: K): Unit

  def multiDelete(keys: Set[K]): Unit
}
