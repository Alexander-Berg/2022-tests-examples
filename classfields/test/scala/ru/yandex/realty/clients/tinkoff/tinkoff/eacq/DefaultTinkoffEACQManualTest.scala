package ru.yandex.realty.clients.tinkoff.tinkoff.eacq

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.clients.tinkoff.eacq.init.InitRequest
import ru.yandex.realty.clients.tinkoff.eacq.{DefaultTinkoffEACQClient, TinkoffEACQCredentials}
import ru.yandex.realty.http.{ApacheHttpClient, HttpEndpoint, LoggedHttpClient, RemoteHttpService}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.{AsyncSpecBase, SpecBase}

class DefaultTinkoffEACQManualTest extends SpecBase with AsyncSpecBase with PropertyChecks {

  val realAsyncClient = {
    val client = HttpAsyncClientBuilder
      .create()
      .setMaxConnPerRoute(1024)
      .setMaxConnTotal(40 * 1024)
      .build()
    client.start()
    sys.addShutdownHook(client.close())
    client
  }

  val remoteHttpService = new RemoteHttpService(
    "tinkoff",
    HttpEndpoint("rest-api-test.tinkoff.ru", 443, "https"),
    new ApacheHttpClient(realAsyncClient) with LoggedHttpClient
  )

  val realClient =
    new DefaultTinkoffEACQClient(remoteHttpService, new TinkoffEACQCredentials("1612433180567", "w8gyn3t926tmu40j"))

  "DefaultTinkoffEACQClient" should {
    "init transaction successfully" in {
      val req = InitRequest(
        Amount = "140000",
        OrderId = "21051",
        IP = None,
        Description = "Подарочная карта на 1000 рублей",
        Currency = None,
        CustomerKey = None,
        Recurrent = None,
        PayType = None,
        Language = None,
        DATA = Map(
          "Email" -> "a@test.com",
          "Phone" -> "+71234567890"
        ),
        Receipt = None,
        NotificationURL = None,
        SuccessURL = None,
        FailURL = None,
        RedirectDueDate = None
      )
      implicit val trace: Traced = Traced.empty
      val resp = realClient.init(req).futureValue
      resp.Success shouldEqual (true)
      resp.Status shouldEqual (Some("NEW"))
    }
  }
}
