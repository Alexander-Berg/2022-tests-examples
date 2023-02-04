package ru.yandex.vos2.autoru.dao.offers.diff

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import ru.auto.api.DiffLogModel.OfferChangeEvent
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Phone
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferDaoImpl
import ru.yandex.vos2.autoru.dao.offers.diff.sender.DiffSender
import ru.yandex.vos2.autoru.model.AutoruOfferID
import ru.yandex.vos2.autoru.utils.converters.offerform.OfferFormConverter
import ru.yandex.vos2.dao.offers.OfferUpdate
import ru.yandex.vos2.getNow
import ru.yandex.vos2.util.ExternalAutoruUserRef

import scala.util.Try

/**
  * Created by sievmi on 15.09.17.
  */

@RunWith(classOf[JUnitRunner])
class DiffSendAutoruOfferDaoTest
  extends AnyFunSuite
  with InitTestDbs
  with MockitoSupport
  with BeforeAndAfter
  with BeforeAndAfterAll {

  initDbs()

  val mockDiffSender: DiffSender = mock[DiffSender]

  val offers: List[Offer] = getOffersFromJson

  val offerDao = new AutoruOfferDaoImpl(
    components.mySql,
    components.carsCatalog,
    components.trucksCatalog,
    components.motoCatalog,
    components.regionTree,
    components.stringDeduplication,
    components.featuresManager,
    components.env.props
  ) with DiffSendAutoruOfferDao {

    override val diffSender: DiffSender = mockDiffSender

    override val offerFormConverter: OfferFormConverter = new OfferFormConverter(
      components.mdsPhotoUtils,
      components.regionTree,
      components.mdsPanoramasUtils,
      components.offerValidator,
      components.salonConverter,
      components.currencyRates,
      components.featuresManager,
      components.banReasons,
      components.carsCatalog,
      components.trucksCatalog,
      components.motoCatalog
    )
    override val feature: Feature[Boolean] = components.featuresManager.SendDiffLog
    override val host: String = "test"
    override val diffGenerator: DiffGenerator = components.diffGenerator
  }

  val feature: Feature[Boolean] = components.featuresManager.SendDiffLog
  components.featureRegistry.updateFeature(feature.name, true)

  before {
    reset(mockDiffSender)
    when(mockDiffSender.send(any[Iterable[OfferChangeEvent]]())).thenReturn(Try())
    when(mockDiffSender.send(any[OfferChangeEvent]())).thenReturn(Try())
  }

  test("migrateAnsSave") {
    offerDao.migrateAndSave(getSalesFromDb)((sale, optOffer) => {
      components.carOfferConverter.convertStrict(sale, optOffer).converted
    })(Traced.empty)

    verify(mockDiffSender, atLeastOnce()).send(any[Iterable[OfferChangeEvent]]())
  }

  test("saveMigratedWithoutLock") {
    offerDao.saveMigratedWithoutLock(offers)(Traced.empty)

    verify(mockDiffSender, atLeastOnce()).send(any[Iterable[OfferChangeEvent]]())
  }

  test("useOfferId") {
    offerDao.useOfferID(AutoruOfferID.parse("1043270830-6b56a"))(offer => {
      val builder = offer.toBuilder
      builder.getOfferAutoruBuilder.getDocumentsBuilder.setVin("123")
      OfferUpdate.visitNow(builder.build())
    })(Traced.empty)

    verify(mockDiffSender).send(any[OfferChangeEvent]())
  }

  test("userOfferIdExt") {
    offerDao.useOfferIDExt(AutoruOfferID.parse("1043270830-6b56a"))(offer => {
      val builder = offer.toBuilder
      builder.getOfferAutoruBuilder.getDocumentsBuilder.setVin("1234")
      (OfferUpdate.visitNow(builder.build()), 123)
    })(Traced.empty)

    verify(mockDiffSender).send(any[OfferChangeEvent]())
  }

  test("useOnShard") {
    val offer = offers.head
    val shard = offerDao.getShardForUserMapping(offer.getOfferID)
    val userRef = ExternalAutoruUserRef.fromExt(offer.getUserRef)
    offerDao.useOnShard(shard, Seq(offer.getOfferID), includeRemoved = false, userRef, reportMissed = false)(offer =>
      offer.map(OfferUpdate.visitNow)
    )(Traced.empty)

    verify(mockDiffSender, atLeastOnce()).send(any[Iterable[OfferChangeEvent]]())
  }

  test("useOnShardExt") {
    val offer = offers.head
    val shard = offerDao.getShardForUserMapping(offer.getOfferID)
    val userRef = ExternalAutoruUserRef.fromExt(offer.getUserRef)
    offerDao.useOnShardExt(shard, Seq(offer.getOfferID), includeRemoved = false, userRef, reportMissed = false)(offer =>
      offer.map(x => (OfferUpdate.visitNow(x), 123))
    )(Traced.empty)

    verify(mockDiffSender, atLeastOnce()).send(any[Iterable[OfferChangeEvent]]())
  }

  test("activate") {
    val offer = offers.head
    val offerId = AutoruOfferID.parse(offer.getOfferID)
    offerDao.useOfferID(offerId)(offer => {
      val builder = offer.toBuilder
      builder.getOfferAutoruBuilder.getDocumentsBuilder.setVin("12345")
      builder.getOfferAutoruBuilder.getRecallInfoBuilder.setRecallTimestamp(getNow)
      builder.getOfferAutoruBuilder.getSellerBuilder.addPhone(Phone.newBuilder().setNumber("+79168888888"))
      OfferUpdate.visitNow(builder.build())
    })(Traced.empty)

    val userRef = ExternalAutoruUserRef.fromExt(offer.getUserRef)

    val res = offerDao.activate(
      AutoruOfferID.parse(offer.getOfferID),
      userRef,
      Some(offer.getOfferAutoru.getCategory),
      needActivation = false,
      ""
    )(Traced.empty)

    withClue(res)(assert(res.isSuccess))

    verify(mockDiffSender, times(2)).send(any[OfferChangeEvent]())
  }
}
