package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.AutoruSaleStatus
import ru.yandex.vos2.model.ModelUtils.{RichOffer, RichOfferBuilder}
import ru.yandex.vos2.{getNow, OfferModel}

import scala.jdk.CollectionConverters._

class ExpirationYdbWorkerTest
  extends AnyWordSpec
  with MockitoSupport
  with Matchers
  with OptionValues
  with InitTestDbs
  with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  initOldSalesDbs()

  val saleId1 = 1043270830 // объявление от частника с активными услугами

  val saleId2 = 1044159039 // объявление клиента с активной услугой

  val truckId = 6229746 // объявление частника в траксах с активными услугами

  val offer1 = getOfferById(saleId1) // expire_date = Thu Oct 20 14:37:02 MSK 2016
  .toBuilder.clearFlag(OfferFlag.OF_INACTIVE).build

  val offer2 = getOfferById(saleId2) // expire_date = Wed Sep 21 22:42:17 MSK 2016

  val offer3 = truck2proto(truckId).toBuilder.setTimestampWillExpire(new DateTime().minusDays(1).getMillis).build()

  private def truck2proto(truckId: Long): Offer = {
    components.truckOfferConverter
      .convertStrict(components.autoruTrucksDao.getOffer(truckId).value, None)
      .converted
      .value
  }

  abstract private class Fixture {

    val worker = new ExpirationYdbWorker(
      components.oldDbWriter
    ) with YdbWorkerTestImpl {
      override def features = components.featuresManager
    }
  }

  ("process") in new Fixture {
    // 1. если стоит флаг OF_EXPIRED - ничего не делаем
    checkNotProcessed(offer1.toBuilder.putFlag(OfferFlag.OF_EXPIRED).build(), worker)

    // 2. если стоит флаг OF_DRAFT - тоже ничего не делаем
    checkNotProcessed(offer1.toBuilder.putFlag(OfferFlag.OF_DRAFT).build(), worker)

    // 3. если объявление от салона - ничего не делаем
    checkNotProcessed(offer2, worker)

    // 4. если expire_date не наступил - объявление не меняем, время следующего посещения меняем
    // ставим expire_date через час от текущего момента
    checkDelayed(offer1.toBuilder.setTimestampWillExpire(getNow + 3600000L).build(), 3600, worker)

    // 5. если все звезды сошлись - ставим флаг expired, ставим статус expired в старой базе, деактивируем сервисы
    checkChanged(offer1, worker)

    // 6. все звезды сходятся еще разок и для старой базы
    // сначала готовим объявление в старой базе - делаем активным и проверяем, что есть активные услуги
    components.autoruSalesDao.setStatus(saleId1, Seq.empty, AutoruSaleStatus.STATUS_SHOW_FOR_USER)
    val sale1 = components.autoruSalesDao.getOffer(saleId1).value
    assert(sale1.status == AutoruSaleStatus.STATUS_SHOW_FOR_USER)
    assert(sale1.services.value.length == 5)
    assert(sale1.services.value.exists(_.isActivated))
    checkChanged(offer1.toBuilder.clearFlag(OfferFlag.OF_EXPIRED).build(), worker)
    val sale1_2 = components.autoruSalesDao.getOffer(saleId1).value
    // статус в старой базе должен стать expired и все услуги должны стать неактивными
    assert(sale1_2.status == AutoruSaleStatus.STATUS_EXPIRED)
    assert(sale1_2.services.value.length == 5)

    // проверим для траксов:
    val truck = components.autoruTrucksDao.getOffer(truckId).value
    assert(truck.status == AutoruSaleStatus.STATUS_SHOW)
    assert(truck.services.value.length == 3)
    assert(truck.services.value.exists(_.isActivated))
    checkChanged(offer3, worker)
    val truck2 = components.autoruTrucksDao.getOffer(truckId).value
    // статус в старой базе должен стать expired и все услуги должны стать неактивными
    assert(truck2.status == AutoruSaleStatus.STATUS_EXPIRED)
    assert(truck2.services.value.length == 3)

    // TODO тесты для moto
  }

  def checkNotProcessed(offer: OfferModel.Offer, worker: ExpirationYdbWorker): Unit = {
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  def checkDelayed(offer: OfferModel.Offer, delaySec: Long, worker: ExpirationYdbWorker): Unit = {
    assert(worker.shouldProcess(offer, None).shouldProcess)
    val result = worker.process(offer, None)
    assert(result.updateOfferFunc.isEmpty)
    assert(result.nextCheck.nonEmpty)
  }

  def checkChanged(offer: OfferModel.Offer, worker: ExpirationYdbWorker): Unit = {
    assert(worker.shouldProcess(offer, None).shouldProcess)
    assert(offer.getOfferAutoru.getServicesList.asScala.exists(_.getIsActive))
    val result = worker.process(offer, None)
    assert(result.updateOfferFunc.get(offer) != offer)
    assert(result.nextCheck.isEmpty)
    assert(result.updateOfferFunc.get(offer).hasFlag(OfferFlag.OF_EXPIRED))
    assert(offer.getOfferAutoru.getServicesCount == result.updateOfferFunc.get(offer).getOfferAutoru.getServicesCount)
    result.updateOfferFunc
      .get(offer)
      .getOfferAutoru
      .getServicesList
      .asScala
      .zip(offer.getOfferAutoru.getServicesList.asScala)
      .foreach {
        case (s1, s2) =>
          assert(s1.getServiceType == s2.getServiceType)
      }
  }
}
