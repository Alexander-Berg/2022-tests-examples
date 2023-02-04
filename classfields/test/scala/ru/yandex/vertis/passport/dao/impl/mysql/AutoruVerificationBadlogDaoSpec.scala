package ru.yandex.vertis.passport.dao.impl.mysql

import org.joda.time.DateTime
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.dao.impl.mysql.AutoruVerificationBadlogDao.VerificationBadlogRecord
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, MySqlSupport, SpecBase}
import ru.yandex.vertis.passport.util.mysql.DualDatabase

/**
  *
  * @author zvez
  */
class AutoruVerificationBadlogDaoSpec extends WordSpec with SpecBase with MySqlSupport {

  val dao = new AutoruVerificationBadlogDao(DualDatabase(dbs.legacyUsers))

  val logGen = {
    import ModelGenerators._
    for {
      userId <- userId
      ip <- readableString
      userValue <- readableString
      userAgent <- readableString
      userPass <- readableString
    } yield VerificationBadlogRecord(
      ip,
      userId,
      userValue,
      "passport",
      DateTime.now.withMillisOfSecond(0),
      userAgent,
      userPass
    )
  }

  "AutoruVerificationBadlogDao" should {
    "insert log records" in {
      val record = logGen.next
      dao.insert(record).futureValue
      dao.getLast(1).futureValue shouldBe Seq(record)
    }

    "insert multiple records" in {
      val records = logGen.next(10)
      records.foreach(r => dao.insert(r).futureValue)

      dao.getLast(10).futureValue should contain theSameElementsAs records
    }
  }

}
