package ru.auto.salesman.controller

import org.joda.time.DateTime
import ru.auto.salesman.client.GuardianClient.HoldStates
import ru.auto.salesman.client.{AutoPartsClient, GuardianClient}
import ru.auto.salesman.controller.QuotaController.{
  Activate,
  NoAction,
  NoCampaign,
  Response
}
import ru.auto.salesman.controller.QuotaControllerImplSpec._
import ru.auto.salesman.environment.now
import ru.auto.salesman.exception.NotEnoughFundsException
import ru.auto.salesman.model.QuotaRequest.Settings
import ru.auto.salesman.model._
import ru.auto.salesman.service.QuotaPriceEstimateService.SkippedActivationDuration
import ru.auto.salesman.service._
import ru.auto.salesman.service.impl.StaticCampaignServiceImpl
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.util.{AutomatedContext, DateTimeInterval}
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.Model.Cost.{Constraints, PerIndexing}
import ru.yandex.vertis.billing.Model.Good.Custom
import ru.yandex.vertis.billing.Model._
import ru.yandex.vertis.billing.model.Versions
import ru.yandex.vertis.generators.ProducerProvider.asProducer
import ru.yandex.vertis.mockito.MockitoSupport
import zio.{Task, ZIO}

import scala.concurrent.duration.DurationInt
import scala.util.Success

class QuotaControllerImplSpec extends BaseSpec {

  private val regionId = RegionId(1L)

  private def interval(q: QuotaRequest) =
    QuotaControllerImpl.activateInterval(q)
  private def getDays(q: Quota) = DateTimeInterval(q.from, q.to).duration.toDays

  private val priceEstimator = QuotaPriceEstimateServiceSpec.PriceEstimator
  private val tariffServiceEmpty = TariffServiceSpec.TariffServiceEmptyMock
  private val tariffServicePromo = TariffServiceSpec.TariffServicePromoMock

  private val campaignSource =
    StaticCampaignServiceImpl(
      Iterable(
        campaignHeaderBuilder(enoughMoneyClient, enoughAmount).build(),
        campaignHeaderBuilder(notEnoughMoneyClient, notEnoughAmount).build()
      )
    )

  private val controller = new QuotaControllerImpl(
    priceEstimator,
    tariffServiceEmpty,
    campaignSource,
    holdMock,
    clientSource,
    partsMock,
    promocoderSource
  )

  private val controllerPromo = new QuotaControllerImpl(
    priceEstimator,
    tariffServicePromo,
    campaignSource,
    holdMock,
    clientSource,
    partsMock,
    promocoderSource
  )

  implicit val rc = AutomatedContext("test")

