package ru.auto.salesman.service.quota_offers

import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatcher}
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfter, Suite}
import ru.auto.salesman.dao.ClientDao.ForIdWithPoi7
import ru.auto.salesman.dao.GoodsDao.Filter.ForOfferCategory
import ru.auto.salesman.dao.OfferDao.Condition.OfferIdCategory
import ru.auto.salesman.dao.OfferDao.ForClientPaymentGroupStatus
import ru.auto.salesman.dao.{ClientDao, GoodsDao, OfferDao}
import ru.auto.salesman.environment.startOfToday
import ru.auto.salesman.model._
import ru.auto.salesman.test.service.payment_model.TestPaymentModelFactory
import ru.auto.salesman.service.quota_offers.QuotaOffersServiceImplSpec._
import ru.auto.salesman.service.quota_offers.QuotaOffersTestData._
import ru.auto.salesman.service.QuotaService
import ru.auto.salesman.service.client.ClientService
import ru.auto.salesman.service.client.ClientService.ClientServiceError
import ru.auto.salesman.test.dao.gens.GoodRecordGen
import ru.auto.salesman.test.{DeprecatedMockitoBaseSpec, TestException}
import ru.auto.salesman.util.AutomatedContext
import ru.yandex.vertis.generators.ProducerProvider.asProducer
import ru.yandex.vertis.mockito.util.RichTryOngoingStub
import ru.yandex.vertis.util.time.DateTimeUtil
import zio.ZIO

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class QuotaOffersServiceImplSpec extends DeprecatedMockitoBaseSpec with Mocking {

  implicit val oc = AutomatedContext("quota-offers-service")

  "QuotaOffersService" should {

    "empty offers for quota" in {
      val offers = List.empty[Offer]
      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(Success(testClient))
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(offers))
      whenQuotaService(Success(Iterable(testQuota)))
      quotaOffersService
        .actualize(testQuota.quotaType, testQuota.clientId)
        .success
        .value shouldBe (())
      verifyZeroInteractions(goodsDao)
      verify(offerDao, times(0)).update(?, ?)
    }

    "activate expired when poi7 setup found for client" in {
      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(Success(testClient))
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(Nil))
      whenQuotaService(Success(Iterable(testQuota)))

      quotaOffersService
        .actualize(testQuota.quotaType, testQuota.clientId)
        .success
        .value shouldBe (())

      verify(offerDao).get {
        argThat {
          new ArgumentMatcher[ForClientPaymentGroupStatus] {
            def matches(filter: ForClientPaymentGroupStatus): Boolean =
              filter.clientId == testClientId &&
              filter.paymentGroup == testPaymentGroup &&
              filter.offerStatus.contains(OfferStatuses.Expired)
          }
        }
      }
      verifyZeroInteractions(goodsDao)
      verifyNoMoreInteractions(offerDao)
    }

    "not activate expired when no poi7 setup found for client" in {
      whenClientDaoGet(testClientId, None)
      whenClientSource(Success(testClient))
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(Nil))
      whenQuotaService(Success(Iterable(testQuota)))

      quotaOffersService
        .actualize(testQuota.quotaType, testQuota.clientId)
        .success
        .value shouldBe (())

      verify(offerDao).get {
        argThat {
          new ArgumentMatcher[ForClientPaymentGroupStatus] {
            def matches(filter: ForClientPaymentGroupStatus): Boolean =
              filter.clientId == testClientId &&
              filter.paymentGroup == testPaymentGroup &&
              !filter.offerStatus.contains(OfferStatuses.Expired)
          }
        }
      }
      verifyZeroInteractions(goodsDao)
      verifyNoMoreInteractions(offerDao)
    }

    "activate all offers for quota" in new Captors {
      val offers = genOffers(testQuotaSize.toInt)
      val forUpdateOffers = offers
      val expectedPatch =
        OfferDao.OfferPatch(Some(testQuota.to), Some(OfferStatuses.Show), None)
      val expectedPatches = List.fill(forUpdateOffers.size)(expectedPatch)

      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(Success(testClient))
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(offers))
      whenQuotaService(Success(Iterable(testQuota)))

      quotaOffersService
        .actualize(testQuota.quotaType, testQuota.clientId)
        .success
        .value shouldBe (())

      verifyZeroInteractions(goodsDao)
      verify(offerDao, times(forUpdateOffers.size))
        .update(offerConditionCaptor.capture(), offerPatchCaptor.capture())

      val offerPatches =
        offerPatchCaptor.getAllValues.asScala.map(_.copy(setDate = None))
      val offerIds = offerConditionCaptor.getAllValues.asScala.map {
        case OfferIdCategory(offerId, _) => offerId
        case other => fail(s"expected OfferIdCategory; got $other")
      }

      offerIds should contain theSameElementsAs forUpdateOffers.map(_.id)
      offerPatches should contain theSameElementsAs expectedPatches
    }

    "fail on activation failure" in new Captors {
      val offers = genOffers(1)
      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(Success(testClient))
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(offers))
      whenQuotaService(Success(Iterable(testQuota)))
      when(offerDao.update(?, ?)).thenThrowT(new TestException)

      quotaOffersService
        .actualize(testQuota.quotaType, testQuota.clientId)
        .failure
        .exception shouldBe a[TestException]
    }

    "fail on deactivation failure" in new Captors {
      val offers = genOffers(1)
      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(Success(testClient))
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(offers))
      whenQuotaService(Success(Nil))
      when(offerDao.update(?, ?)).thenThrowT(new TestException)

      quotaOffersService
        .actualize(testQuota.quotaType, testQuota.clientId)
        .failure
        .exception shouldBe a[TestException]
    }

    "deactivate offers for inactive quota" in new Captors {
      val quota =
        testQuota.copy(from = startOfToday().minusDays(1), to = startOfToday())
      val offers = genOffers(testQuotaSize.toInt)

      val expectedPatch =
        OfferDao.OfferPatch(None, Some(OfferStatuses.Expired), None)
      val expectedPatches = List.fill(testQuotaSize.toInt)(expectedPatch)
      val goodsCountForOffer = 3
      val goods = offers
        .map(o => o.id -> genGoods(o, goodsCountForOffer))
        .toMap

      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(Success(testClient))
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(offers))
      whenGoodsDaoGet(testPaymentGroup)(goods)
      whenQuotaService(Success(Iterable.empty[StoredQuota]))

      quotaOffersService
        .actualize(quota.quotaType, quota.clientId)
        .success
        .value shouldBe (())
      verify(offerDao, times(testQuotaSize.toInt))
        .update(offerConditionCaptor.capture(), offerPatchCaptor.capture())

      val offerPatches = offerPatchCaptor.getAllValues.asScala
        .map(_.copy(setDate = None, expireDate = None))
      val offerIds = offerConditionCaptor.getAllValues.asScala.map {
        case OfferIdCategory(offerId, _) => offerId
        case other => fail(s"expected OfferIdCategory; got $other")
      }

      offerIds should contain theSameElementsAs offers.map(_.id)
      offerPatches should contain theSameElementsAs expectedPatches
    }

    "deactivate offers for quota size less than active offers" in new Captors {
      val offersCount = 9
      val goodsCountForOffer = 3
      val goodsCountForDeactivate =
        goodsCountForOffer * (offersCount - testQuotaSize.toInt)
      val offers =
        genOffers(offersCount).map(_.copy(status = OfferStatuses.Show))
      val goods = offers
        .map(o => o.id -> genGoods(o, goodsCountForOffer))
        .toMap

      val expectedPatch = OfferDao.OfferPatch(
        Some(testQuota.to),
        Some(OfferStatuses.Hidden),
        None
      )
      val expectedPatches =
        List.fill(offersCount - testQuotaSize.toInt)(expectedPatch)
      val expectedOffers = offers.toList
        .takeRight(offersCount - testQuotaSize.toInt)
      val expectedGoodsPatch = List.fill(goodsCountForDeactivate)(
        GoodsDao.Patch(status = Some(GoodStatuses.Inactive))
      )

      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(Success(testClient))
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(offers))
      whenGoodsDaoGet(testPaymentGroup)(goods)
      whenQuotaService(Success(Iterable(testQuota)))

      quotaOffersService
        .actualize(testQuota.quotaType, testQuota.clientId)
        .success
        .value shouldBe (())
      verify(offerDao, times(offersCount))
        .update(offerConditionCaptor.capture(), offerPatchCaptor.capture())
      verify(goodsDao, times(offersCount - testQuotaSize.toInt)).get(?)
      verify(goodsDao, times(goodsCountForDeactivate))
        .update(goodsConditionCaptor.capture(), goodsPatchCaptor.capture())

      val offerPatches =
        offerPatchCaptor.getAllValues.asScala.map(_.copy(setDate = None))
      val offerIds = offerConditionCaptor.getAllValues.asScala.map {
        case OfferIdCategory(offerId, _) => offerId
        case other => fail(s"expected OfferIdCategory; got $other")
      }

      offerIds.takeRight(
        offersCount - testQuotaSize.toInt
      ) should contain theSameElementsAs expectedOffers
        .map(_.id)
      offerPatches.filterNot(
        _.status.get == OfferStatuses.Show
      ) should contain theSameElementsAs expectedPatches

      val goodsPatches = goodsPatchCaptor.getAllValues.asScala
      val goodsConditionGoodsIds = goodsConditionCaptor.getAllValues.asScala
        .map(_.goodsId)

      val goodsIds = goods.values.flatMap(_.map(_.primaryKeyId))

      goodsPatches should contain theSameElementsAs expectedGoodsPatch
      goodsConditionGoodsIds.size should be(goodsCountForDeactivate)

      goodsConditionGoodsIds.foreach { g =>
        goodsIds should contain(g)
      }
    }

    "activate waiting and expired offers" in new Captors {
      val offersCount = 9
      val activeOffers =
        genOffers(3).map(_.copy(status = OfferStatuses.CreatedByClient))
      val waitingOffers =
        genOffers(3).map(_.copy(status = OfferStatuses.WaitingActivation))
      val expiredOffers =
        genOffers(3).map(_.copy(status = OfferStatuses.Expired))
      val offers = expiredOffers ++ waitingOffers ++ activeOffers
      val goodsCountForOffer = 3
      val goods = offers
        .map(o => o.id -> genGoods(o, goodsCountForOffer))
        .toMap

      val quota = testQuota.copy(size = 8)

      val expectedActivePatch =
        OfferDao.OfferPatch(Some(quota.to), Some(OfferStatuses.Show), None)
      val expectedActivePatches = List.fill(quota.size)(expectedActivePatch)
      val expectedInactivePatch =
        OfferDao.OfferPatch(Some(quota.to), Some(OfferStatuses.Hidden), None)

      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(Success(testClient))
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(offers))
      whenGoodsDaoGet(testPaymentGroup)(goods)
      whenQuotaService(Success(Iterable(quota)))
      quotaOffersService
        .actualize(quota.quotaType, quota.clientId)
        .success
        .value shouldBe (())
      verify(offerDao, times(offersCount))
        .update(offerConditionCaptor.capture(), offerPatchCaptor.capture())

      val offerPatches = offerPatchCaptor.getAllValues.asScala
        .map(_.copy(setDate = None))
        .groupBy(p => p.status.get)
      val offerIds = offerConditionCaptor.getAllValues.asScala.map {
        case OfferIdCategory(offerId, _) => offerId
        case other => fail(s"expected OfferIdCategory; got $other")
      }

      offerIds should contain theSameElementsAs offers.map(_.id)
      offerPatches
        .get(OfferStatuses.Show)
        .value should contain theSameElementsAs expectedActivePatches
      val patches = offerPatches.get(OfferStatuses.Hidden).value
      patches.size shouldBe 1
      patches.head shouldBe expectedInactivePatch
    }

    "activate only fresh expired offers" in new Captors {
      val waitingOffers = genOffers(3).map {
        _.copy(status = OfferStatuses.WaitingActivation)
      }

      val expiredOffers = genOffers(3).map {
        _.copy(status = OfferStatuses.Expired)
      }

      val oldExpiredOffers = genOffers(2).map {
        _.copy(
          status = OfferStatuses.Expired,
          expireDate = DateTimeUtil.now().minusDays(15)
        )
      }

      val offers = expiredOffers ++ waitingOffers ++ oldExpiredOffers

      val goodsCountForOffer = 3
      val goods = offers
        .map(o => o.id -> genGoods(o, goodsCountForOffer))
        .toMap

      val quota = testQuota.copy(size = 10)
      val expectedActiveOffersCount = 6

      val expectedActivePatch =
        OfferDao.OfferPatch(Some(quota.to), Some(OfferStatuses.Show), None)
      val expectedActivePatches =
        List.fill(expectedActiveOffersCount)(expectedActivePatch)

      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(Success(testClient))
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(offers))
      whenGoodsDaoGet(testPaymentGroup)(goods)
      whenQuotaService(Success(Iterable(quota)))
      quotaOffersService
        .actualize(quota.quotaType, quota.clientId)
        .success
        .value shouldBe (())

      verify(offerDao, times(expectedActiveOffersCount))
        .update(offerConditionCaptor.capture(), offerPatchCaptor.capture())

      val offerPatches = offerPatchCaptor.getAllValues.asScala
        .map(_.copy(setDate = None))
        .groupBy(p => p.status.get)

      val offerIds = offerConditionCaptor.getAllValues.asScala.map {
        case OfferIdCategory(offerId, _) => offerId
        case other => fail(s"expected OfferIdCategory; got $other")
      }

      offerIds should contain theSameElementsAs (expiredOffers ++ waitingOffers)
        .map(_.id)
      offerPatches
        .get(OfferStatuses.Show)
        .value should contain theSameElementsAs expectedActivePatches
    }

    "deactivate offers for inactive client" in new Captors {
      val client = testClient.copy(status = ClientStatuses.Inactive)
      val offersCount = 9
      val offers = genOffers(offersCount).map(
        _.copy(
          status = Gen
            .oneOf(List(OfferStatuses.Show, OfferStatuses.WaitingActivation))
            .next
        )
      )
      val goodsCountForOffer = 3
      val goods = offers
        .map(o => o.id -> genGoods(o, goodsCountForOffer))
        .toMap

      val expectedOffers = offers.toList
        .sortBy(_.status)
        .takeRight(offersCount - testQuota.size.toInt)
      val expectedPatch = OfferDao.OfferPatch(
        Some(testQuota.to),
        Some(OfferStatuses.Hidden),
        None
      )
      val expectedPatches =
        List.fill(offersCount - testQuotaSize.toInt)(expectedPatch)

      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(Success(client))
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(offers))
      whenGoodsDaoGet(testPaymentGroup)(goods)
      whenQuotaService(Success(Iterable(testQuota)))

      quotaOffersService
        .actualize(testQuota.quotaType, testQuota.clientId)
        .success
        .value shouldBe (())
      verify(offerDao, times(offersCount - testQuotaSize.toInt))
        .update(offerConditionCaptor.capture(), offerPatchCaptor.capture())

      val offerPatches =
        offerPatchCaptor.getAllValues.asScala.map(_.copy(setDate = None))
      val offerIds = offerConditionCaptor.getAllValues.asScala.map {
        case OfferIdCategory(offerId, _) => offerId
        case other => fail(s"expected OfferIdCategory; got $other")
      }

      offerIds should contain theSameElementsAs expectedOffers.map(_.id)
      offerPatches should contain theSameElementsAs expectedPatches
    }

    "activate all categorized offers for quota" in new Captors {
      val categorizedClientId = 11111L
      val client =
        testClient.copy(categorizedClientId = Some(categorizedClientId))
      val quota = testQuota.copy(quotaType = ProductId.QuotaPlacementMoto)
      val paymentGroup = PaymentGroup(ProductId.QuotaPlacementMoto)
      val offers =
        genOffers(testQuotaSize.toInt, categorizedClientId, paymentGroup)
      val forUpdateOffers = offers
      val expectedPatch =
        OfferDao.OfferPatch(Some(testQuota.to), Some(OfferStatuses.Show), None)
      val expectedPatches = List.fill(forUpdateOffers.size)(expectedPatch)

      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(Success(client))
      whenCategorizedOfferDaoGet(paymentGroup, categorizedClientId)(
        Success(offers)
      )
      whenQuotaService(Success(Iterable(quota)))

      quotaOffersService
        .actualize(quota.quotaType, quota.clientId)
        .success
        .value shouldBe (())

      verifyZeroInteractions(goodsDao)
      verify(offerDao, times(forUpdateOffers.size))
        .update(offerConditionCaptor.capture(), offerPatchCaptor.capture())

      val offerPatches =
        offerPatchCaptor.getAllValues.asScala.map(_.copy(setDate = None))
      val offerIds = offerConditionCaptor.getAllValues.asScala.map {
        case OfferIdCategory(offerId, _) => offerId
        case other => fail(s"expected OfferIdCategory; got $other")
      }

      offerIds should contain theSameElementsAs forUpdateOffers.map(_.id)
      offerPatches should contain theSameElementsAs expectedPatches
    }

    "failed to get client quotas" in {
      whenQuotaService(Failure(new TestException))
      quotaOffersService
        .actualize(testQuota.quotaType, testQuota.clientId)
        .failure
        .exception shouldBe an[TestException]
    }

    "failed for not resolved client" in {
      whenQuotaService(Success(Iterable(testQuota)))
      whenClientSource(Failure(new TestException))
      quotaOffersService
        .actualize(testQuota.quotaType, testQuota.clientId)
        .failure
        .exception
        .getCause shouldBe an[TestException]
    }

    "failed for client without categorized client id" in {
      val quota = testQuota.copy(quotaType = ProductId.QuotaPlacementMoto)

      whenQuotaService(Success(Iterable(quota)))
      whenClientSource(Success(testClient))
      quotaOffersService
        .actualize(quota.quotaType, quota.clientId)
        .failure
        .exception shouldBe an[IllegalArgumentException]
    }

    "failed for bad offers" in {
      whenQuotaService(Success(Iterable(testQuota)))
      whenClientSource(Success(testClient))
      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenOfferDaoGet(testPaymentGroup, testClientId)(
        Failure(new TestException)
      )
      quotaOffersService
        .actualize(testQuota.quotaType, testQuota.clientId)
        .failure
        .exception shouldBe an[TestException]
    }
  }
}

