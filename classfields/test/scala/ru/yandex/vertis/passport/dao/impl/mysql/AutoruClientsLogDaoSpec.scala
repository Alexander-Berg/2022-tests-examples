package ru.yandex.vertis.passport.dao.impl.mysql

import org.scalatest.WordSpec
import ru.yandex.vertis.passport.test.{ModelGenerators, MySqlSupport, SpecBase}
import ru.yandex.vertis.passport.test.Producer._
import ru.yandex.vertis.passport.util
import ru.yandex.vertis.passport.util.mysql.DualDatabase

class AutoruClientsLogDaoSpec extends WordSpec with SpecBase with MySqlSupport {

  val dao = new AutoruClientLogDao(DualDatabase(dbs.legacyOfficeLogs))

  "AutoruClientsLogDao" should {
    "work" in {
      val userEmail = ModelGenerators.emailAddress.next
      val byUserId = ModelGenerators.userId.next
      val clientId = ModelGenerators.clientId.next
      val record =
        AutoruClientLogDao.ClientLogRecord(userEmail, clientId, AutoruClientLogDao.Action.Add, Some(byUserId), None)
      dao.insert(record).futureValue
      dao.list(util.Range.Full).futureValue should contain(record)
    }
  }
}
