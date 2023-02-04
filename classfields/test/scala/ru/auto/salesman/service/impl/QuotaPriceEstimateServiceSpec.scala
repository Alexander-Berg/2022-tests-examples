package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import org.scalatest.Inspectors
import ru.auto.salesman.controller.QuotaControllerImpl
import ru.auto.salesman.model.{
  Funds,
  ProductId,
  Quota,
  QuotaRequest,
  RegionId,
  TariffType
}
import ru.auto.salesman.service.PriceEstimateService.PriceRequest.QuotaContext
import ru.auto.salesman.service.PriceEstimateService.{PriceRequest, PriceResponse}
import ru.auto.salesman.service.PriceExtractor.ProductInfo
import ru.auto.salesman.service.impl.QuotaPriceEstimateServiceSpec._
import ru.auto.salesman.service.{
  PriceEstimateService,
  PriceExtractor,
  PriceRequestCreator,
  QuotaPriceEstimateService
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.util.money.Money.Kopecks
import ru.auto.salesman.util.{AutomatedContext, DateTimeInterval}
import ru.yandex.vertis.generators.ProducerProvider.asProducer
import zio.ZIO.succeed
import zio.{Task, ZIO}

import scala.util.Random

class QuotaPriceEstimateServiceSpec extends BaseSpec {

  val service = new QuotaPriceEstimateService(
    EstimateServiceMock,
    PriceRequestCreatorMock,
    Extractor
  )
  val region = RegionId(100L)
  implicit val rc = AutomatedContext("test")

  private def interval(q: QuotaRequest) =
    QuotaControllerImpl.activateInterval(q)

  "QuotaPriceEstimateService" should {

    "correctly set price for new quota" in {
      val requests = quotaRequestGen(1).next(100)
      Inspectors.forEvery(requests) { r =>
        val pr = service
          .estimate(r, region, interval(r), None, None)
          .success
          .value
        pr.price shouldBe r.settings.size * 100
        pr.revenue shouldBe r.settings.size * 100
      }
    }

    "correctly handle prolongation" in {
      val requests = quotaRequestGen(1).next(100).toList
      Inspectors.forEvery(requests) { request =>
        val days = Random.nextInt(10) + 1
        val quota = quotaGen(1).next
          .copy(
            clientId = request.clientId,
            size = request.settings.size,
            from = request.from.plusDays(days),
            to = request.from.plusDays(days + 1)
          )
        val pr = service
          .estimate(request, region, interval(request), Some(quota), None)
          .success
          .value
        pr.price shouldBe request.settings.size * 100
        pr.revenue shouldBe request.settings.size * 100
      }
    }

    "correctly handle quota update" in {
      val requests = quotaRequestGen(1).next(100).toList
      Inspectors.forEvery(requests) { request =>
        val minutes = Random.nextInt(50 * 10) + 30
        val quota = quotaGen(1).next
          .copy(
            clientId = request.clientId,
            size = request.settings.size / 2,
            from = request.from.minusMinutes(minutes),
            to = request.from.minusMinutes(minutes).plusDays(1),
            price = request.settings.size * 100 / 2
          )
        val pr = service
          .estimate(request, region, interval(request), Some(quota), None)
          .success
          .value
        pr.revenue < pr.price shouldBe true
        pr.price shouldBe request.settings.size * 100
      }
    }

    "handler quota update" in {
      val quota = {
        val q = quotaGen(1).next
        q.copy(size = 100, price = 100 * 100, revenue = 100 * 100)
      }
      val req = updateRequest(Some(100), Some(100))(quota)
      val pr = service
        .estimate(req, region, interval(req), Some(quota), None)
        .success
        .value
      pr.price shouldBe 20000
      pr.revenue shouldBe 10900
      val req1 = updateRequest(Some(500), Some(100))(quota)
      val pr1 = service
        .estimate(req1, region, interval(req1), Some(quota), None)
        .success
        .value
      pr1.price shouldBe 20000
      pr1.revenue shouldBe 13800
      val req2 = updateRequest(Some(500), Some(-20))(quota)
      val pr2 = service
        .estimate(req2, region, interval(req2), Some(quota), None)
        .success
        .value
      pr2.price shouldBe 8000
      pr2.revenue shouldBe 1800
      val req3 = updateRequest(Some(1400), Some(-20))(quota)
      val pr3 = service
        .estimate(req3, region, interval(req3), Some(quota), None)
        .success
        .value
      pr3.price shouldBe 8000
      pr3.revenue shouldBe 8000
      val req4 = updateRequest(Some(50), Some(-20))(quota)
      val pr4 = service
        .estimate(req4, region, interval(req4), Some(quota), None)
        .success
        .value
      pr4.price shouldBe 8000
      pr4.revenue shouldBe 0
    }

    "handle premature activation" in {
      val quota = {
        val q = quotaGen(1).next
        q.copy(size = 100, price = 100 * 100, revenue = 100 * 100)
      }
      val req = updateRequest(Some(-10), Some(100))(quota)
      val pr = service
        .estimate(req, region, interval(req), Some(quota), None)
        .success
        .value
      pr.price shouldBe 20000
      pr.revenue shouldBe 10000
      val req2 = updateRequest(Some(-10), Some(-50))(quota)
      val pr2 = service
        .estimate(req2, region, interval(req2), Some(quota), None)
        .success
        .value
      pr2.price shouldBe 5000
      pr2.revenue shouldBe 0
    }
  }

}

object QuotaPriceEstimateServiceSpec {

  import ru.yandex.vertis.mockito.MockitoSupport.{mock, stub}

  private def updateRequest(minutes: Option[Int], size: Option[Int])(
      quota: Quota
  ): QuotaRequest = {
    val request =
      QuotaRequest(
        quota.clientId,
        quota.quotaType,
        QuotaRequest.Settings(quota.size, 1, None),
        quota.from
      )
    val r = minutes
      .map(m => request.copy(from = request.from.plusMinutes(m)))
      .getOrElse(request)
    val settings: QuotaRequest.Settings =
      size
        .map(s => r.settings.copy(size = request.settings.size + s))
        .getOrElse(r.settings)
    r.copy(settings = settings)
  }

  private val EstimateServiceMock = {
    val m = mock[PriceEstimateService]
    stub(m.estimate(_: PriceEstimateService.PriceRequest)) {
      case PriceRequest(_, QuotaContext(_, _, size, _), _, _, _) =>
        succeed(
          new PriceResponse((size * 100).toString.getBytes, DateTime.now())
        )
    }
    m
  }

  private val PriceRequestCreatorMock = {
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

  case class Extractor(r: PriceResponse) extends PriceExtractor {

    def price(product: ProductId, date: DateTime): Task[Funds] =
      ZIO.succeed(new String(r.response).toInt)

    def productInfo(product: ProductId, date: DateTime): Task[ProductInfo] =
      ZIO.succeed(
        ProductInfo(
          product,
          Kopecks(new String(r.response).toLong),
          prolongPrice = None,
          duration = None,
          tariff = None,
          appliedExperiment = None,
          policyId = None
        )
      )

  }

}
