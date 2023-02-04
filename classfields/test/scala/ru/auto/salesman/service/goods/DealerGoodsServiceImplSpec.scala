package ru.auto.salesman.service.goods

import cats.data.NonEmptyList
import com.github.nscala_time.time.Imports._
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.Section.{NEW, USED}
import ru.auto.salesman.api.akkahttp.ApiException.BadRequestException
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.dao.GoodsDao.Filter.ForOffersCategories
import ru.auto.salesman.dao.impl.jdbc.database.Database
import ru.auto.salesman.dao.{BadgeDao, ClientDao, GoodsDao}
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.AdsRequestTypes.{CarsUsed, Commercial}
import ru.auto.salesman.model.GoodStatuses.Active
import ru.auto.salesman.model.OfferCategories.{Cars, Moto, Trucks}
import ru.auto.salesman.model.ProductId._
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.payment_model.PaymentModel.Quota
import ru.auto.salesman.model.payment_model._
import ru.auto.salesman.model.{OfferId, RegionId}
import ru.auto.salesman.service.DealerFeatureService
import ru.auto.salesman.service.client.ClientService.ClientServiceError
import ru.auto.salesman.service.client.ClientServiceImpl
import ru.auto.salesman.service.goods.domain._
import ru.auto.salesman.service.goods.provider.{
  BadgeProvider,
  FreshProvider,
  GeneralServiceProvider
}
import ru.auto.salesman.service.impl.GoodsDaoProviderImpl
import ru.auto.salesman.test.dao.gens._
import ru.auto.salesman.test.gens._
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.service.payment_model.TestPaymentModelFactory
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.yandex.vertis.generators.ProducerProvider.asProducer

import scala.util.Try

class DealerGoodsServiceImplSpec extends BaseSpec {

  private val goodsDao = mock[GoodsDao]
  private val categorizedGoodsDao = mock[GoodsDao]
  private val goodsDaoProvider = new GoodsDaoProviderImpl(goodsDao, categorizedGoodsDao)
  private val badgeDao = mock[BadgeDao]
  private val clientDao = mock[ClientDao]
  private val clientService = new ClientServiceImpl(clientDao)

  private val paymentModelFactory = TestPaymentModelFactory.withoutSingleWithCalls()

  private val featureService = mock[DealerFeatureService]

  private val vosClient = mock[VosClient]

  private val database = mock[Database]
  private val badgeProvider = new BadgeProvider(badgeDao, goodsDaoProvider, database)

  private val freshProvider =
    new FreshProvider(goodsDaoProvider, database, paymentModelFactory)

  private val generalServiceProvider = new GeneralServiceProvider(
    paymentModelFactory,
    goodsDaoProvider,
    featureService
  )

  private val goodsService =
    new DealerGoodsServiceImpl(
      goodsDaoProvider,
      badgeDao,
      clientService,
      vosClient,
      featureService,
      badgeProvider,
      freshProvider,
      generalServiceProvider
    )

  private val autoruOfferId = AutoruOfferId("1043045004-977b3")
  private val offerId = 1043045004L
  private val offerHash = "977b3"
  private val clientRegionId = RegionId(1)

