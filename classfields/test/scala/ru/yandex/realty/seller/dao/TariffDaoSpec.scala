package ru.yandex.realty.seller.dao

import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.seller.model.tariff.JuridicalTariff
import ru.yandex.realty.tracing.Traced

/**
  * @author azakharov
  */
trait TariffDaoSpec extends AsyncSpecBase {

  def tariffDao: TariffDao

  implicit val trace: Traced = Traced.empty

  "TariffDao" should {
    "create and get tariff for user" in {
      val uid = "1324567890"
      val clientId = None
      tariffDao.create(uid, JuridicalTariff.CallsMaximum, clientId).futureValue
      tariffDao.get(uid, clientId).futureValue shouldEqual (Some(JuridicalTariff.CallsMaximum))
    }

    "create update tariff for user" in {
      val uid = "1324567890"
      val clientId = None
      tariffDao.create(uid, JuridicalTariff.CallsMinimum, clientId).futureValue
      tariffDao.update(uid, JuridicalTariff.CallsExtended, clientId).futureValue
      tariffDao.get(uid, clientId).futureValue shouldEqual (Some(JuridicalTariff.CallsExtended))
    }
  }
}
