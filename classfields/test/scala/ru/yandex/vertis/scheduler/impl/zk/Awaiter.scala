package ru.yandex.vertis.scheduler.impl.zk

import java.util.concurrent.CountDownLatch

import ru.yandex.vertis.curator.recipes.map.{Event, EventListener}

/**
 * Awaits something
 *
 * @author dimas
 */
class Awaiter[A, B]
  extends EventListener[Event[A, B]] {

  private val cdl = new CountDownLatch(1)

  def onEvent(event: Event[A, B]): Unit = {
    cdl.countDown()
  }

  def await() = cdl.await()
}
