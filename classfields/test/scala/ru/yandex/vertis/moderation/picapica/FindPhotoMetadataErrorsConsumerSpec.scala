package ru.yandex.vertis.moderation.picapica

import akka.actor.ActorSystem
import cats.syntax.option._
import com.typesafe.config.ConfigFactory
import org.asynchttpclient.DefaultAsyncHttpClient
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Seconds, Span}
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.autoru
import ru.yandex.vertis.moderation.model.autoru.PhotoInfo.PicaPicaInfo
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{instanceGen, AutoruEssentialsGen}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.photos.PicaImagesMetadataFetcher
import ru.yandex.vertis.moderation.picapica.impl.{HttpPicaClient, PicaServiceImpl, SelfSufficientPicaRequestResolver}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.scheduler.task.contactindescription.PhonesFinder
import ru.yandex.vertis.moderation.scheduler.task.images.{
  DelegatePhotoMetadataDecider,
  LoggingPhotoMetadataDecider,
  PhotoDecider
}
import ru.yandex.vertis.moderation.stopwords.impl.SimpleStopwordsProvider
import ru.yandex.vertis.picapica.client.PicaPicaClient.Options
import ru.yandex.vertis.picapica.client._

import scala.concurrent.ExecutionContext

/**
  * Specs for pica pica scan
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class FindPhotoMetadataErrorsConsumerSpec extends SpecBase {

  private val DefaultPatienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(360, Seconds), interval = Span(1, Seconds))

  implicit override def patienceConfig: PatienceConfig = DefaultPatienceConfig

  "FindPhotoMetadataErrorsConsumer" should {

    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val system: ActorSystem = ActorSystem("test", ConfigFactory.empty())

    "asSource, return Ok verdict for custom photo" in {
      val options =
        Options(
          "http://realty3-pica-01-sas.test.vertis.yandex.net:35020",
          "autoru-all",
          PicaPicaClient.Mode.Async,
          Compression.GZipCompression
        )
      val client = new HttpPicaClient(options, new DefaultAsyncHttpClient())

      val picaService: PicaService = new PicaServiceImpl(client) with LoggingPicaService

      val resolver: PicaRequestResolver = SelfSufficientPicaRequestResolver
      val decider: PhotoDecider =
        new DelegatePhotoMetadataDecider(
          PhotoDecider.forService(
            Service.AUTORU,
            PhonesFinder.Empty,
            new SimpleStopwordsProvider(Set.empty),
            new SimpleStopwordsProvider(Set.empty)
          )
        ) with LoggingPhotoMetadataDecider

      val fetcher = new PicaImagesMetadataFetcher(picaService, resolver)

      val instance = {
        val i = instanceGen(Service.AUTORU).next
        val ess = AutoruEssentialsGen.next
        val photo =
          autoru.PhotoInfo(
            "2993-509a25883388583fdb154a8c99879f58",
            Some("M32391435DE61D0E7"),
            PicaPicaInfo(
              "a_15507083",
              "http://...",
              Some(4)
            ).some,
            deleted = Some(false),
            namespace = Some("some-namespace"),
            photoType = None,
            uploadTime = None,
            checkType = None
          )
        i.copy(essentials = ess.copy(photos = Seq(photo)))
      }

      val v =
        for {
          optSource <- fetcher.fetchMetadata(instance)
          verdict = optSource.map(decider)
        } yield verdict
      v.futureValue match {
        case Some(PhotoDecider.Verdict.Ok) => info("Done")
        case other                         => fail(s"Unexpected $other")
      }
    }
  }

}