  "QuotaController" should {

    "correctly provide new activation" in {
      val request = QuotaRequestGen.next
        .copy(clientId = enoughMoneyClient.clientId, quotaType = testProduct)
      whenPromocoderSource(
        enoughMoneyClient.clientId,
        ZIO.succeed(List.empty[FeatureInstance])
      )
      controller.activate(request, None).success.value.action match {
        case Activate(quota, _) =>
          quota.price shouldBe quota.revenue
          checkOfferBilling(quota.offerBilling) shouldBe true
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "handle no enough funds" in {
      val request = QuotaRequestGen.next
        .copy(clientId = notEnoughMoneyClient.clientId, quotaType = testProduct)
      whenPromocoderSource(
        notEnoughMoneyClient.clientId,
        ZIO.succeed(List.empty[FeatureInstance])
      )
      intercept[NotEnoughFundsException] {
        controller.activate(request, None).get
      }
    }

    "handle not active client" in {
      val request = QuotaRequestGen.next
        .copy(clientId = notActiveClient.clientId, quotaType = testProduct)
      whenPromocoderSource(
        notActiveClient.clientId,
        ZIO.succeed(List.empty[FeatureInstance])
      )

      controller.activate(request, None) should matchPattern {
        case Success(
              Response(NoAction(None, Some(msg)), _, _, _, _, _, _, _, _, _)
            ) =>
      }
    }

    "handle no campaign" in {
      val request = QuotaRequestGen.next
        .copy(clientId = noCampaignClient.clientId, quotaType = testProduct)
      whenPromocoderSource(
        noCampaignClient.clientId,
        ZIO.succeed(List.empty[FeatureInstance])
      )

      controller.activate(request, None).success.value.action match {
        case NoCampaign(source) =>
          import noCampaignClient._
          source.accountId shouldBe accountId
          source.agency shouldBe balanceAgencyId
          source.client shouldBe balanceClientId
          source.product shouldBe request.quotaType
          info("Done")
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "not activate already activated quota request" in {
      val quota = QuotaGen.next
        .copy(clientId = enoughMoneyClient.clientId, quotaType = testProduct)
      val request = QuotaRequest(
        quota.clientId,
        quotaType = quota.quotaType,
        settings = Settings(quota.size, getDays(quota), None),
        quota.from
      )

      whenPromocoderSource(
        enoughMoneyClient.clientId,
        ZIO.succeed(List.empty[FeatureInstance])
      )

      controller.activate(request, Some(quota)) should matchPattern {
        case Success(
              Response(NoAction(None, Some(msg)), _, _, _, _, _, _, _, _, _)
            ) =>
      }
    }

    "not activate already activated quota request for week quota" in {
      val from = now().minusDays(1).plusHours(1)
      val request = {
        val r = QuotaRequestGen.next
        val settings = r.settings.copy(days = 7)
        r.copy(
          settings = settings,
          from = from,
          clientId = enoughMoneyClient.clientId
        )
      }
      val quota =
        QuotaGen.next.copy(
          clientId = request.clientId,
          regionId = request.regionId,
          from = from,
          to = from.plusDays(7),
          entity = request.settings.entity,
          size = request.settings.size
        )

      whenPromocoderSource(
        enoughMoneyClient.clientId,
        ZIO.succeed(List.empty[FeatureInstance])
      )

      controller.activate(request, Some(quota)) should matchPattern {
        case Success(
              Response(NoAction(None, Some(msg)), _, _, _, _, _, _, _, _, _)
            ) =>
      }
    }

    "prolongation of week quota" in {
      val from = now().minusDays(7).minusMinutes(20)
      val request = {
        val r = QuotaRequestGen.next
        val settings = r.settings.copy(days = 7)
        r.copy(
          settings = settings,
          from = from,
          clientId = enoughMoneyClient.clientId,
          quotaType = testProduct
        )
      }
      val quota =
        QuotaGen.next.copy(
          clientId = request.clientId,
          regionId = request.regionId,
          from = from,
          to = from.plusDays(7),
          entity = request.settings.entity,
          size = request.settings.size
        )

      whenPromocoderSource(
        enoughMoneyClient.clientId,
        ZIO.succeed(List.empty[FeatureInstance])
      )

      controller.activate(request, Some(quota)).success.value.action match {
        case Activate(q, _) =>
          q.size shouldBe request.settings.size
          q.revenue shouldBe
            priceEstimator
              .estimate(request, regionId, interval(request), Some(quota), None)
              .success
              .value
              .revenue
          q.from shouldBe request.from.plusDays(7)
          q.to shouldBe request.from.plusDays(14)
          checkOfferBilling(q.offerBilling) shouldBe true
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "activate update quota" in {
      val quota = QuotaGen.next
        .copy(clientId = enoughMoneyClient.clientId, quotaType = testProduct)
      val request = QuotaRequest(
        quota.clientId,
        quotaType = quota.quotaType,
        settings = Settings(quota.size + 100, getDays(quota), None),
        quota.from.plusMinutes(5)
      )

      whenPromocoderSource(
        enoughMoneyClient.clientId,
        ZIO.succeed(List.empty[FeatureInstance])
      )

      controller.activate(request, Some(quota)).success.value.action match {
        case Activate(q, _) =>
          q.size shouldBe request.settings.size
          q.revenue shouldBe
            priceEstimator
              .estimate(request, regionId, interval(request), Some(quota), None)
              .success
              .value
              .revenue
          q.from shouldBe request.from
          q.to shouldBe request.from.plusSeconds(
            request.settings.duration.toSeconds.toInt
          )
          checkOfferBilling(q.offerBilling) shouldBe true
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "activate prolongation quota" in {
      val quota = QuotaGen.next.copy(
        clientId = enoughMoneyClient.clientId,
        from = new DateTime("2016-12-02T18:32:38.596"),
        to = new DateTime("2016-12-03T18:32:38.596"),
        size = 300,
        revenue = 300L,
        quotaType = testProduct
      )
      val request = QuotaRequest(
        quota.clientId,
        quota.quotaType,
        QuotaRequest.Settings(300, getDays(quota), None),
        new DateTime("2016-12-02T18:32:38.596")
      )
      whenPromocoderSource(
        enoughMoneyClient.clientId,
        ZIO.succeed(List.empty[FeatureInstance])
      )

      controller.activate(request, Some(quota)).success.value.action match {
        case Activate(q, _) =>
          q.size shouldBe request.settings.size
          q.revenue shouldBe
            priceEstimator
              .estimate(request, regionId, interval(request), Some(quota), None)
              .success
              .value
              .revenue
          q.from shouldBe QuotaControllerImpl.activateInterval(request).from
          q.to shouldBe q.from.plusSeconds(
            request.settings.duration.toSeconds.toInt
          )
          checkOfferBilling(q.offerBilling) shouldBe true
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "activate prolongation quota 2" in {
      val quota = QuotaGen.next.copy(
        clientId = enoughMoneyClient.clientId,
        from = new DateTime("2016-12-04T18:32:38.596"),
        to = new DateTime("2016-12-05T18:32:38.596"),
        size = 300,
        revenue = 300L,
        quotaType = testProduct
      )
      val request = QuotaRequest(
        quota.clientId,
        quota.quotaType,
        QuotaRequest.Settings(300, getDays(quota), None),
        new DateTime("2016-12-02T18:32:38.596")
      )
      whenPromocoderSource(
        enoughMoneyClient.clientId,
        ZIO.succeed(List.empty[FeatureInstance])
      )

      controller.activate(request, Some(quota)).success.value.action match {
        case Activate(q, _) =>
          q.size shouldBe request.settings.size
          q.revenue shouldBe
            priceEstimator
              .estimate(request, regionId, interval(request), Some(quota), None)
              .success
              .value
              .revenue
          q.from shouldBe QuotaControllerImpl.activateInterval(request).from
          q.to shouldBe q.from.plusSeconds(
            request.settings.duration.toSeconds.toInt
          )
          checkOfferBilling(q.offerBilling) shouldBe true
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "noCampaign for non created product" in {
      val request = QuotaRequestGen.next
        .copy(
          clientId = enoughMoneyClient.clientId,
          quotaType = nonCreatedProduct
        )

      controller.activate(request, None).success.value.action match {
        case NoCampaign(source) =>
          import enoughMoneyClient._
          source.accountId shouldBe accountId
          source.agency shouldBe balanceAgencyId
          source.client shouldBe balanceClientId
          source.product shouldBe request.quotaType
          info("Done")
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "correctly make premature activation of quota request" in {
      val currentTime = now().plusMinutes(20)
      val quota = QuotaGen.next.copy(
        clientId = enoughMoneyClient.clientId,
        from = currentTime.minusDays(1),
        to = currentTime,
        size = 300,
        revenue = 300L,
        quotaType = testProduct
      )
      val request = QuotaRequest(
        quota.clientId,
        quota.quotaType,
        QuotaRequest.Settings(300, getDays(quota), None),
        quota.from
      )
      whenPromocoderSource(
        enoughMoneyClient.clientId,
        ZIO.succeed(List.empty[FeatureInstance])
      )
      controller.activate(request, Some(quota)).success.value.action match {
        case Activate(q, _) =>
          import enoughMoneyClient._
          q.from shouldBe currentTime
          q.to shouldBe currentTime.plusSeconds(
            request.settings.duration.toSeconds.toInt
          )
          q.clientId shouldBe clientId
          q.size shouldBe request.settings.size
          info("Done")
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "activate correctly quotas after premature activation" in {
      val currentTime = now().plusMinutes(10)
      val quota = QuotaGen.next.copy(
        clientId = enoughMoneyClient.clientId,
        from = currentTime,
        to = currentTime.plusDays(1),
        size = 300,
        revenue = 300L,
        price = 30000L,
        quotaType = testProduct
      )
      val request = QuotaRequest(
        quota.clientId,
        quota.quotaType,
        QuotaRequest.Settings(400, getDays(quota), None),
        currentTime.minusMinutes(10)
      )
      whenPromocoderSource(
        enoughMoneyClient.clientId,
        ZIO.succeed(List.empty[FeatureInstance])
      )

      controller.activate(request, Some(quota)).success.value.action match {
        case Activate(q, _) =>
          import enoughMoneyClient._
          q.from shouldBe request.from
          q.to shouldBe request.from.plusSeconds(
            request.settings.duration.toSeconds.toInt
          )
          q.clientId shouldBe clientId
          q.size shouldBe request.settings.size
          q.revenue shouldBe 10000
          q.price shouldBe 40000
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "not make premature activation" in {
      val currentTime = now().plusMinutes(31)
      val quota = QuotaGen.next.copy(
        clientId = enoughMoneyClient.clientId,
        from = currentTime.minusDays(1),
        to = currentTime,
        size = 300,
        revenue = 300L,
        quotaType = testProduct
      )
      val request = QuotaRequest(
        quota.clientId,
        quota.quotaType,
        QuotaRequest.Settings(300, getDays(quota), None),
        quota.from
      )
      whenPromocoderSource(
        enoughMoneyClient.clientId,
        ZIO.succeed(List.empty[FeatureInstance])
      )

      controller.activate(request, Some(quota)) should matchPattern {
        case Success(
              Response(NoAction(None, Some(msg)), _, _, _, _, _, _, _, _, _)
            ) =>
      }
    }

    "activate quota with promo tariff" in {
      val quota = QuotaGen.next
        .copy(clientId = enoughMoneyClient.clientId, quotaType = testProduct)

      val request = QuotaRequest(
        quota.clientId,
        quotaType = quota.quotaType,
        settings = Settings(quota.size + 100, getDays(quota), None),
        quota.from.plusMinutes(5)
      )
      whenPromocoderSource(
        enoughMoneyClient.clientId,
        ZIO.succeed(List.empty[FeatureInstance])
      )

      controllerPromo
        .activate(request, Some(quota))
        .success
        .value
        .action match {
        case Activate(q, _) =>
          q.size shouldBe request.settings.size
          q.revenue shouldBe
            priceEstimator
              .estimate(
                request,
                regionId,
                interval(request),
                Some(quota),
                Some(TariffTypes.LuxaryMsk)
              )
              .success
              .value
              .revenue
          q.from shouldBe request.from
          q.to shouldBe request.from.plusSeconds(
            request.settings.duration.toSeconds.toInt
          )
          checkOfferBilling(q.offerBilling) shouldBe true
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "correctly provide new activation by promocode" in {
      val request = QuotaRequestGen.next
        .copy(clientId = enoughMoneyClient.clientId, quotaType = testProduct)

      val featureId = s"$testProduct:promo_salesman-test:96eb92e69602f216"
      val featureTag = testProduct.toString
      val testUser =
        PromocoderUser(enoughMoneyClient.clientId, UserTypes.ClientUser)
      val featurePayload = FeaturePayload(FeatureUnits.Items)
      val featureCount = FeatureCount(10L, FeatureUnits.Items)
      val featureOrigin = FeatureOrigin("origin")
      val feature = FeatureInstance(
        featureId,
        featureOrigin,
        featureTag,
        testUser.toString,
        featureCount,
        now.minusDays(1),
        now.plusDays(2),
        featurePayload
      )

      whenPromocoderSource(
        enoughMoneyClient.clientId,
        ZIO.succeed(List(feature))
      )

      controller.activate(request, None).success.value.action match {
        case Activate(quota, _) =>
          quota.revenue shouldBe 0
          checkOfferBillingByPromocode(quota.offerBilling) shouldBe true
        case other =>
          fail(s"Unexpected $other")
      }
    }
  }

  "fail when feature get failed" in {
    val request = QuotaRequestGen.next
      .copy(clientId = enoughMoneyClient.clientId, quotaType = testProduct)
    whenPromocoderSource(enoughMoneyClient.clientId, ZIO.fail(new Exception))

    val result =
      controller.activate(request, lastActivation = None).failure.exception
    result shouldBe an[Exception]
  }

  "correctly activations with skip" in {
    val settings = QuotaSettingsGen.next.copy(price = Some(10000L), days = 1)
    val request = QuotaRequestGen.next
      .copy(
        clientId = enoughMoneyClient.clientId,
        quotaType = testProduct,
        from = now().minusMinutes(330),
        settings = settings
      )

    val laFrom = request.from.minusDays(2)
    val la = Quota(
      request.clientId,
      request.quotaType,
      request.settings.size,
      request.settings.price.get,
      request.settings.price.get,
      laFrom,
      laFrom.plusSeconds(request.settings.duration.toSeconds.toInt),
      QuotaGen.next.offerBilling,
      None
    )
    whenPromocoderSource(enoughMoneyClient.clientId, ZIO.succeed(List.empty))
    val expectedDiscountHours = 5
    controller.activate(request, Some(la)).success.value.action match {
      case Activate(quota, _) =>
        quota.price > quota.revenue shouldBe true
        quota.revenue shouldBe (quota.price - (quota.price * expectedDiscountHours / 24 / 100 * 100))
        checkOfferBilling(quota.offerBilling) shouldBe true
      case other =>
        fail(s"Unexpected $other")
    }
  }

  "correctly activations with no quota size update" in {
    val settings =
      QuotaSettingsGen.next.copy(size = 100, price = Some(10000L), days = 1)
    val request = QuotaRequestGen.next
      .copy(
        clientId = enoughMoneyClient.clientId,
        quotaType = testProduct,
        from = now(),
        settings = settings
      )

    val laFrom = now().minusMinutes(330)
    val la = Quota(
      request.clientId,
      request.quotaType,
      request.settings.size,
      request.settings.price.get,
      request.settings.price.get,
      laFrom,
      laFrom.plusSeconds(settings.duration.toSeconds.toInt),
      QuotaGen.next.offerBilling,
      None
    )
    whenPromocoderSource(enoughMoneyClient.clientId, ZIO.succeed(List.empty))
    val expectedDiscountHours = 18
    controller.activate(request, Some(la)).success.value.action match {
      case Activate(quota, _) =>
        val discount = quota.price - quota.revenue
        val expectedDiscount = la.price * expectedDiscountHours / 24 / 100 * 100
        discount shouldBe expectedDiscount
        checkOfferBilling(quota.offerBilling) shouldBe true
      case other =>
        fail(s"Unexpected $other")
    }
  }

  "first activation with skipped time more than SkippedActivationDuration" in {
    val settings = QuotaSettingsGen.next.copy(days = 1)
    val request = QuotaRequestGen.next
      .copy(
        clientId = enoughMoneyClient.clientId,
        quotaType = testProduct,
        from = now().minusMinutes(
          SkippedActivationDuration.plus(90.minutes).toMinutes.toInt
        ),
        settings = settings
      )
    whenPromocoderSource(enoughMoneyClient.clientId, ZIO.succeed(List.empty))
    val expectedDiscountHours = 5
    controller.activate(request, None).success.value.action match {
      case Activate(quota, _) =>
        quota.price > quota.revenue shouldBe true
        quota.revenue shouldBe (quota.price - (quota.price * expectedDiscountHours / 24 / 100 * 100))
        checkOfferBilling(quota.offerBilling) shouldBe true
      case other =>
        fail(s"Unexpected $other")
    }
  }

  "first activation with skipped time less than SkippedActivationDuration" in {
    val settings = QuotaSettingsGen.next
    val request = QuotaRequestGen.next
      .copy(
        clientId = enoughMoneyClient.clientId,
        quotaType = testProduct,
        from = now().minusMinutes(
          SkippedActivationDuration.minus(90.minutes).toMinutes.toInt
        ),
        settings = settings
      )
    whenPromocoderSource(enoughMoneyClient.clientId, ZIO.succeed(List.empty))
    controller.activate(request, None).success.value.action match {
      case Activate(quota, _) =>
        quota.price shouldBe quota.revenue
        checkOfferBilling(quota.offerBilling) shouldBe true
      case other =>
        fail(s"Unexpected $other")
    }
  }

}

object QuotaControllerImplSpec {

  import MockitoSupport.{mock, stub, when}

  val testProduct = ProductId.QuotaPlacementCarsUsed
  val nonCreatedProduct = ProductId.QuotaPlacementCarsNewPremium
  val enoughAmount: Funds = 1000000
  val notEnoughAmount: Funds = 10

  private val enoughMoneyClient =
    DetailedClient(
      1L,
      None,
      2,
      None,
      None,
      None,
      RegionId(0L),
      CityId(1123L),
      3,
      isActive = true,
      firstModerated = false,
      singlePayment = Set.empty[AdsRequestType]
    )

  private val notEnoughMoneyClient =
    DetailedClient(
      4L,
      None,
      5,
      None,
      None,
      None,
      RegionId(0L),
      CityId(1123L),
      6,
      isActive = true,
      firstModerated = false,
      singlePayment = Set.empty[AdsRequestType]
    )

  private val noCampaignClient =
    DetailedClient(
      7L,
      None,
      8,
      None,
      None,
      None,
      RegionId(0L),
      CityId(1123L),
      9,
      isActive = true,
      firstModerated = false,
      singlePayment = Set.empty[AdsRequestType]
    )

  private val notActiveClient =
    DetailedClient(
      10L,
      None,
      11L,
      None,
      None,
      None,
      RegionId(0L),
      CityId(1123L),
      12,
      isActive = false,
      firstModerated = false,
      singlePayment = Set.empty[AdsRequestType]
    )

  private val clients =
    Map(
      enoughMoneyClient.clientId -> enoughMoneyClient,
      notEnoughMoneyClient.clientId -> notEnoughMoneyClient,
      noCampaignClient.clientId -> noCampaignClient,
      notActiveClient.clientId -> notActiveClient
    )

  private def checkOfferBilling(ob: OfferBilling): Boolean = {
    val hasCampaign = ob.hasKnownCampaign && ob.getKnownCampaign.hasCampaign
    val hasHoldId = ob.getKnownCampaign.hasHold
    val hasDeadline = ob.getKnownCampaign.hasActiveDeadline
    (hasCampaign ::
      hasHoldId ::
      hasDeadline ::
      Nil).forall(_ == true)
  }

  private def checkOfferBillingByPromocode(ob: OfferBilling): Boolean = {
    val hasCampaign = ob.hasKnownCampaign && ob.getKnownCampaign.hasCampaign
    val hasNotHoldId = !ob.getKnownCampaign.hasHold
    val hasDeadline = ob.getKnownCampaign.hasActiveDeadline
    (hasCampaign ::
      hasDeadline ::
      hasNotHoldId ::
      Nil).forall(_ == true)
  }

  private def campaignHeaderBuilder(client: DetailedClient, amount: Long) = {
    val customerId = CustomerId
      .newBuilder()
      .setVersion(Versions.CUSTOMER_ID)
      .setClientId(client.balanceClientId)
    val owner = CustomerHeader
      .newBuilder()
      .setVersion(Versions.CUSTOMER_HEADER)
      .setId(customerId)
    client.balanceAgencyId.foreach(customerId.setAgencyId)
    val order = Order
      .newBuilder()
      .setVersion(Versions.ORDER)
      .setId(client.accountId)
      .setOwner(customerId)
      .setText(" ")
      .setCommitAmount(amount)
      .setApproximateAmount(amount)
      .setTotalIncome(amount)
    val good = {
      val constraints = Constraints
        .newBuilder()
        .setCostType(CostType.COSTPERINDEXING)
      val perIndexing = PerIndexing
        .newBuilder()
        .setConstraints(constraints)
      val cost = Cost
        .newBuilder()
        .setVersion(Versions.COST)
        .setPerIndexing(perIndexing)
      val custom = Custom
        .newBuilder()
        .setCost(cost)
        .setId(ProductId.alias(testProduct))
      Good
        .newBuilder()
        .setVersion(Versions.GOOD)
        .setCustom(custom)
    }
    val product = Model.Product
      .newBuilder()
      .setVersion(Versions.PRODUCT)
      .addGoods(good)
    val settings = CampaignSettings
      .newBuilder()
      .setVersion(Versions.CAMPAIGN_SETTINGS)
      .setIsEnabled(true)
    CampaignHeader
      .newBuilder()
      .setVersion(Versions.CAMPAIGN_HEADER)
      .setId(randomAlphanumericString(5))
      .setOrder(order)
      .setProduct(product)
      .setSettings(settings)
      .setOwner(owner)
  }

  private def holdMock = {
    val m = mock[GuardianClient]

    stub(m.hold _) {
      case source if source.accountId == notEnoughMoneyClient.accountId =>
        Success(HoldStates.NotEnoughFunds)
      case source =>
        Success(HoldStates.Ok)
    }
    m
  }

  private val partsMock = mock[AutoPartsClient]

  private def clientSource = {
    val m = mock[DetailedClientSource]

    stub(m.unsafeResolve(_: Long, _: Boolean)) { case (c, false) =>
      ZIO.succeed(clients(c))
    }
    m
  }

  private val promocoderSource = mock[PromocoderSource]

  private def whenPromocoderSource(
      clientId: ClientId,
      response: Task[List[FeatureInstance]]
  ) = {
    val testUser = PromocoderUser(clientId, UserTypes.ClientUser)
    when(
      promocoderSource.getFeaturesForUser(MockitoSupport.eq(testUser))
    ).thenReturn(response)
  }
}