  "GoodsServiceImpl" should {
    "get recent goods" in {
      forAll(
        RecentFilterGen,
        Gen.listOf(GoodRecordGen),
        Gen.listOf(GoodRecordGen)
      ) { (recentFilter, goods, categorizedGoods) =>
        (goodsDao.getZIO _)
          .expects(recentFilter)
          .returningZ(goods)

        (categorizedGoodsDao.getZIO _)
          .expects(recentFilter)
          .returningZ(categorizedGoods)

        goodsService
          .getRecent(
            recentFilter.since,
            recentFilter.excludedProducts,
            recentFilter.status
          )
          .success
          .value shouldBe (goods ++ categorizedGoods)
      }
    }

    "ignore all_sale_badge without badge from dao" in {
      forAll(Gen.nonEmptyListOf(Gen.posNum[OfferId]), Gen.listOf(GoodRecordGen)) {
        (offerIds, goods) =>
          (goodsDao.getZIO _)
            .expects(ForOffersCategories(offerIds, NonEmptyList.of(Cars)))
            .returningZ(goods)

          val badges = goods
            .filter(_.product == Badge)
            .map { goods =>
              val optBadge = Gen.option(Gen.alphaNumStr).next
              (badgeDao.getBadge _)
                .expects(goods.primaryKeyId, goods.offerId, goods.category)
                .returningZ(optBadge)
              goods -> optBadge
            }
            .toMap

          val expected =
            goods.flatMap { goods =>
              Try(
                GoodsDetails(
                  goods.offerId,
                  goods.category,
                  goods.product,
                  goods.createDate,
                  goods.expireDate,
                  badges.get(goods).flatten,
                  Some(goods.offerHash),
                  goods.epoch
                )
              ).toOption
            }

          goodsService
            .get(offerIds, Cars)
            .success
            .value should contain theSameElementsAs expected
      }
    }
  }

