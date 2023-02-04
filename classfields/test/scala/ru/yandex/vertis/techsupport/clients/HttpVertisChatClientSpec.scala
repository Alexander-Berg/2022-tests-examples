package ru.yandex.vertis.vsquality.techsupport.clients

import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import org.scalatest.Ignore
import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
import ru.yandex.vertis.vsquality.techsupport.clients.impl.HttpVertisChatClient
import ru.yandex.vertis.vsquality.techsupport.model.client.RequestParams
import ru.yandex.vertis.vsquality.techsupport.model.{Domain, Tags}
import ru.yandex.vertis.vsquality.techsupport.model.external.jivosite
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import com.softwaremill.tagging._

/**
  * @author devreggs
  */
@Ignore
class HttpVertisChatClientSpec extends SpecBase {

  private val operator = jivosite.ChatUser(Some("Adnrew"), None, None, None, None, None, None, None)
  private val me = jivosite.ChatUser(Some("user:42911777"), None, None, None, None, None, None, None)
  private val message = jivosite.UserMessage("text", Some("You awesome! Spec."), None, None, None, None)
  private val request = jivosite.Request(sender = Some(operator), message = Some(message), recipient = Some(me))
  private val rp = generate[RequestParams]()

  private val vertisChatUrl = "http://chat-api-test-int.slb.vertis.yandex.net".taggedWith[Tags.Url]
  implicit private val backend: SttpBackend[F, _] = AsyncHttpClientCatsBackend[F]().await
  private val vertisChatClient = new HttpVertisChatClient(vertisChatUrl)

  "HttpVertisChatClient" should {
    "sends request" in {
      vertisChatClient.request(request, Domain.Autoru, rp).await shouldBe (())
    }
  }

}
