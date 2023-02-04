package ru.yandex.vertis.tvm

import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.util.DateTimeUtils
import ru.yandex.vertis.tvm.impl.RequestSignerImpl

/**
  * @author alex-kovalenko
  */
trait TvmClientIntegrationTest extends TvmSpecBase with AsyncSpecBase {

  def client: TvmClient

  lazy val signer = new RequestSignerImpl(SelfClientId, ClientSecret)

  "TvmClient" should {
    "get ticket" in {
      val time = DateTimeUtils.now()
      val sign = signer.sign(time, List(1)).get
      val request = TvmClient.TicketRequest(SelfClientId, List(1), time, sign)
      val response = client.getTickets(request).futureValue
      info(s"Response: $response")
    }
  }
}