  "GoodsServiceImpl.add(goods)" should {

    "return quota placement without creating it" in {
      forAll(clientRecordGen()) { client =>
        val testStart = now()
        (clientDao.get _)
          .expects(ClientDao.ForId(client.clientId))
          .returningZ(List(client))
        (featureService.checkDealerOffersOwnership _).expects().returning(true)
        (vosClient
          .checkOffersBelong(_, _))
          .expects(*, *)
          .returningZ(true)

        val request = GoodsRequest(autoruOfferId, Moto, NEW, Placement, Set.empty)
        val List(result) = goodsService.add(client.clientId, List(request)).success.value
        result.offerId shouldBe offerId
        result.category shouldBe Moto
        result.product shouldBe Placement
        result.createDate should be >= testStart
        result.expireDate shouldBe None
        result.badge shouldBe None
        result.offerHash.value shouldBe offerHash
        result.epoch.value should be >= testStart.getMillis
        result.paymentModel should be(Quota)
      }
    }

    "create single cars:used placement" in {
      forAll(
        clientRecordGen(singlePaymentsGen = adsRequestTypesGenWith(CarsUsed)),
        goodRecordGen(offerId, offerHash, Cars, USED, Placement)
      ) { (client, good) =>
        val testStart = now()
        (clientDao.get _)
          .expects(ClientDao.ForId(client.clientId))
          .returningZ(List(client))
        (featureService.checkDealerOffersOwnership _).expects().returning(true)
        (vosClient
          .checkOffersBelong(_, _))
          .expects(*, *)
          .returningZ(true)
        (goodsDao.insert _)
          .expects {
            argAssert { source: GoodsDao.Source =>
              source.offerId shouldBe offerId
              source.product shouldBe Placement
              source.status shouldBe Active
              source.extraData shouldBe ""
              source.activateDate should be >= testStart
              val details = source.details.value
              details.offerHash.value shouldBe offerHash
              details.category shouldBe Cars
              details.section shouldBe USED
              details.clientId shouldBe client.clientId
            }
          }
          .returningZ(good)
        val request = GoodsRequest(autoruOfferId, Cars, USED, Placement, Set.empty)
        val List(result) =
          goodsService.add(client.clientId, List(request)).success.value
        result.offerId shouldBe offerId
        result.category shouldBe Cars
        result.product shouldBe Placement
        result.createDate shouldBe good.createDate
        result.expireDate shouldBe good.expireDate
        result.badge shouldBe None
        result.offerHash.value shouldBe offerHash
        result.epoch shouldBe good.epoch
        result.paymentModel shouldBe PaymentModel.Single
      }
    }

    "create single trucks placement" in {
      val goodGen =
        goodRecordGen(offerId, offerHash, Trucks, productGen = Placement)
      val clientGen =
        clientRecordGen(singlePaymentsGen = adsRequestTypesGenWith(Commercial))
      forAll(clientGen, goodGen) { (client, good) =>
        val testStart = now()
        (clientDao.get _)
          .expects(ClientDao.ForId(client.clientId))
          .returningZ(List(client))
        (featureService.checkDealerOffersOwnership _).expects().returning(true)
        (vosClient
          .checkOffersBelong(_, _))
          .expects(*, *)
          .returningZ(true)
        (categorizedGoodsDao.insert _)
          .expects {
            argAssert { source: GoodsDao.Source =>
              source.offerId shouldBe offerId
              source.product shouldBe Placement
              source.status shouldBe Active
              source.extraData shouldBe ""
              source.activateDate should be >= testStart
              val details = source.details.value
              details.offerHash.value shouldBe offerHash
              details.category shouldBe Trucks
              details.section shouldBe good.section
              details.clientId shouldBe client.clientId
            }
          }
          .returningZ(good)
        val request =
          GoodsRequest(autoruOfferId, Trucks, good.section, Placement, Set.empty)
        val List(result) =
          goodsService.add(client.clientId, List(request)).success.value
        result.offerId shouldBe offerId
        result.category shouldBe Trucks
        result.product shouldBe Placement
        result.createDate shouldBe good.createDate
        result.expireDate shouldBe good.expireDate
        result.badge shouldBe None
        result.offerHash.value shouldBe offerHash
        result.epoch shouldBe good.epoch
        result.paymentModel shouldBe PaymentModel.Single
      }
    }

    "create non-placement cars product" in {
      val productGen = Gen.oneOf(Premium, Top, Special, Color)
      forAll(
        clientRecordGen(),
        goodRecordGen(offerId, offerHash, Cars, productGen = productGen)
      ) { (client, good) =>
        val testStart = now()
        (clientDao.get _)
          .expects(ClientDao.ForId(client.clientId))
          .returningZ(List(client))
        (featureService.checkDealerOffersOwnership _).expects().returning(true)
        (vosClient
          .checkOffersBelong(_, _))
          .expects(*, *)
          .returningZ(true)
        (goodsDao.insert _)
          .expects {
            argAssert { source: GoodsDao.Source =>
              source.offerId shouldBe offerId
              source.product shouldBe good.product
              source.status shouldBe Active
              source.extraData shouldBe ""
              source.activateDate should be >= testStart
              val details = source.details.value
              details.offerHash.value shouldBe offerHash
              details.category shouldBe Cars
              details.section shouldBe good.section
              details.clientId shouldBe client.clientId
            }
          }
          .returningZ(good)
        (featureService.regionsDealerDisabledBoostAndPremium _)
          .expects()
          .noMoreThanOnce()
          .returning(Set.empty[RegionId])
        val request =
          GoodsRequest(autoruOfferId, Cars, good.section, good.product, Set.empty)
        val List(result) =
          goodsService.add(client.clientId, List(request)).success.value
        result.offerId shouldBe offerId
        result.category shouldBe Cars
        result.product shouldBe good.product
        result.createDate shouldBe good.createDate
        result.expireDate shouldBe good.expireDate
        result.badge shouldBe None
        result.offerHash.value shouldBe offerHash
        result.epoch shouldBe good.epoch
        result.paymentModel shouldBe PaymentModel.Single
      }
    }

    "not allow create non-placement cars product by region" in {
      val productGen = Gen.oneOf(Turbo, Premium)
      forAll(
        clientRecordGen(clientRegionId),
        goodRecordGen(offerId, offerHash, Cars, USED, productGen = productGen)
      ) { (client, good) =>
        (clientDao.get _)
          .expects(ClientDao.ForId(client.clientId))
          .returningZ(List(client))
        (featureService.checkDealerOffersOwnership _).expects().returning(true)
        (vosClient
          .checkOffersBelong(_, _))
          .expects(*, *)
          .returningZ(true)
        (featureService.regionsDealerDisabledBoostAndPremium _)
          .expects()
          .noMoreThanOnce()
          .returning(Set(clientRegionId))
        (featureService.regionsDealerDisabledTurbo _)
          .expects()
          .noMoreThanOnce()
          .returning(Set(clientRegionId))
        val request =
          GoodsRequest(autoruOfferId, Cars, good.section, good.product, Set.empty)
        val result = goodsService.add(client.clientId, List(request)).failure.exception
        result shouldBe a[IllegalProductForRegion]
        result.getMessage shouldBe s"Product ${good.product} is illegal for region $clientRegionId"
      }
    }

    "create non-placement trucks product" in {
      val productGen = Gen.oneOf(Premium, Top, Special, Color)
      forAll(
        clientRecordGen(),
        goodRecordGen(offerId, offerHash, Trucks, productGen = productGen)
      ) { (client, good) =>
        val testStart = now()
        (clientDao.get _)
          .expects(ClientDao.ForId(client.clientId))
          .returningZ(List(client))
        (featureService.checkDealerOffersOwnership _).expects().returning(true)
        (vosClient
          .checkOffersBelong(_, _))
          .expects(*, *)
          .returningZ(true)
        (categorizedGoodsDao.insert _)
          .expects {
            argAssert { source: GoodsDao.Source =>
              source.offerId shouldBe offerId
              source.product shouldBe good.product
              source.status shouldBe Active
              source.extraData shouldBe ""
              source.activateDate should be >= testStart
              val details = source.details.value
              details.offerHash.value shouldBe offerHash
              details.category shouldBe Trucks
              details.section shouldBe good.section
              details.clientId shouldBe client.clientId
            }
          }
          .returningZ(good)
        val request =
          GoodsRequest(autoruOfferId, Trucks, good.section, good.product, Set.empty)
        val List(result) =
          goodsService.add(client.clientId, List(request)).success.value
        result.offerId shouldBe offerId
        result.category shouldBe Trucks
        result.product shouldBe good.product
        result.createDate shouldBe good.createDate
        result.expireDate shouldBe good.expireDate
        result.badge shouldBe None
        result.offerHash.value shouldBe offerHash
        result.epoch shouldBe good.epoch
        result.paymentModel shouldBe PaymentModel.Single
      }
    }

    "return error if client not found" in {
      forAll(Gen.listOf(goodsRequestGen())) { requests =>
        val clientId = 20101
        (clientDao.get _).expects(ClientDao.ForId(clientId)).returningZ(Nil)
        val result = goodsService.add(clientId, requests).failure.exception
        result shouldBe a[ClientServiceError.ClientNotFound]
      }
    }

    "return error if good adding failed" in {
      val goodsRequestsGen = Gen.nonEmptyListOf(
        goodsRequestGen(productGen = Gen.oneOf(Premium, Top, Special, Color))
      )
      forAll(clientRecordGen(), goodsRequestsGen) { (client, requests) =>
        (clientDao.get _)
          .expects(ClientDao.ForId(client.clientId))
          .returningZ(List(client))
        (featureService.checkDealerOffersOwnership _).expects().returning(true)
        (vosClient
          .checkOffersBelong(_, _))
          .expects(*, *)
          .returningZ(true)
        (goodsDao.insert _)
          .expects(*)
          .anyNumberOfTimes()
          .throwingZ(new TestException)
        (categorizedGoodsDao.insert _)
          .expects(*)
          .anyNumberOfTimes()
          .throwingZ(new TestException)
        (featureService.regionsDealerDisabledBoostAndPremium _)
          .expects()
          .noMoreThanOnce()
          .returning(Set.empty[RegionId])
        val result =
          goodsService.add(client.clientId, requests).failure.exception
        result shouldBe a[TestException]
      }
    }

    "return error on not owned offer" in {

      forAll(clientRecordGen(), goodsRequestGen()) { (client, requests) =>
        (clientDao.get _)
          .expects(ClientDao.ForId(client.clientId))
          .returningZ(List(client))

        (featureService.checkDealerOffersOwnership _).expects().returning(true)
        (vosClient
          .checkOffersBelong(_, _))
          .expects(*, *)
          .returningZ(false)

        val result = goodsService.add(client.clientId, List(requests)).failure.exception
        result shouldBe a[BadRequestException]
      }
    }
  }
}
