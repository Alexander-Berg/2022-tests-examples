package ru.yandex.vertis.passport.dao.impl.composite

import ru.yandex.vertis.passport.dao.impl.memory.InMemoryConfirmationDao
import ru.yandex.vertis.passport.dao.{ConfirmationDao, ConfirmationDaoSpec}
import ru.yandex.vertis.passport.model.Confirmation
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer

import scala.concurrent.ExecutionContext

class CompositeConfirmationDaoSpec extends ConfirmationDaoSpec {

  implicit protected val ec: ExecutionContext = ExecutionContext.global
  val oldDao = new InMemoryConfirmationDao
  val newDao = new InMemoryConfirmationDao

  val compCode = ModelGenerators.identityConfirmationCode.next
  val compPayload = ModelGenerators.confirmationPayload.next

  val compConfirmation = Confirmation(code, payload)

  override val confirmationDao: ConfirmationDao = new CompositeConfirmationDao(oldDao, newDao)

  "CompositeConfirmationDao" should {
    "transfer data from old to new" in {
      val confirmation = compConfirmation
      oldDao.upsert(confirmation, ttl).futureValue
      confirmationDao.get(confirmation.code).futureValue shouldBe confirmation
      Thread.sleep(1000); //writing to new dao is async
      newDao.get(confirmation.code).futureValue shouldBe confirmation
    }
  }
}
