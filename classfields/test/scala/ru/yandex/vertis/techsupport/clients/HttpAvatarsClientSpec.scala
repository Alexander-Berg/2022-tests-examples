package ru.yandex.vertis.vsquality.techsupport.clients

import com.softwaremill.tagging._
import org.scalatest.Ignore
import ru.yandex.vertis.vsquality.techsupport.clients.impl.HttpAvatarsClient
import ru.yandex.vertis.vsquality.techsupport.model.Tags
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.SttpBackend
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.techsupport.clients.AvatarsClient.{MDSImageSize, UploadRequest, WatermarkRequest}
import ru.yandex.vertis.vsquality.techsupport.util.tvm.TvmTicketProvidersHolder

import scala.concurrent.duration._

/**
  * To run it manually you need to install tvmauth java library locally
  *
  * @see https://wiki.yandex-team.ru/passport/tvm2/library/
  */

@Ignore
class HttpAvatarsClientSpec extends SpecBase {
  implicit private val backend: SttpBackend[F, _] = AsyncHttpClientCatsBackend[F]().await

  private val writeUrl = "http://avatars-int.mdst.yandex.net:13000".taggedWith[Tags.Url]
  private val readUrl = "https://avatars.mdst.yandex.net".taggedWith[Tags.Url]
  private val client = new HttpAvatarsClient(writeUrl, readUrl)
  private val namespace = "vs-support-pd"
  private val moderationClientId = 2016679
  private val avatarsClientId = 2002148
  private val secret = "paste secret here"

  private lazy val tvmTicketProvidersHolder = new TvmTicketProvidersHolder(
    selfClientId = moderationClientId,
    secret = secret,
    destinationClientIds = Seq(avatarsClientId)
  )

  private lazy val tvmTicketProvider = tvmTicketProvidersHolder.providersMapResource.map(_.apply(avatarsClientId))

  private lazy val tvmTicket = tvmTicketProvider.use(_.getTvmTicket).await

  "HttpAvatarsClient" should {
    "upload an image by url" in {
      val existingImageUrl = "http://avatars.mdst.yandex.net/get-vertis-chat/3915/avatars_test/orig"

      val uploadRequest =
        UploadRequest(
          imageUrl = existingImageUrl,
          targetNamespace = namespace,
          imageName = None,
          ttl = Some(5.minutes)
        )
      val image = client.uploadImageByUrl(uploadRequest, tvmTicket).await

      image.name.nonEmpty shouldBe true
      val url = image.imagePath(readUrl, MDSImageSize.Orig)
      println(s"Uploaded image url:\n$url")

    }

    "create secure watermark" in {
      val groupId = 1398844
      val imageName = "2a00000171a750b4eff27c8da0fde5895228"
      val watermarkRequest =
        WatermarkRequest(
          namespace = namespace,
          groupId = groupId,
          imageName = imageName,
          imageSize = MDSImageSize.Square1200,
          watermarkText = "verysecure",
          ttl = Some(1.hour)
        )
      val watermark = client.createSecureWatermark(watermarkRequest, tvmTicket).await

      watermark.path.nonEmpty shouldBe true
      val url = s"$readUrl${watermark.path}"
      println(s"Created secure watermark:\n$url")
    }
  }

}
