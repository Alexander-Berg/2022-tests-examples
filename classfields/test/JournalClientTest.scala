package auto.carfax.common.clients.journal.test

import org.scalatest.Ignore
import org.scalatest.wordspec.AnyWordSpec
import auto.carfax.common.clients.journal.JournalClient
import auto.carfax.common.utils.tracing.Traced
import ru.yandex.auto.vin.decoder.tvm.{DefaultTvmConfig, DefaultTvmTicketsProvider, TvmConfig, TvmTicketsProvider}
import auto.carfax.common.utils.config.Environment.config
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.Await
import scala.concurrent.duration._

@Ignore
class JournalClientTest extends AnyWordSpec {

  implicit val t: Traced = Traced.empty

  val remoteService = new RemoteHttpService(
    "journal",
    new HttpEndpoint("journal-api-autoru-http.vrts-slb.test.vertis.yandex.net", 80, "http")
  )
  lazy val tvmConfig: TvmConfig = DefaultTvmConfig(config.getConfig("auto-vin-decoder.tvm"))
  lazy val tvmTicketsProvider: TvmTicketsProvider = DefaultTvmTicketsProvider(tvmConfig)

  lazy val client = new JournalClient(tvmTicketsProvider, remoteService)

  "ret" in {
    val eventualArticle = client.findPost("vo-skolko-obhoditsya-soderzhanie-volkswagen-touareg-dnevnik-trat")
    val article = Await.result(eventualArticle, 20.seconds)
    article
  }
}
