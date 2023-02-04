package ru.yandex.vertis.vsquality.techsupport.clients

import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import org.scalatest.Ignore
import ru.yandex.vertis.vsquality.utils.http_client_utils.config.HttpClientConfig
import ru.yandex.vertis.vsquality.techsupport.clients.JivositeClient.Channel
import ru.yandex.vertis.vsquality.techsupport.clients.impl.HttpJivositeClient
import ru.yandex.vertis.vsquality.techsupport.config.{JivositeChannelConfig, JivositeClientConfig}
import ru.yandex.vertis.vsquality.techsupport.model.external.jivosite
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._

/**
  * @author devreggs
  */
@Ignore
class HttpJivositeClientSpec extends SpecBase {

  private val me = jivosite.ChatUser(Some("user:42911777"), None, None, None, None, None, None, None)
  private val message = jivosite.UserMessage("text", Some("\uD83E\uDD16 You awesome! Spec."), None, None, None, None)
  private val request = jivosite.Request(sender = Some(me), message = Some(message), recipient = None)

  private val jivositeUrl = "https://wh.jivosite.com:443"

  private val config =
    JivositeClientConfig(
      HttpClientConfig(jivositeUrl),
      JivositeChannelConfig("IF6YK0nYgC56npYB", "oncz3I2jBH"),
      JivositeChannelConfig("IF6YK0nYgC56npYB", "WfLEup15TT"),
      JivositeChannelConfig("IF6YK0nYgC56npYB", "cYPZTSutrj"),
      JivositeChannelConfig("IF6YK0nYgC56npYB", "")
    )

  implicit private val backend: SttpBackend[F, _] = AsyncHttpClientCatsBackend[F]().await
  private val jivositeClient = new HttpJivositeClient(config)

  "HttpJivositeClient" should {
    "sends request" in {
      jivositeClient.request(request, Channel.AutoruPrivateUser).await should equal(())
    }
  }

}
