package ru.yandex.vertis.passport.dao.impl.mysql

import org.joda.time.DateTime
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, MySqlSupport, SpecBase}
import ru.yandex.vertis.passport.util.mysql.DualDatabase

/**
  *
  * @author zvez
  */
class MysqlUserPreviousPasswordsDaoSpec extends WordSpec with SpecBase with MySqlSupport {

  val dao = new MysqlUserPreviousPasswordsDao(DualDatabase(dbs.passport))

  "UserPreviousPasswordsDao" should {
    "return empty list for unknown user" in {
      val userId = ModelGenerators.userId.next
      dao.get(userId, 10).futureValue shouldBe Nil
    }

    "store and return last password" in {
      val userId = ModelGenerators.userId.next
      val hash = ModelGenerators.readableString.next
      val pwdHash = ModelGenerators.readableString.next
      dao.store(userId, pwdHash, DateTime.now()).futureValue
      dao.get(userId, 1).futureValue should contain theSameElementsAs Seq(pwdHash)
    }

    "sotre and return last N passwords" in {
      val n = 10
      val m = 10
      val userId = ModelGenerators.userId.next

      val pwdHashes = ModelGenerators.readableString.next(n * m)

      pwdHashes.foreach(pwd => dao.store(userId, pwd, DateTime.now()).futureValue)
      val expected = pwdHashes.takeRight(n).toSeq.reverse
      dao.get(userId, n).futureValue should contain theSameElementsAs expected
    }
  }

}
