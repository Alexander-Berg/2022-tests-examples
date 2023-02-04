package ru.auto.salesman.service.user

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import ru.auto.salesman.dao.user.GoodsDao
import ru.auto.salesman.dao.user.GoodsDao.Filter.ProlongIntervalIsNotEnded
import ru.auto.salesman.model.ProductDuration.days
import ru.auto.salesman.model.ProductId.Placement
import ru.auto.salesman.model.{
  DeprecatedDomain,
  DeprecatedDomains,
  Funds,
  ProductStatuses
}
import ru.auto.salesman.model.user.{ProlongIntervalInfo, Prolongable}
import ru.auto.salesman.service.PriceExtractor.ProductInfo
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.{ServiceModelGenerators, UserModelGenerators}
import ru.auto.salesman.util.HasRequestContext
import ru.auto.salesman.util.money.Money.Kopecks
import zio.blocking.Blocking
import zio.test.environment.{TestClock, TestEnvironment}

class ProlongIntervalServiceSpec
    extends BaseSpec
    with UserModelGenerators
    with ServiceModelGenerators {
  private val goodsDao = mock[GoodsDao]
  private val modifyPriceService = stub[ModifyPriceService]
  private val experimentSelectServiceMock = stub[ExperimentSelectService]

  private val prolongIntervalService = new ProlongIntervalService(
    goodsDao,
    modifyPriceService,
    experimentSelectServiceMock
  )

  "ProlongIntervalService.getProlongIntervalInfo" should {

    "use correct interval and return prolongIntervalInfo" in {
      forAll(
        OfferIdentityGen,
        ProductGen,
        goodsGen(
          context = goodsContextGen(productPrice =
            productPriceGen(
              price = constPriceGen(prolongPrice = Some(100: Funds))
            )
          )
        )
      ) { (offerId, product, good) =>
        val now = OffsetDateTime.parse(
          "2020-03-19T12:00:00+00:00",
          DateTimeFormatter.ISO_OFFSET_DATE_TIME
        )

        val jodaParser = ISODateTimeFormat.dateTimeParser()
        val jodaBefore = jodaParser.parseDateTime("2020-03-16T12:00:00+00:00")
        val start = jodaBefore
        (goodsDao.get _)
          .expects(ProlongIntervalIsNotEnded(offerId, product, start))
          .returningZ(Iterable(good))
        val res =
          (TestClock.setDateTime(now) *>
            prolongIntervalService
              .fetchProlongIntervalInfo(offerId, product))
            .provideSomeLayer[Blocking with HasRequestContext](
              zio.ZEnv.live >>> TestEnvironment.live
            )
            .success
            .value
        res.headOption shouldBe Some(
          ProlongIntervalInfo(
            100: Funds,
            good.deadline.plus(ProlongIntervalService.prolongInterval),
            good.context.productPrice
          )
        )
      }
    }

    "return None if prolongationPrice is empty" in {
      forAll(
        OfferIdentityGen,
        ProductGen,
        goodsGen(
          context = goodsContextGen(productPrice =
            productPriceGen(price = constPriceGen(prolongPrice = None))
          )
        )
      ) { (offerId, product, good) =>
        val now = OffsetDateTime.parse(
          "2020-03-19T12:00:00+00:00",
          DateTimeFormatter.ISO_OFFSET_DATE_TIME
        )

        val jodaParser = ISODateTimeFormat.dateTimeParser()
        val jodaBefore = jodaParser.parseDateTime("2020-03-16T12:00:00+00:00")
        val start = jodaBefore
        (goodsDao.get _)
          .expects(ProlongIntervalIsNotEnded(offerId, product, start))
          .returningZ(Iterable(good))
        val res =
          (TestClock.setDateTime(now) *>
            prolongIntervalService
              .fetchProlongIntervalInfo(offerId, product))
            .provideSomeLayer[Blocking with HasRequestContext](
              zio.ZEnv.live >>> TestEnvironment.live
            )
            .success
            .value
        res shouldBe None
      }
    }

    //seems redundant in absence of real db dao
    "return updated price even if deadline is not happened yet" in {
      val now = OffsetDateTime.parse(
        "2020-03-19T12:00:00+00:00",
        DateTimeFormatter.ISO_OFFSET_DATE_TIME
      )

      val jodaParser = ISODateTimeFormat.dateTimeParser()
      val jodaNow = jodaParser.parseDateTime("2020-03-19T12:00:00+00:00")
      val jodaDeadline = jodaNow.plusHours(1)
      val start = jodaParser.parseDateTime("2020-03-16T12:00:00+00:00")

      forAll(
        OfferIdentityGen,
        ProductGen,
        goodsGen(
          status = ProductStatuses.Inactive,
          prolongable = Prolongable(true),
          context = goodsContextGen(productPrice =
            productPriceGen(price =
              constPriceGen(prolongPrice = FundsGen.map(Some.apply))
            )
          )
        )
      ) { (offerId, paidProduct, good) =>
        val deadlinedGood = good.copy(deadline = jodaDeadline)

        (goodsDao.get _)
          .expects(ProlongIntervalIsNotEnded(offerId, paidProduct, start))
          .returningZ(Iterable(deadlinedGood))

        val res = (TestClock.setDateTime(now) *>
          prolongIntervalService
            .fetchProlongIntervalInfo(offerId, paidProduct))
          .provideSomeLayer[Blocking with HasRequestContext](
            zio.ZEnv.live >>> TestEnvironment.live
          )
          .success
          .value

        res.headOption shouldNot be(None)
      }
    }
  }

  "ProlongIntervalService.applyProlongInterval" should {

    "return None for non-Placement product" in {
      val product = ProductNotPlacementGen.next
      forAll(
        ProductInfoGen,
        productDurationGen,
        enrichedProduct(product),
        EnrichedPriceRequestContextGen
      ) {
        (
            matrixPrice,
            newDurationIfNoProlongInterval,
            enrichedProduct,
            context
        ) =>
          prolongIntervalService
            .applyProlongInterval(
              matrixPrice,
              newDurationIfNoProlongInterval,
              enrichedProduct,
              context
            )
            .success
            .value shouldBe None
      }
    }

    "return None if applyProlongInterval = false" in {
      forAll(
        ProductInfoGen,
        productDurationGen,
        EnrichedProductGen,
        enrichedPriceRequestContext(applyProlongInterval = false)
      ) {
        (
            matrixPrice,
            newDurationIfNoProlongInterval,
            enrichedProduct,
            context
        ) =>
          prolongIntervalService
            .applyProlongInterval(
              matrixPrice,
              newDurationIfNoProlongInterval,
              enrichedProduct,
              context
            )
            .success
            .value shouldBe None
      }
    }
  }

  "ProlongIntervalService.basePrice" should {

    "return matrix price if product duration is the same as it was on last product activation" in {
      forAll(
        prolongIntervalInfoGen(
          productPriceWithNonEmptyProlongPriceGen(duration = days(7))
        )
      ) { prolongIntervalInfo =>
        ProlongIntervalService.basePrice(
          prolongIntervalInfo,
          newDurationIfNoProlongInterval = days(7),
          matrixPrice = ProductInfo(
            Placement,
            Kopecks(66600),
            prolongPrice = Some(33000),
            duration = Some(days(7)),
            tariff = None,
            appliedExperiment = None,
            policyId = None
          )
        ) shouldBe Kopecks(66600)
      }
    }

    "return last activation price if product duration has changed since last product activation" in {
      forAll(
        prolongIntervalInfoGen(
          productPriceWithNonEmptyProlongPriceGen(
            duration = days(7),
            basePrice = 55500L
          )
        )
      ) { prolongIntervalInfo =>
        ProlongIntervalService.basePrice(
          prolongIntervalInfo,
          newDurationIfNoProlongInterval = days(60),
          matrixPrice = ProductInfo(
            Placement,
            Kopecks(66600),
            prolongPrice = Some(33000),
            duration = Some(days(7)),
            tariff = None,
            appliedExperiment = None,
            policyId = None
          )
        ) shouldBe Kopecks(55500)
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
