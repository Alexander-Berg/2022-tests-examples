package ru.yandex.vertis.telepony.dummy

import ru.yandex.vertis.telepony.dummy.InMemoryHydraClient.{InMemoryClicker, InMemoryCounter}
import ru.yandex.vertis.telepony.hydra.HydraClient
import HydraClient.{Clicker, Counter, Limiter}

import scala.concurrent.Future

/**
  * @author evans
  */
class InMemoryHydraClient extends HydraClient {

  private var counters = Map.empty[String, Counter].withDefault(_ => new InMemoryCounter)

  private var clickers = Map.empty[String, Clicker].withDefault(_ => new InMemoryClicker)

  override def limiter(component: String): Limiter = ???

  override def counter(component: String): Counter = {
    val res = counters(component)
    counters = counters + (component -> res)
    res
  }

  override def clicker(component: String): Clicker = {
    val res = clickers(component)
    clickers = clickers + (component -> res)
    res
  }

}

object InMemoryHydraClient {

  class InMemoryCounter extends Counter {

    private var values = Map.empty[String, Set[String]].withDefaultValue(Set.empty)

    override def get(key: String): Future[Int] =
      Future.successful(values(key).size)

    override def incrementAndGet(key: String, value: String): Future[Int] = Future.successful {
      val nextValue = values(key) + value
      values = values + (key -> nextValue)
      nextValue.size
    }
  }

  class InMemoryClicker extends Clicker {
    private var values = Map.empty[String, Int].withDefaultValue(0)

    override def get(key: String): Future[Int] =
      Future.successful(values(key))

    override def incrementAndGet(key: String): Future[Int] = Future.successful {
      val nextValue = values(key) + 1
      values = values + (key -> nextValue)
      nextValue
    }
  }

}
