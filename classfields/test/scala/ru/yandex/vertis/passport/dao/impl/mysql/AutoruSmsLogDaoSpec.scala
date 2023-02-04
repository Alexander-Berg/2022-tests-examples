package ru.yandex.vertis.passport.dao.impl.mysql

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, MySqlSupport, SpecBase}
import ru.yandex.vertis.passport.util.mysql.DualDatabase

/**
  *
  * @author zvez
  */
class AutoruSmsLogDaoSpec extends WordSpec with SpecBase with MySqlSupport {

  val dao = new AutoruSmsLogDao(DualDatabase(dbs.legacyUsers))

  val recordGen = {
    import ru.yandex.vertis.passport.test.ModelGenerators._
    for {
      phone <- phoneNumber
      incoming <- readableString
      outgoing <- readableString
      userId <- Gen.option(userId)
    } yield AutoruSmsLogDao.SmsLogRecord(phone, incoming, outgoing, userId, DateTime.now().withMillisOfSecond(0))
  }

  "AutoruSmsLogDao" should {
    "insert records" in {
      val phone = ModelGenerators.phoneNumber.next
      val records = recordGen.next(10).map(_.copy(toPhone = phone))
      records.foreach(r => dao.insert(r).futureValue)

      val saved = dao.list(AutoruSmsLogDao.Filter.ByPhone(phone), 100).futureValue
      saved should contain theSameElementsAs records
    }
  }

}
