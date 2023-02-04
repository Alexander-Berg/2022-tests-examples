package ru.yandex.vertis.moderation.scheduler.task.images

import org.asynchttpclient.DefaultAsyncHttpClient
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.DetailedReason
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.realty.PhotoInfo
import ru.yandex.vertis.moderation.model.realty.PhotoInfo.PicaPicaRequest
import ru.yandex.vertis.moderation.photos.PicaImagesMetadataFetcher
import ru.yandex.vertis.moderation.picapica.impl.{HttpPicaClient, PicaServiceImpl, SelfSufficientPicaRequestResolver}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.scheduler.task.images.PhotoDecider.VerdictAction
import ru.yandex.vertis.moderation.scheduler.task.images.impl.ScreenshotFinderImpl
import ru.yandex.vertis.picapica.client.PicaPicaClient.Options
import ru.yandex.vertis.picapica.client._

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
@RunWith(classOf[JUnitRunner])
class ManualScreenshotDeciderSpec extends SpecBase {

  private val options =
    Options(
      baseUrl = "http://realty3-pica-01-sas.test.vertis.yandex.net:35020",
      serviceName = "realty",
      mode = PicaPicaClient.Mode.Async,
      compression = Compression.GZipCompression
    )
  private val client = new HttpPicaClient(options, new DefaultAsyncHttpClient())

  private val fetcher = new PicaImagesMetadataFetcher(new PicaServiceImpl(client), SelfSufficientPicaRequestResolver)

  private val minimalConfidence = 0.5f
  private val screenshotDecider =
    new ScreenshotDecider(new ScreenshotFinderImpl(minimalConfidence), _ => VerdictAction.Warn)

  "ScreenshotDecider" should {
    "make screenshot decision" in {
      val photoInfo =
        PhotoInfo(
          url = "//avatars.mdst.yandex.net/get-realty/3220/add.1611834302485d1b2c2010c/",
          srcUrl = Some("//avatars.mdst.yandex.net/get-realty/3220/add.1611834302485d1b2c2010c/main"),
          cvHash = None,
          picaRequest = Some(PicaPicaRequest("i_3789074792227018496", 3)),
          deleted = None
        )

      val realtyEssentials =
        RealtyEssentialsGen.next
          .copy(photos = Seq(photoInfo))

      val instance =
        instanceGen(Service.REALTY).next
          .copy(essentials = realtyEssentials)

      val source = fetcher.fetchMetadata(instance).futureValue.get

      val screenshotWarns = screenshotDecider.apply(source).warns
      screenshotWarns should not be empty
      screenshotWarns.head.reason shouldBe DetailedReason.ScreenshotOnPhoto

    }
  }
}
