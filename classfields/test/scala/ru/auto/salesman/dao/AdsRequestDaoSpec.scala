package ru.auto.salesman.dao

import ru.auto.salesman.model.AdsRequestTypes._
import ru.auto.salesman.model.ClientId
import ru.auto.salesman.test.BaseSpec
import zio.ZIO
import ru.auto.salesman.dao.impl.jdbc.JdbcAdsRequestDao.table
import ru.auto.salesman.dao.impl.jdbc.JdbcClientsChangedBufferDao.commonFilter

trait AdsRequestDaoSpec extends BaseSpec {

  private val clientId: ClientId = 1L

  protected def adsRequestDao: AdsRequestDao
  protected def clientChangedBufferDao: ClientsChangedBufferDao

  "AdsRequestDao" should {

    "delete all records for client" in {

      adsRequestDao.insert(clientId, Commercial)
      adsRequestDao.get(clientId, Commercial).success.value.value

      adsRequestDao.insert(clientId, CarsUsed)
      adsRequestDao.get(clientId, Commercial).success.value.value

      adsRequestDao.deleteAllForClient(clientId)

      adsRequestDao.get(clientId, Commercial).success.value shouldBe None

      adsRequestDao.get(clientId, CarsUsed).success.value shouldBe None

    }

    "write to buffer table on insert" in {
      val (before, after) = (for {
        b <- clientChangedBufferDao.get(commonFilter)
        _ <- ZIO.fromTry(adsRequestDao.insert(clientId, Commercial))
        a <- clientChangedBufferDao.get(commonFilter)
      } yield (b, a)).success.value

      assert(before.length < after.length)
      assert(after.exists(r => r.clientId == clientId && r.dataSource == table))
    }
  }
}
