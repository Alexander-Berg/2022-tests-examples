package ru.yandex.vertis.moderation.dao.impl.inmemory

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.dao.{InstanceDao, InstanceDaoSpecBase}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Specs on [[InMemoryInstanceDao]]
  *
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class InMemoryInstanceDaoSpec extends InstanceDaoSpecBase {

  private val storage = new InMemoryStorageImpl

  override val instanceDao: InstanceDao[Future] = new InMemoryInstanceDao(ServiceGen.next, storage)

  before {
    storage.clear()
  }
}
