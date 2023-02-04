package ru.yandex.vertis.passport.dao.impl.mysql

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.dao.impl.mysql.AutoruBlacklistLogDao.BlacklistLogRecord
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, MySqlSupport, SpecBase}
import ru.yandex.vertis.passport.util.mysql.DualDatabase

/**
  *
  * @author zvez
  */
class AutoruBlacklistLogDaoSpec extends WordSpec with SpecBase with MySqlSupport {

  val dao = new AutoruBlacklistLogDao(DualDatabase(dbs.legacyLogs))

  val recordGen = {
    import ModelGenerators._
    for {
      userId <- userId
      adminId <- ModelGenerators.userId
      action <- Gen.oneOf(AutoruBlacklistLogDao.BlockValue, AutoruBlacklistLogDao.UnblockValue)
    } yield BlacklistLogRecord(
      userId = userId,
      adminId = adminId,
      action = action,
      setDate = DateTime.now().withMillisOfSecond(0)
    )
  }

  "AutoruBlacklistLogDao" should {
    "insert record" in {
      val record = recordGen.next
      dao.insert(record).futureValue
    }
    "get last records" in {
      val records = recordGen.next(10)
      records.foreach(r => dao.insert(r).futureValue)
      val loaded = dao.getNewRecordsSince(0L, 100).futureValue.map(_.copy(id = None))
      loaded should contain allElementsOf records
    }
  }

}
