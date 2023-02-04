package ru.auto.salesman.service

import org.scalatest.Inspectors
import ru.auto.salesman.Task
import ru.auto.salesman.controller.QuotaControllerImpl
import ru.auto.salesman.model._
import ru.auto.salesman.service.PriceEstimateService.PriceRequest
import ru.auto.salesman.service.PriceEstimateService.PriceRequest.QuotaContext
import ru.auto.salesman.service.QuotaPriceEstimateServiceSpec.PriceEstimator
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.util.{AutomatedContext, DateTimeInterval}
import ru.yandex.vertis.generators.ProducerProvider.asProducer
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.util.time.DateTimeUtil.now
import zio.ZIO
import zio.ZIO.{fail, succeed}

class QuotaPriceEstimateServiceSpec extends BaseSpec {

  private val regionId = RegionId(1L)

  private def interval(q: QuotaRequest) =
    QuotaControllerImpl.activateInterval(q)
  private def getDays(q: Quota) = DateTimeInterval(q.from, q.to).duration.toDays

  implicit val rc = AutomatedContext("test")

  "QuotaPriceEstimateService" should {

    "correctly count price and revenue for new activation" in {
      val request = QuotaRequestGen.next

      val priceAndRevenue = PriceEstimator
        .estimate(request, regionId, interval(request), None, None)
        .success
        .value
      priceAndRevenue.price shouldBe request.settings.size * 100
      priceAndRevenue.revenue shouldBe request.settings.size * 100
    }

    "correctly estimate price on prolongation of quota" in {
      Iterable(1, 7).foreach { days =>
        val request = quotaRequestGen(days).next
        val quota = quotaGen(days).next
          .copy(
            size = request.settings.size,
            price = request.settings.size * 100,
            from = request.from,
            to = request.to
          )

        val priceAndRevenue = PriceEstimator
          .estimate(request, regionId, interval(request), Some(quota), None)
          .success
          .value
        priceAndRevenue.price shouldBe (request.settings.size * 100)
        priceAndRevenue.revenue shouldBe (request.settings.size * 100)
      }
    }

    "for 1.day correctly estimate price and revenue on update (10 minutes)" in {
      val quota = quotaGen(1).next.copy(size = 100, price = 10000L)
      val request = {
        val r = quotaRequestGen(1).next
        r.copy(
          from = quota.from.plusMinutes(10),
          settings = r.settings.copy(size = quota.size + 100)
        )
      }

      val priceAndRevenue = PriceEstimator
        .estimate(request, regionId, interval(request), Some(quota), None)
        .success
        .value
      priceAndRevenue.price shouldBe (request.settings.size * 100)
      priceAndRevenue.revenue shouldBe 10500
    }

    "for 1.week correctly estimate price and revenue on update (10 minutes)" in {
      val quota = quotaGen(7).next.copy(size = 100, price = 10000L)
      val request = {
        val r = quotaRequestGen(7).next
        r.copy(
          from = quota.from.plusMinutes(430),
          settings = r.settings.copy(size = quota.size + 100)
        )
      }

      val priceAndRevenue = PriceEstimator
        .estimate(request, regionId, interval(request), Some(quota), None)
        .success
        .value
      priceAndRevenue.price shouldBe (request.settings.size * 100)
      priceAndRevenue.revenue shouldBe 10500
    }

    "correctly estimate price and revenue on update (less 1 second)" in {
      Inspectors.forEvery(Iterable(1, 7)) { days =>
        val quota = quotaGen(days).next.copy(size = 100, price = 10000L)
        val request = {
          val r = quotaRequestGen(days).next
          r.copy(
            from = quota.from.plusMillis(500),
            settings = r.settings.copy(size = quota.size + 100)
          )
        }

        val priceAndRevenue = PriceEstimator
          .estimate(request, regionId, interval(request), Some(quota), None)
          .success
          .value
        priceAndRevenue.price shouldBe (request.settings.size * 100)
        priceAndRevenue.revenue shouldBe 10000
      }
    }

    "for 1.day quota correctly estimate price and revenue on update (1 hour)" in {
      val quota = quotaGen(1).next.copy(size = 100, price = 10000L)
      val request = {
        val r = quotaRequestGen(1).next
        r.copy(
          from = quota.from.plusHours(1),
          settings = r.settings.copy(size = quota.size + 100, days = getDays(quota))
        )
      }

      val priceAndRevenue = PriceEstimator
        .estimate(request, regionId, interval(request), Some(quota), None)
        .success
        .value
      priceAndRevenue.price shouldBe (request.settings.size * 100)
      priceAndRevenue.revenue shouldBe 10500
    }

    "for 1.week quota correctly estimate price and revenue on update (1 hour)" in {
      val quota = quotaGen(7).next.copy(size = 100, price = 10000L)
      val request = {
        val r = quotaRequestGen(7).next
        r.copy(
          from = quota.from.plusHours(7),
          settings = r.settings.copy(size = quota.size + 100)
        )
      }

      val priceAndRevenue = PriceEstimator
        .estimate(request, regionId, interval(request), Some(quota), None)
        .success
        .value
      priceAndRevenue.price shouldBe (request.settings.size * 100)
      priceAndRevenue.revenue shouldBe 10500
    }

    "for 1.day quota correctly estimate price and revenue on update (20 hour)" in {
      val quota = quotaGen(1).next.copy(size = 100, price = 10000L)
      val request = {
        val r = quotaRequestGen(1).next
        r.copy(
          from = quota.from.plusHours(20),
          settings = r.settings.copy(size = quota.size + 100)
        )
      }

      val priceAndRevenue = PriceEstimator
        .estimate(request, regionId, interval(request), Some(quota), None)
        .success
        .value
      priceAndRevenue.price shouldBe (request.settings.size * 100)
      priceAndRevenue.revenue shouldBe 18400
    }

    "for 7.day quota correctly estimate price and revenue on update (20 hour)" in {
      val quota = quotaGen(7).next.copy(size = 100, price = 10000L)
      val request = {
        val r = quotaRequestGen(7).next
        r.copy(
          from = quota.from.plusHours(140),
          settings = r.settings.copy(size = quota.size + 100)
        )
      }

      val priceAndRevenue = PriceEstimator
        .estimate(request, regionId, interval(request), Some(quota), None)
        .success
        .value
      priceAndRevenue.price shouldBe (request.settings.size * 100)
      priceAndRevenue.revenue shouldBe 18400
    }

    "for 1.day quota correctly estimate price and revenue on update decrease size (10 minutes)" in {
      val quota = quotaGen(1).next.copy(size = 200, price = 20000L)
      val request = {
        val r = quotaRequestGen(1).next
        r.copy(
          from = quota.from.plusMinutes(10),
          settings = r.settings.copy(size = quota.size - 100)
        )
      }

      val priceAndRevenue = PriceEstimator
        .estimate(request, regionId, interval(request), Some(quota), None)
        .success
        .value
      priceAndRevenue.price shouldBe (request.settings.size * 100)
      priceAndRevenue.revenue shouldBe 0
    }

    "for 1.week quota correctly estimate price and revenue on update decrease size (10 minutes)" in {
      val quota = quotaGen(7).next.copy(size = 200, price = 20000L)
      val request = {
        val r = quotaRequestGen(7).next
        r.copy(
          from = quota.from.plusMinutes(70),
          settings = r.settings.copy(size = quota.size - 100)
        )
      }

      val priceAndRevenue = PriceEstimator
        .estimate(request, regionId, interval(request), Some(quota), None)
        .success
        .value
      priceAndRevenue.price shouldBe (request.settings.size * 100)
      priceAndRevenue.revenue shouldBe 0
    }

    "for 1.day quota correctly estimate price and revenue on update decrease size (22 hours)" in {
      val quota = quotaGen(1).next.copy(size = 200, price = 20000L)
      val request = {
        val r = quotaRequestGen(1).next
        r.copy(
          from = quota.from.plusHours(22),
          settings = r.settings.copy(size = quota.size - 100)
        )
      }

      val priceAndRevenue = PriceEstimator
        .estimate(request, regionId, interval(request), Some(quota), None)
        .success
        .value
      priceAndRevenue.price shouldBe (request.settings.size * 100)
      priceAndRevenue.revenue shouldBe 8400
    }

    "for 7.day quota correctly estimate price and revenue on update decrease size (22 hours)" in {
      val quota = quotaGen(7).next.copy(size = 200, price = 20000L)
      val request = {
        val r = quotaRequestGen(1).next
        r.copy(
          from = quota.from.plusHours(154),
          settings = r.settings.copy(size = quota.size - 100)
        )
      }

      val priceAndRevenue = PriceEstimator
        .estimate(request, regionId, interval(request), Some(quota), None)
        .success
        .value
      priceAndRevenue.price shouldBe (request.settings.size * 100)
      priceAndRevenue.revenue shouldBe 8400
    }

    "correctly estimate price and revenue on quota skipped for 135 min" in {
      val settings = QuotaRequest.Settings(500, 1, None)
      val request = QuotaRequestGen.next
        .copy(from = now().minusMinutes(135), settings = settings)
      val quota = QuotaGen.next
        .copy(
          size = request.settings.size,
          price = request.settings.size * 100,
          from = request.from.minusDays(2),
          to = request.from.minusDays(1)
        )

      val priceAndRevenue = PriceEstimator
        .estimate(request, regionId, interval(request), Some(quota), None)
        .success
        .value
      priceAndRevenue.price shouldBe (request.settings.size * 100)
      priceAndRevenue.revenue shouldBe priceAndRevenue.price
    }

    "correctly estimate price and revenue on quota skipped for more than 4 h" in {
      val settings = QuotaRequest.Settings(500, 1, None)
      val request = QuotaRequestGen.next
        .copy(from = now().minusMinutes(250), settings = settings)
      val quota = QuotaGen.next
        .copy(
          size = request.settings.size,
          price = request.settings.size * 100,
          from = request.from.minusDays(2),
          to = request.from.minusDays(1)
        )

      val priceAndRevenue = PriceEstimator
        .estimate(request, regionId, interval(request), Some(quota), None)
        .success
        .value
      val expectedDiscount = (priceAndRevenue.price * 4 / 24) / 100 * 100
      priceAndRevenue.price shouldBe (request.settings.size * 100)
      priceAndRevenue.revenue shouldBe priceAndRevenue.price - expectedDiscount
    }

    "correctly estimate price and revenue on quota skipped for more than 20 h" in {
      val settings = QuotaRequest.Settings(500, 1, None)
      val request = QuotaRequestGen.next
        .copy(from = now().minusMinutes(1220), settings = settings)
      val quota = QuotaGen.next
        .copy(
          size = request.settings.size,
          price = request.settings.size * 100,
          from = request.from.minusDays(2),
          to = request.from.minusDays(1)
        )

      val priceAndRevenue = PriceEstimator
        .estimate(request, regionId, interval(request), Some(quota), None)
        .success
        .value
      val expectedDiscount = (priceAndRevenue.price * 20 / 24) / 100 * 100
      priceAndRevenue.price shouldBe (request.settings.size * 100)
      priceAndRevenue.revenue shouldBe priceAndRevenue.price - expectedDiscount
    }

  }
}

object QuotaPriceEstimateServiceSpec extends MockitoSupport {

  private val EstimateService = mock[PriceEstimateService]

  private val PriceRequestCreator = {
    val m = mock[PriceRequestCreator]
    stub(
      m.forQuota(_: QuotaRequest, _: Option[TariffType], _: DateTimeInterval)
    ) {
      case (
            QuotaRequest(
              _,
              quotaType,
              QuotaRequest.Settings(size, _, _, _),
              _,
              _
            ),
            tariff,
            interval
          ) =>
        ZIO.succeed {
          PriceRequest(
            offer = None,
            PriceRequest
              .QuotaContext(RegionId(1L), clientMarks = Nil, size, tariff),
            quotaType,
            interval,
            priceRequestId = None
          )
        }
    }
    m
  }

  object PriceEstimator
      extends QuotaPriceEstimateService(EstimateService, PriceRequestCreator) {

    override protected def estimatePrice(
        priceRequest: PriceRequest
    ): Task[Funds] =
      priceRequest.context match {
        case QuotaContext(_, _, size, _) =>
          succeed(size * 100)
        case _ =>
          fail(throw new IllegalArgumentException("Unexpected context type"))
      }
  }
}
