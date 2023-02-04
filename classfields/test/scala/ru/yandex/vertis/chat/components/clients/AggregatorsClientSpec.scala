package ru.yandex.vertis.chat.components.clients

import ru.yandex.vertis.chat.{CacheControl, RobotRequestContext}
import ru.yandex.vertis.chat.common.aggregators.AggregatorsSupport
import ru.yandex.vertis.chat.components.ComponentsSpecBase
import ru.yandex.vertis.chat.components.clients.aggregators.{AggregatorClient, DefaultAggregatorClient}
import ru.yandex.vertis.chat.components.dao.aggregators.Aggregator
import ru.yandex.vertis.chat.model.ModelGenerators.userId
import ru.yandex.vertis.chat.util.http.MockedHttpClientSupport
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.mockito.MockitoSupport

class AggregatorsClientSpec
  extends ComponentsSpecBase
  with MockitoSupport
  with ProducerProvider
  with MockedHttpClientSupport {

  implicit private val rc = RobotRequestContext.random(userId.next, "test", CacheControl.Disallow)

  private val service: AggregatorClient = new DefaultAggregatorClient(http, null, null);

  "DefaultAggregatorClient" should {

    "Send requests to aggregator" in {
      val aggregator = Aggregator("", "http://hook.com?token=tokeeeen", "tokeeeen", "")
      val request: AggregatorsSupport.Request = AggregatorsSupport.Request(
        Some(AggregatorsSupport.ChatUser(id = Some("123"), name = Some("123"))),
        Some(AggregatorsSupport.UserMessage(text = Some("aaa")))
      )
      http.respondWithStatus(200)
      service.send(request, aggregator).futureValue
    }
  }

}
