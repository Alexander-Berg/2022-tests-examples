package ru.yandex.realty2.extdataloader.loaders.sites

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.util
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsValue}
import ru.yandex.extdata.core.Data.WriterData
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.extdata.core.{ProduceResult, ServerController}
import ru.yandex.realty.clients.BnbSearcherClient
import ru.yandex.realty.context.SiteCTRStorage
import ru.yandex.realty.context.v2.AuctionResultStorage
import ru.yandex.realty.model.auction.AuctionResult
import ru.yandex.realty.model.sites.{ExtendedSiteStatisticsAtom, SimpleSiteStatisticsResult, Site}
import ru.yandex.realty.proto.storage.Storage
import ru.yandex.realty.sites.SitesGroupingService
import ru.yandex.realty.storage.SimilarSitesStorage
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.generators.BasicGenerators
import ru.yandex.realty.util.Mappings._

import scala.collection.JavaConverters._
import scala.util.Try

@RunWith(classOf[JUnitRunner])
class SimilarSitesFetcherSpec extends WordSpec with Matchers with BasicGenerators with MockFactory {

  private val MapSize = 1000
  implicit private val traced: Traced = Traced.empty

  private lazy val SimilarSites: Map[Long, Seq[Long]] = {
    val rowGen = for {
      key <- Gen.posNum[Long]
      values <- listNUnique(10, posNum[Long])(identity)
    } yield (key, values)

    Gen.mapOfN(MapSize, rowGen).next
  }

  private def auctionStorage: AuctionResultStorage = {
    val sites = SimilarSites.keySet.map { id =>
      val stat = mock[SimpleSiteStatisticsResult].applySideEffect { stat =>
        (stat.getTrustedOffers _).expects().anyNumberOfTimes().returning(1)
      }
      val atom = new ExtendedSiteStatisticsAtom(stat, new util.HashMap())

      AuctionResult(
        id,
        IndexedSeq.empty,
        None,
        atom,
        pikStats = Some(atom),
        filter = None,
        topOfferBid = 0
      )
    }.toSeq

    new AuctionResultStorage(sites)
  }

  private val ctrStorage = mock[SiteCTRStorage]
  (ctrStorage
    .get(_: Long))
    .expects(*)
    .anyNumberOfTimes()
    .returning(0)

  private def wrapProvider[T](value: T): Provider[T] = {
    val provider = mock[Provider[T]]
    (provider.get _)
      .expects()
      .anyNumberOfTimes()
      .returning(value)

    provider
  }

  private def itemsToResponse(items: Seq[Long]): JsValue = {
    val array: JsValue = JsArray(
      items.map { id =>
        JsObject(Seq("id" -> JsNumber(id)))
      }
    )
    val result: JsValue = JsObject(Seq("items" -> array))

    JsObject(Seq("result" -> result))
  }

  private lazy val bnbSearcherClient =
    mock[BnbSearcherClient].applySideEffect { client =>
      (client
        .searchLikeSync(_: Long)(_: Traced))
        .expects {
          where { case (x, _) => SimilarSites.contains(x) }
        }
        .anyNumberOfTimes()
        .onCall { (id: Long, _) =>
          Try {
            itemsToResponse(SimilarSites(id))
          }
        }
    }

  private lazy val sitesGroupingService: SitesGroupingService = {
    val service = mock[SitesGroupingService]
    (service.getAllSites _)
      .expects()
      .returning(SimilarSites.keys.map(new Site(_)).asJavaCollection)
    service
  }

  private val controller = {
    val c = mock[ServerController]
    (c.register _).expects(*).anyNumberOfTimes()
    c
  }

  private def isToStorage(is: InputStream) = {
    SimilarSitesStorage.fromProto(
      Storage.SimilarSitesStorage.parseFrom(is)
    )
  }

  "SimilarSitesFetcher" should {
    "correctly fetch data" in {
      val fetcher = new SimilarSitesFetcher(
        controller,
        sitesGroupingService,
        bnbSearcherClient,
        wrapProvider(auctionStorage),
        wrapProvider(ctrStorage)
      )

      val baos = new ByteArrayOutputStream()
      val result = fetcher.fetch(None)

      result.get match {
        case ProduceResult.Produced(WriterData(writer), _) =>
          writer(baos)
      }
      val actual = isToStorage(new ByteArrayInputStream(baos.toByteArray))

      actual.similar.forall {
        case (id, similar) =>
          val currentSimilar = SimilarSites(id)

          similar.forall(currentSimilar.contains)
      } shouldBe true
    }
  }

}
