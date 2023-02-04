package ru.yandex.vertis.vsquality.techsupport.clients

import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import org.scalatest.Ignore
import com.softwaremill.tagging._
import ru.auto.api.response_model.OfferListingResponse
import ru.yandex.vertis.vsquality.techsupport.clients.impl.HttpVosAutoruClient
import ru.yandex.vertis.vsquality.techsupport.model.{ClientOffer, Tags, Url, UserId}
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import ru.yandex.vertis.vsquality.utils.lang_utils.Use
import ru.yandex.vertis.vsquality.techsupport.util.DateTimeUtils._
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.lang_utils.interval.Interval

import scala.concurrent.duration._

/**
  * @author devreggs
  */
@Ignore
class HttpVosAutoruClientSpec extends SpecBase {

  private val baseVosUrl: Url =
    "http://vos2-autoru-api.vrts-slb.test.vertis.yandex.net/api/v1".taggedWith[Tags.Url]
  implicit private val backend: SttpBackend[F, _] = AsyncHttpClientCatsBackend[F]().await
  private val client: VosAutoruClient[F] = new HttpVosAutoruClient(baseVosUrl)
  private val user = UserId.Client.Autoru.PrivatePerson(47549791L.taggedWith[Tags.AutoruPrivatePersonId])

  "VosAutoruClient.offers" should {
    "get blocked offers" in {
      val params = VosAutoruClient.OffersFilter(
        Interval(now().minusSeconds(1000.days.toSeconds), now()),
        1,
        Use(ClientOffer.Status.Banned)
      )
      client.offers(user, params).await shouldBe a[OfferListingResponse]
    }
  }
}
