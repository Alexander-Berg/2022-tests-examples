package ru.yandex.auto.vin.decoder.partners.scrapinghub.gibdd

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.event.NoopPartnerEventManager
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class DefaultScrapingHubGibddClientIntTest extends AnyFunSuite {

  implicit val t = Traced.empty

  private val endpoint = HttpEndpoint(
    "yandex-scrapyrt.scrapinghub.com",
    443,
    "https"
  )

  private val remoteService = new RemoteHttpService(
    name = "scrapping-hub-gibdd",
    endpoint = endpoint
  )

  val client = new DefaultScrapingHubGibddClient(remoteService, new NoopPartnerEventManager)

  implicit val trigger = PartnerRequestTrigger.Unknown

  test("found registration") {
    pending
    val vin = VinCode("XWEPC811DD0043100")
    val reportFuture = client.getGibddReportByType(vin, ScrapingHubGibddReportType.Registration)
    val result = Await.result(reportFuture, 200.seconds)

    val data = result.result.get
    assert(data.periods.length == 2)
    assert(data.periods(0).rawOwnerType == "Natural")
    assert(data.periods(1).rawOwnerType == "Natural")
    // assert(data.vehicle.bodyNumber == "XWEPC811DD0043100")
    assert(data.vehicle.rawColor.contains("ОРАНЖЕВЫЙ"))
    assert(data.vehicle.rawYear.contains("2013"))
    assert(data.vehicle.category.contains("В"))
    assert(data.vehicle.rawPowerHp.contains("150"))
  }

  test("found constraints") {
    pending
    val vin = VinCode("XWEPC811DD0043100")
    val reportFuture = client.getGibddReportByType(vin, ScrapingHubGibddReportType.Constraints)
    val result = Await.result(reportFuture, 200.seconds)

    assert(result.result.get.records.length == 1)
  }

  test("found wanted") {
    pending
    val vin = VinCode("JMZBK12Z501750303")
    val reportFuture = client.getGibddReportByType(vin, ScrapingHubGibddReportType.Wanted)
    val result = Await.result(reportFuture, 200.seconds)

    assert(result.result.nonEmpty)
    assert(result.result.get.count == 1)
    assert(result.result.get.records.length == 1)
  }

  test("not found wanted") {
    pending
    val vin = VinCode("KNEJC524885850216")
    val reportFuture = client.getGibddReportByType(vin, ScrapingHubGibddReportType.Wanted)
    val result = Await.result(reportFuture, 200.seconds)

    assert(result.result.nonEmpty)
    assert(result.result.get.count == 0)
    assert(result.result.get.records.isEmpty)
  }

  test("found accidents") {
    pending
    val vin = VinCode("X7LBSRB1HEH699349")
    val reportFuture = client.getGibddReportByType(vin, ScrapingHubGibddReportType.Accidents)
    val result = Await.result(reportFuture, 200.seconds)

    assert(result.result.nonEmpty)
    assert(result.result.get.accidents.length == 4)
  }

  test("not found accidents") {
    pending
    val vin = VinCode("XTA111930B0148049")
    val reportFuture = client.getGibddReportByType(vin, ScrapingHubGibddReportType.Accidents)
    val result = Await.result(reportFuture, 200.seconds)

    assert(result.result.nonEmpty)
    assert(result.result.get.accidents.isEmpty)
  }
}
