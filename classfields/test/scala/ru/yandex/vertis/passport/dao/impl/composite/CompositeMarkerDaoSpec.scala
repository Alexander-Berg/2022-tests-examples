package ru.yandex.vertis.passport.dao.impl.composite

import ru.yandex.vertis.passport.dao.impl.memory.InMemoryMarkerDao
import ru.yandex.vertis.passport.dao.{MarkerDao, MarkerDaoSpec}
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer

import scala.concurrent.ExecutionContext

class CompositeMarkerDaoSpec extends MarkerDaoSpec {

  implicit protected val ec: ExecutionContext = ExecutionContext.global

  val oldDao = new InMemoryMarkerDao
  val newDao = new InMemoryMarkerDao

  override val dao: MarkerDao = new CompositeMarkerDao(oldDao, newDao)

  "CompositeMarkerDao" should {
    "transfer data from old to new" in {
      val key = ModelGenerators.readableString.next
      oldDao.mark(key, ttl).futureValue
      dao.hasMark(key).futureValue shouldBe true
      Thread.sleep(1000); //writing to new dao is async
      newDao.hasMark(key).futureValue shouldBe true
    }
  }
}
