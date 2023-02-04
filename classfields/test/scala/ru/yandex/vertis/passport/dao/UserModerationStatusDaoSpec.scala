package ru.yandex.vertis.passport.dao

import org.scalatest.WordSpec
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

/**
  *
  * @author zvez
  */
trait UserModerationStatusDaoSpec extends WordSpec with SpecBase {

  def dao: UserModerationStatusDao

  "UserModerationStatusDao.upsert" should {
    "insert new record" in {
      val userId = ModelGenerators.userId.next
      val status = ModelGenerators.userModerationStatus.next

      dao.upsert(userId, status).futureValue

      dao.get(userId).futureValue shouldBe Some(status)
    }

    "update existing record" in {
      val userId = ModelGenerators.userId.next
      val status = ModelGenerators.userModerationStatus.next

      dao.upsert(userId, status).futureValue

      val newStatus = ModelGenerators.userModerationStatus.next
      dao.upsert(userId, newStatus).futureValue

      dao.get(userId).futureValue shouldBe Some(newStatus)
    }
  }

  "UserModerationStatusDao.delete" should {
    "delete record" in {
      val userId = ModelGenerators.userId.next
      val status = ModelGenerators.userModerationStatus.next

      dao.upsert(userId, status).futureValue
      dao.delete(userId).futureValue

      dao.get(userId).futureValue shouldBe None
    }
  }

  "UserModerationStatusDao" should {
    "return None if record doesn't exist" in {
      val userId = ModelGenerators.userId.next
      dao.get(userId).futureValue shouldBe None
    }
  }

}
