package ru.yandex.vertis.billing.service

import com.google.protobuf.Message
import org.apache.http.Header
import org.apache.http.entity.ContentType
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.realty.abram.proto.api.call.prices.{CallPrice, CallPriceResponse}
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.http.SyncHttpClient
import ru.yandex.vertis.billing.model_core.gens.{CampaignHeaderGen, MetrikaCallFactGen, Producer}
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.concurrent.atomic.AtomicLong
import scala.util.{Failure, Success}

/**
  * @author brs-lphv
  */
class RealtyCommercialCallPriceServiceSpec extends AnyWordSpec with Matchers with AsyncSpecBase with MockitoSupport {

  val callFact = MetrikaCallFactGen.next

  val campaignHeader = CampaignHeaderGen.next

  "RealtyCommercialCallPriceService" should {
    "retry on http failure" in {
      val price = 9999
      val client = mockHttpClient(price, failCount = 2)
      val service = new RealtyCommercialCallPriceService("some/url", client)
      service.getPrice(callFact, campaignHeader) shouldBe Success(price)
    }
  }

  private def mockHttpClient(price: Long, failCount: Int) = {
    val client = mock[SyncHttpClient]
    val counter = new AtomicLong()
    val response = CallPriceResponse.newBuilder().setPrice(CallPrice.newBuilder().setBasePrice(price).build()).build()
    stub(client.post(_: String, _: Option[Message], _: ContentType, _: Iterable[Header])(_: Array[Byte] => Message)) {
      case (_, _, _, _, _) =>
        if (counter.incrementAndGet <= failCount) Failure(new Exception("some error"))
        else Success(response)
    }
    client
  }
}
