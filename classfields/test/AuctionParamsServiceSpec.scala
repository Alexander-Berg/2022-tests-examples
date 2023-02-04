package vsmoney.auction.services.test

import common.models.finance.Money.Kopecks
import vsmoney.auction.converters.all._
import vsmoney.auction.model._
import vsmoney.auction.services.UserAuctionService.AuctionError.AuctionParamsNotFoundException
import vsmoney.auction.services.{AuctionParamsService, GeoIds}
import vsmoney.auction.services.impl.AuctionParamsServiceLive
import vsmoney.auction.services.impl.AuctionParamsServiceLive.{
  DefaultFirstStep,
  DefaultFirstStepCallCarsUsed,
  DefaultNextStep,
  DefaultNextStepCallCarsUsed
}
import vsmoney.auction.services.testkit.ParamsSourceMock
import vsmoney.auction.storage.ParamsSource.AuctionPalmaParams
import zio.ZIO
import zio.test.Assertion.{anything, equalTo, fails, isSubtype}
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._
import zio.test.{assert, assertM, DefaultRunnableSpec, ZSpec}

object AuctionParamsServiceSpec extends DefaultRunnableSpec {

  def getAuctionParamMock(key: AuctionKey, k: String) = ParamsSourceMock.Get(
    equalTo(key),
    value(
      Some(
        AuctionPalmaParams(
          k,
          FirstStep.BasePricePlusAmount(Kopecks(400L)),
          NextStep.ArithmeticProgression(Kopecks(1000L))
        )
      )
    )
  )

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("AuctionParamsService")(
      testM("should return default value if auction not found") {
        val key = createKey(("key", "value"))
        val default = AuctionParams(
          key,
          DefaultFirstStep,
          DefaultNextStep
        )

        ZIO
          .service[AuctionParamsService]
          .flatMap(_.get(key))
          .map(assert(_)(equalTo(default)))
          .provideLayer(ParamsSourceMock.Get(equalTo(key), value(None)) >>> AuctionParamsServiceLive.live)
      },
      testM("should return auction params if found") {
        val key = createKey(("key", "value"))
        val result = AuctionParams(
          key,
          FirstStep.BasePricePlusAmount(Kopecks(400L)),
          NextStep.ArithmeticProgression(Kopecks(1000L))
        )

        (for {
          k <- createKey(("key", "value")).convert[String]
        } yield getAuctionParamMock(key, k).toLayer >>> AuctionParamsServiceLive.live).flatMap(a =>
          ZIO
            .service[AuctionParamsService]
            .flatMap(_.get(key))
            .map(assert(_)(equalTo(result)))
            .provideLayer(a)
        )
      },
      testM("should return default base price = 49900 and step = 5000 if regionId = RegMoscow") {
        testBasePriceAndStepForRegion(regionId = GeoIds.RegMoscow, basePrice = 49900, step = 5000)
      },
      testM("should return default base price = 34900 and step = 5000 if regionId = RegSPb") {
        testBasePriceAndStepForRegion(regionId = GeoIds.RegSPb, basePrice = 34900, step = 5000)
      },
      testM("should return default base price = 39900 and step = 5000 if regionId = RegChelyabinsk") {
        testBasePriceAndStepForRegion(regionId = GeoIds.RegChelyabinsk, basePrice = 39900, step = 5000)
      },
      testM("should return default base price = 39900 and step = 5000 if regionId = RegSverdlovsk") {
        testBasePriceAndStepForRegion(regionId = GeoIds.RegSverdlovsk, basePrice = 39900, step = 5000)
      },
      testM("should throw Exception if product = lbu and not make region") {
        val auctionObject = Criterion(
          CriterionKey("t"),
          CriterionValue("v")
        )
        val key = createKey(auctionObject, (CriterionKey.regionId, GeoIds.RegAltay), ("key", "value"))
          .copy(product = ProductId(ProductId.CarsUsedProductId))

        val res = ZIO
          .service[AuctionParamsService]
          .flatMap(_.get(key))
          .provideLayer(ParamsSourceMock.Get(equalTo(key), value(None)) >>> AuctionParamsServiceLive.live)
        assertM(res.run)(fails(isSubtype[AuctionParamsNotFoundException](anything)))

      }
    )
  }

  private def createKey(kv: (String, String)*): AuctionKey =
    AuctionKey(
      Project.Autoru,
      ProductId("call"),
      CriteriaContext(kv.map { case (k, v) => Criterion(CriterionKey(k), CriterionValue(v)) }),
      auctionObject = None
    )

  private def createKey(auctionObject: Criterion, kv: (String, String)*): AuctionKey =
    AuctionKey(
      Project.Autoru,
      ProductId("call"),
      CriteriaContext(kv.map { case (k, v) => Criterion(CriterionKey(k), CriterionValue(v)) }),
      auctionObject = Some(auctionObject)
    )

  private def testBasePriceAndStepForRegion(regionId: String, basePrice: Long, step: Long) = {
    val auctionObject = Criterion(
      CriterionKey("t"),
      CriterionValue("v")
    )
    val key = createKey(auctionObject, (CriterionKey.regionId, regionId), ("key", "value"))
      .copy(product = ProductId(ProductId.CarsUsedProductId))
    val default = AuctionParams(
      key,
      FirstStep.BasePricePlusAmount(Kopecks(basePrice)),
      NextStep.ArithmeticProgression(Kopecks(step))
    )

    ZIO
      .service[AuctionParamsService]
      .flatMap(_.get(key))
      .map(assert(_)(equalTo(default)))
      .provideLayer(ParamsSourceMock.Get(equalTo(key), value(None)) >>> AuctionParamsServiceLive.live)
  }
}
