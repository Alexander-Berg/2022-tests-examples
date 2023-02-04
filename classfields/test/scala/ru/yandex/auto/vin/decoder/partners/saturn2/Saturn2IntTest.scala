package ru.yandex.auto.vin.decoder.partners.saturn2

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.clients.saturn2.DefaultSaturn2Client
import ru.yandex.auto.vin.decoder.clients.saturn2.Saturn2Client.Services
import ru.yandex.auto.vin.decoder.partners.event.{NoopPartnerEventManager, PartnerEventManager}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

@Ignore
class Saturn2IntTest extends AnyFunSuite {

  implicit val t = Traced.empty
  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown

  val remoteService = new RemoteHttpService(
    "saturn2",
    new HttpEndpoint("81.90.214.206", 8081, "http")
  )

  val partnerEventClient: PartnerEventManager = new NoopPartnerEventManager
  val client = new DefaultSaturn2Client(remoteService)

  test("get for year") {
    val twoDaysBefore = Instant.now().minus(100, ChronoUnit.DAYS)
    val res = Await.result(client.downloadByType(twoDaysBefore.toEpochMilli, Services), 10.minutes)
    println(res.getAbsolutePath)
  }

  test("get for 2 days") {
    val twoDaysBefore = Instant.now().minus(2, ChronoUnit.DAYS)
    val res = Await.result(client.downloadByType(twoDaysBefore.toEpochMilli, Services), 10.minutes)
    println(res.getAbsolutePath)
  }
}