object QuotaOffersServiceImplSpec {

  def genGoods(offer: Offer, size: Int) =
    GoodRecordGen
      .next(size)
      .map(
        _.copy(
          offerId = offer.id,
          clientId = offer.clientId,
          category = offer.categoryId,
          section = offer.sectionId
        )
      )

  trait Captors {

    val offerPatchCaptor: ArgumentCaptor[OfferDao.OfferPatch] =
      ArgumentCaptor.forClass(classOf[OfferDao.OfferPatch])

    val offerConditionCaptor: ArgumentCaptor[OfferDao.Condition] =
      ArgumentCaptor.forClass(classOf[OfferDao.Condition])

    val goodsPatchCaptor: ArgumentCaptor[GoodsDao.Patch] =
      ArgumentCaptor.forClass(classOf[GoodsDao.Patch])

    val goodsConditionCaptor: ArgumentCaptor[GoodsDao.Condition.WithGoodsId] =
      ArgumentCaptor.forClass(classOf[GoodsDao.Condition.WithGoodsId])
  }

  trait Mocking extends DeprecatedMockitoBaseSpec with BeforeAndAfter {
    this: Suite =>

    val offerDao = mock[OfferDao]
    val categorizedOfferDao = mock[OfferDao]
    val goodsDao = mock[GoodsDao]
    val clientDao = mock[ClientDao]
    val clientService = mock[ClientService]
    val quotaService = mock[QuotaService]

