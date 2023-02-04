package ru.yandex.vertis.passport.dao

import org.scalatest.WordSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import ru.yandex.vertis.passport.model.Confirmation
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

import scala.concurrent.duration.DurationDouble

/**
  * tests for [[ConfirmationDao]]
  *
  * @author zvez
  */
trait ConfirmationDaoSpec extends WordSpec with SpecBase with GeneratorDrivenPropertyChecks {

  val confirmationDao: ConfirmationDao

  val code = ModelGenerators.identityConfirmationCode.next
  val payload = ModelGenerators.confirmationPayload.next

  val confirmation = Confirmation(code, payload)

  val ttl = 10.minutes

  "ConfirmationDao" should {

    "create confirmation" in {
      confirmationDao.upsert(confirmation, ttl).futureValue
    }

    "get confirmation" in {
      confirmationDao.get(code).futureValue shouldBe confirmation
      confirmationDao.get(code.copy(code = "something")).failed.futureValue shouldBe an[NoSuchElementException]
    }

    "delete confirmation" in {
      confirmationDao.delete(code).futureValue
      confirmationDao.get(code).failed.futureValue shouldBe an[NoSuchElementException]
    }
  }
}
