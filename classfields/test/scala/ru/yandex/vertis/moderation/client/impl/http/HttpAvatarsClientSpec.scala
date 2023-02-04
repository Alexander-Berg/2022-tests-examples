package ru.yandex.vertis.moderation.client.impl.http

import org.asynchttpclient.DefaultAsyncHttpClient
import org.junit.Ignore
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.MDSImageSize
import ru.yandex.vertis.moderation.service.impl.TvmTicketProvidersHolder
import ru.yandex.vertis.moderation.settings.HttpClientConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * To run it manually you need to install ticket_parser2_java library locally
  *
  * @see https://wiki.yandex-team.ru/passport/tvm2/library/
  * @author mpoplavkov
  */
@Ignore("For manually running")
class HttpAvatarsClientSpec extends SpecBase {

  private val writeUrl = "http://avatars-int.mdst.yandex.net:13000"
  private val readUrl = "https://avatars.mdst.yandex.net"
  private val httpClient = new DefaultAsyncHttpClient()
  private val client = new HttpAvatarsClient(writeUrl, readUrl, httpClient)
  private val namespace = "vs-support-pd"
  private val moderationClientId = 2016679
  private val avatarsClientId = 2002148
  private val secret = "paste secret here"

  private val tvmTicketProvidersHolder =
    new TvmTicketProvidersHolder(
      selfClientId = moderationClientId,
      secret = secret,
      destinationClientIds = Seq(avatarsClientId)
    )

  private val tvmTicketProvider = tvmTicketProvidersHolder.providersMap(avatarsClientId)

  private val tvmTicket = tvmTicketProvider.getTvmTicket.futureValue

  "HttpAvatarsClient" should {
    "upload an image by url" in {
      val existingImageUrl = "http://avatars.mdst.yandex.net/get-vertis-chat/3915/avatars_test/orig"

      val image =
        client
          .uploadImageByUrl(
            imageUrl = existingImageUrl,
            targetNamespace = namespace,
            imageName = None,
            ttl = Some(5.minutes),
            tvmTicket = tvmTicket
          )
          .futureValue

      image.name.nonEmpty shouldBe true
      val url = image.imagePath(readUrl, MDSImageSize.Orig)
      println(s"Uploaded image url:\n$url")

    }

    "create secure watermark" in {
      val groupId = 1398844
      val imageName = "2a00000171a750b4eff27c8da0fde5895228"
      val watermark =
        client
          .createSecureWatermark(
            namespace = namespace,
            groupId = groupId,
            imageName = imageName,
            imageSize = MDSImageSize.Square1200,
            watermarkText = "verysecure",
            ttl = Some(1.hour),
            tvmTicket = tvmTicket
          )
          .futureValue

      watermark.link.nonEmpty shouldBe true
      val url = s"$readUrl${watermark.link}"
      println(s"Created secure watermark:\n$url")
    }

    "get image metadata" in {
      val namespace = "o-yandex"
      val groupId = 1398656
      val imageName = "26825cd30564889e1c2183c6235a8ad9"
      val meta = client.getImageMetadata(namespace, groupId, imageName, tvmTicket).futureValue

      println(meta)
    }
  }

}
