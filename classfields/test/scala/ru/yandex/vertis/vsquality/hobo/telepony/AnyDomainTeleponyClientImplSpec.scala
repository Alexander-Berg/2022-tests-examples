package ru.yandex.vertis.vsquality.hobo.telepony

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest.Ignore
import ru.yandex.vertis.vsquality.hobo.concurrent.Threads
import ru.yandex.vertis.vsquality.hobo.model.TeleponyDomain
import ru.yandex.vertis.vsquality.hobo.telepony.model.{CreateBlacklistRequest, Reasons}
import ru.yandex.vertis.vsquality.hobo.util.SpecBase
import ru.yandex.vertis.vsquality.hobo.telepony.protocol.model.Source

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

@Ignore
class AnyDomainTeleponyClientImplSpec extends SpecBase {

  private val url = "http://telepony-api-int.vrts-slb.test.vertis.yandex.net"

  implicit private val ec: ExecutionContext = Threads.SameThreadEc
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer = Materializer.matFromSystem

  private val client = new AnyDomainTeleponyClientImpl(TeleponyClientConfig(url, TeleponyDomain.AutoruDef.name))

  "AnyDomainTeleponyClientImpl" should {
    "gets call info by call id" in {
      val callId = "RCzznqX87BU"
      val calls = client.getCallInfo(TeleponyDomain.AutoruDef, callId).futureValue
      println(calls)
    }

    "gets blacklist info by source" in {
      val source1 = Source("+79062490628") // blocked recently, enriched response
      val info1 = client.getBlacklistInfo(source1).futureValue
      println(info1)

      val source2 = Source("+74951503862") // blocked ages ago, reduced response
      val info2 = client.getBlacklistInfo(source2).futureValue
      println(info2)
    }

    "adds source to blacklist" in {
      val request =
        CreateBlacklistRequest(
          "+74950000000",
          Reasons.Telemarketing,
          1.hour,
          Some("test")
        )
      client.addToBlacklist(request).futureValue
    }

    "removes source from blacklist" in {
      val source = Source("+74950000000")
      client.removeFromBlacklist(source).futureValue
    }
  }
}
