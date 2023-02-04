package ru.yandex.vertis.passport.dao

import org.joda.time.DateTime
import org.scalatest.FreeSpec
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.test.Producer._

trait UserAuthTokenDaoSpec extends FreeSpec with SpecBase {
  def tokenDao: UserAuthTokenDao

  "UserAuthTokenDao" - {
    "return None if token not found" in {
      tokenDao.find("some_test_id").futureValue shouldBe None
    }

    "insert and find token" in {
      val token = ModelGenerators.userAuthToken.next
      tokenDao.insert(token).futureValue
      val res = tokenDao.find(token.id).futureValue

      res.get shouldEqual token
    }

    "use token" in {
      val token = ModelGenerators.userAuthToken.next
      tokenDao.insert(token).futureValue
      val now = DateTime.now().withMillisOfSecond(0)
      tokenDao.setUsed(token.id, now).futureValue
      val res = tokenDao.find(token.id).futureValue

      res.get.used.get shouldEqual now
    }
  }

}
