package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.BeforeAndAfterAll
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.cert.CertModel.BrandCertStatus
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.BrandCertificationUpdateWorkerYdb.{FavoritWarranty, RolfUsedWarranty}
import ru.yandex.vos2.autoru.catalog.cars.CarsCatalog
import ru.yandex.vos2.autoru.catalog.cars.model.Mark
import ru.yandex.vos2.autoru.dao.old.AutoruSalonsDao
import ru.yandex.vos2.autoru.model.{AutoruDealerMark, TestUtils}
import ru.yandex.vos2.autoru.services.vindecoder.VinDecoderClient
import ru.yandex.vos2.autoru.services.vindecoder.VinDecoderClient.BrandCertificationResponse
import ru.yandex.vos2.model.{UserRef, UserRefAutoruClient}
import ru.yandex.vos2.util.RandomUtil

import scala.util.Success

class BrandCertificationUpdateWorkerYdbTest
  extends AnyWordSpec
  with MockitoSupport
  with Matchers
  with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val mockedClient = mock[VinDecoderClient]
    val mockedCatalog = mock[CarsCatalog]
    val mockedDao = mock[AutoruSalonsDao]
    val worker = new BrandCertificationUpdateWorkerYdb(mockedClient, mockedCatalog, mockedDao) with YdbWorkerTestImpl
  }

  "shouldProcess = false, not dealer" in new Fixture {
    val offer1 = TestUtils.createOffer(dealer = false)
    assert(!worker.shouldProcess(offer1.build(), None).shouldProcess)
  }

  "shouldProcess = false, not car" in new Fixture {
    val offer1 = TestUtils.createOffer(dealer = true, category = Category.MOTO)
    assert(!worker.shouldProcess(offer1.build(), None).shouldProcess)
  }

  "shouldProcess = false, no vin" in new Fixture {
    val offer1 = TestUtils.createOffer(dealer = true, category = Category.MOTO)
    offer1.getOfferAutoruBuilder.getDocumentsBuilder.clearVin()
    assert(!worker.shouldProcess(offer1.build(), None).shouldProcess)
  }

  "process with some cert: not official dealer" in new Fixture {

    val resp = BrandCertificationResponse(
      "someCert",
      DateTime.now().minusDays(1).getMillis,
      DateTime.now().getMillis,
      None,
      None,
      None
    )

    when(mockedClient.lastBrandCertification(?)(?)).thenReturn(Success(Some(resp)))
    when(mockedCatalog.getMarkByCode(?)).thenReturn(None)
    val offer = TestUtils.createOffer(dealer = true)

    val updatedOffer = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())

    assert(updatedOffer.getOfferAutoru.getBrandCertInfo.getCertStatus == BrandCertStatus.BRAND_CERT_INACTIVE)
  }

  "process with some cert: trusted program" in new Fixture {

    val resp = BrandCertificationResponse(
      "DasWeltAuto",
      DateTime.now().minusDays(1).getMillis,
      DateTime.now().getMillis,
      None,
      None,
      None
    )

    when(mockedClient.lastBrandCertification(?)(?)).thenReturn(Success(Some(resp)))
    when(mockedCatalog.getMarkByCode(?)).thenReturn(None)
    val offer = TestUtils.createOffer(dealer = true)

    val updatedOffer = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())

    assert(updatedOffer.getOfferAutoru.getBrandCertInfo.getCertStatus == BrandCertStatus.BRAND_CERT_ACTIVE)
  }

  "process with some cert: official dealer" in new Fixture {

    val resp = BrandCertificationResponse(
      "someCert",
      DateTime.now().minusDays(1).getMillis,
      DateTime.now().getMillis,
      None,
      None,
      None
    )

    val offer = TestUtils.createOffer(dealer = true)
    val clientId = UserRef.from(offer.getUserRef).asInstanceOf[UserRefAutoruClient].clientId

    when(mockedClient.lastBrandCertification(?)(?)).thenReturn(Success(Some(resp)))
    private val markId = 15L
    when(mockedCatalog.getMarkByCode(?)).thenReturn(Some(Mark("FORD", markId, "Форд", "Ford", 213)))
    private val moment: DateTime = DateTime.now()
    when(mockedDao.getDealerMarksByClientIds(?)(?))
      .thenReturn(Map(clientId -> List(AutoruDealerMark(1L, clientId, markId, moment, moment, moment))))

    val updatedOffer = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())

    assert(updatedOffer.getOfferAutoru.getBrandCertInfo.getCertStatus == BrandCertStatus.BRAND_CERT_ACTIVE)
  }

  "not process rolf because of wrong dealer" in new Fixture {

    val resp = BrandCertificationResponse(
      RolfUsedWarranty,
      DateTime.now().minusDays(1).getMillis,
      DateTime.now().getMillis,
      None,
      None,
      None
    )

    when(mockedClient.lastBrandCertification(?)(?)).thenReturn(Success(Some(resp)))
    val offer = TestUtils.createOffer(dealer = true)

    val updatedOffer = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())

    assert(updatedOffer.getOfferAutoru.getBrandCertInfo.getCertStatus == BrandCertStatus.BRAND_CERT_INACTIVE)
  }

  "process rolf if dealer is correct" in new Fixture {

    val resp = BrandCertificationResponse(
      RolfUsedWarranty,
      DateTime.now().minusDays(1).getMillis,
      DateTime.now().getMillis,
      None,
      None,
      None
    )

    when(mockedClient.lastBrandCertification(?)(?)).thenReturn(Success(Some(resp)))
    private val dealerId: Long = RandomUtil.choose(BrandCertificationUpdateWorkerYdb.RolfUsedWarrantyDealers.keys.toSeq)
    val offer = TestUtils.createOffer(dealer = true).setUserRef("ac_" + dealerId)

    val updatedOffer = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())

    assert(updatedOffer.getOfferAutoru.getBrandCertInfo.getCertStatus == BrandCertStatus.BRAND_CERT_ACTIVE)
  }

  "not process favorit because of wrong dealer" in new Fixture {

    val resp = BrandCertificationResponse(
      FavoritWarranty,
      DateTime.now().minusDays(1).getMillis,
      DateTime.now().getMillis,
      None,
      None,
      None
    )

    when(mockedClient.lastBrandCertification(?)(?)).thenReturn(Success(Some(resp)))
    val offer = TestUtils.createOffer(dealer = true)

    val updatedOffer = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())

    assert(updatedOffer.getOfferAutoru.getBrandCertInfo.getCertStatus == BrandCertStatus.BRAND_CERT_INACTIVE)
  }

  "process favorit if dealer is correct" in new Fixture {

    val resp = BrandCertificationResponse(
      FavoritWarranty,
      DateTime.now().minusDays(1).getMillis,
      DateTime.now().getMillis,
      None,
      None,
      None
    )

    when(mockedClient.lastBrandCertification(?)(?)).thenReturn(Success(Some(resp)))
    private val dealerId: Long = RandomUtil.choose(BrandCertificationUpdateWorkerYdb.FavoritWarrantyDealers.toSeq)
    val offer = TestUtils.createOffer(dealer = true).setUserRef("ac_" + dealerId)

    val updatedOffer = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())

    assert(updatedOffer.getOfferAutoru.getBrandCertInfo.getCertStatus == BrandCertStatus.BRAND_CERT_ACTIVE)
  }
}
