package ru.yandex.vertis.passport.dao.impl.mysql

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.dao.impl.mysql.AutoruAuthLogDao.AuthLogRecord
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, MySqlSupport, SpecBase}
import ru.yandex.vertis.passport.util.mysql.DualDatabase

/**
  *
  * @author zvez
  */
class AutoruAuthLogDaoSpec extends WordSpec with SpecBase with MySqlSupport {

  val dao = new AutoruAuthLogDao(DualDatabase(dbs.legacyLogs))

  val logGen = {
    import ModelGenerators._
    for {
      userId <- userId
      ip <- Gen.oneOf(0, 100000000)
      ipProxy <- Gen.option(readableString)
      authAlias <- readableString
      login <- Gen.option(readableString)
      transformFromUserId <- Gen.option(userId)
    } yield AuthLogRecord(
      None,
      userId,
      DateTime.now.withMillisOfSecond(0),
      ip,
      ipProxy,
      authAlias,
      login,
      transformFromUserId
    )
  }

  "AutoruAuthLogDao" should {
    "insert record" in {
      val record = logGen.next
      dao.insert(record).futureValue
      dao.getLast(record.userId, 1).futureValue.map(_.withoutId) shouldBe Seq(record)
    }

    "insert multiple records" in {
      val userId = ModelGenerators.userId.next
      val inserted = (1 to 10).map { _ =>
        val record = logGen.next.copy(userId = userId)
        dao.insert(record).futureValue
        record
      }

      dao.getLast(userId, 10).futureValue.map(_.withoutId) should contain theSameElementsAs inserted
    }
  }

}