    private val paymentModelFactory =
      TestPaymentModelFactory.withoutSingleWithCalls()

    val quotaOffersService =
      new QuotaOffersActualizerImpl(
        new QuotaOffersManager(
          offerDao,
          categorizedOfferDao,
          clientDao,
          clientService,
          quotaService,
          paymentModelFactory
        ),
        offerDao,
        goodsDao
      )

    before {
      reset(goodsDao, offerDao, clientDao, clientService, quotaService)
      when(goodsDao.update(?, ?))
        .thenReturn(Success(()))
      when(offerDao.update(?, ?)).thenReturn(Success(()))
    }

    protected def whenClientSource(response: Try[Client]): Unit =
      when(clientService.getByIdOrFail(testClientId, withDeleted = false))
        .thenReturn(ZIO.fromTry(response).mapError(ClientServiceError.TransportError))

    protected def whenClientDaoGet(id: ClientId, result: Option[Client]): Unit =
      stub(clientDao.get _) { case ForIdWithPoi7(`id`, (_, Poi7Value("1"))) =>
        ZIO.succeed(result.toList)
      }

    protected def whenQuotaService(response: Try[Iterable[StoredQuota]]): Unit =
      when(quotaService.get(?)(?)).thenReturn(response)

    protected def whenOfferDaoGet(
        paymentGroup: PaymentGroup,
        clientId: ClientId
    )(offers: Try[List[Offer]]): Unit =
      stub(offerDao.get _) {
        case ForClientPaymentGroupStatus(cId, pg, _)
            if clientId == cId
              && pg.category == paymentGroup.category
              && pg.section == paymentGroup.section =>
          offers
      }

    protected def whenCategorizedOfferDaoGet(
        paymentGroup: PaymentGroup,
        clientId: ClientId
    )(offers: Try[List[Offer]]): Unit =
      stub(categorizedOfferDao.get _) {
        case ForClientPaymentGroupStatus(cId, pg, _)
            if clientId == cId
              && pg.category == paymentGroup.category
              && pg.section == paymentGroup.section =>
          offers
      }

    protected def whenGoodsDaoGet(
        paymentGroup: PaymentGroup
    )(goods: Map[OfferId, Iterable[GoodsDao.Record]]): Unit =
      stub(goodsDao.get _) {
        case ForOfferCategory(offer, category)
            if category.protoParent == paymentGroup.category =>
          Success(goods.getOrElse(offer, Iterable.empty))
      }
  }
}
