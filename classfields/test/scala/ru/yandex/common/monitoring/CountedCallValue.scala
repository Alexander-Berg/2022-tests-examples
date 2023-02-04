package ru.yandex.common.monitoring

import java.util.concurrent.atomic.AtomicInteger

/**
 * @author evans
 */
/**
 * Helper class for counting by-name parameter access.
 * @param a - value to access
 * @tparam A - value to access
 */
class CountedCallValue[A](a: A) {
  private val counter = new AtomicInteger

  def get = {
    counter.incrementAndGet()
    a
  }

  def count = counter.get()
}
