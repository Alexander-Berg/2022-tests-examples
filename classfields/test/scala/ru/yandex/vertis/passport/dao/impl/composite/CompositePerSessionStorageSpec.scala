package ru.yandex.vertis.passport.dao.impl.composite

import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import ru.yandex.vertis.passport.dao.impl.memory.InMemoryPerSessionStorage
import ru.yandex.vertis.passport.dao.{PerSessionStorage, PerSessionStorageSpec}
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer

import scala.concurrent.ExecutionContext

class CompositePerSessionStorageSpec extends PerSessionStorageSpec {

  implicit protected val ec: ExecutionContext = ExecutionContext.global
  val oldStorage = new InMemoryPerSessionStorage
  val newStorage = new InMemoryPerSessionStorage

  override val perSessionStorage: PerSessionStorage = new CompositePerSessionStorage(oldStorage, newStorage)

  "CompositePerSessionStorage" should {
    "transfer data from old to new" in {
      val session = ModelGenerators.session.next
      oldStorage.save(session.id, "qualifier", "value", 10.seconds).futureValue
      val res = perSessionStorage.get(session.id, "qualifier").futureValue
      res.get shouldBe "value"
      Thread.sleep(1000); //writing to new dao is async
      val newRes = newStorage.get(session.id, "qualifier").futureValue
      newRes.get shouldBe "value"
    }
  }

}
