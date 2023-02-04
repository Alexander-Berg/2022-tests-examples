package ru.yandex.realty.cost.plus.builder

import org.joda.time.Instant
import eu.timepit.refined.auto._
import org.junit.runner.RunWith
import ru.yandex.realty.cost.plus.config.DomainConfig
import ru.yandex.realty.cost.plus.model.yml._
import ru.yandex.realty.cost.plus.service.builder.YmlBuilder
import ru.yandex.realty.cost.plus.utils.YmlFeedStatCollector
import ru.yandex.realty.traffic.utils.{CategoryTree, FilesService, UrlUtils}
import FilesService.FilesServiceConfig
import ru.yandex.realty.traffic.model.offer.{OfferRooms, OfferType}
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}
import zio.test._
import zio._

import java.nio.file.{Files, Path}
import scala.xml._

@RunWith(classOf[ZTestJUnitRunner])
class YmlBuilderSpec extends JUnitRunnableSpec {

  private val domainConfig = DomainConfig("desktop.host")

  private object NoOpCollector extends YmlFeedStatCollector.Service {
    override def collectStat(meterTag: String)(ymlFeed: YmlFeed): Task[Unit] = Task.unit
  }

  private def nextOffer(
    id: Long,
    category: CategoryTree.Category,
    data: YmlOfferData,
    sets: Set[WeakYmlSet]
  ): YmlOffer =
    YmlOffer(
      id = id.toString,
      name = s"name $id",
      urlPath = s"/stub/$id",
      price = YmlPrice(10000, isFrom = false),
      categoryId = category.id,
      imageUrl = s"https://image.host/stub/$id",
      relevance = 1.0d,
      additionalData = data,
      sets = sets
    )

  private val set1 = WeakYmlSet("/set/1", "name set 1")
  private val set2 = WeakYmlSet("/set/2", "name set 2")
  private val set3 = WeakYmlSet("/set/3", "name set 3")

  private val offers: Seq[YmlOffer] = Seq(
    nextOffer(
      1,
      CategoryTree.House,
      YmlOfferData.OfferSearchData(
        offerId = "1",
        area = None,
        rooms = None,
        offerType = OfferType.Sell,
        isFromAgent = None,
        updateTime = Instant.now(),
        offerForSiteTable = false
      ),
      Set(set1)
    ),
    nextOffer(
      2,
      CategoryTree.OneRoom,
      YmlOfferData.OfferSearchData(
        offerId = "2",
        area = None,
        rooms = Some(OfferRooms._1),
        offerType = OfferType.Sell,
        isFromAgent = Some(true),
        updateTime = Instant.now(),
        offerForSiteTable = false
      ),
      Set(set1, set2)
    ),
    nextOffer(
      3,
      CategoryTree.Site,
      YmlOfferData.ExtendedNewbuildingSearchData(
        additionalImages = Seq("img1", "img2"),
        "description",
        latitude = 0,
        longitude = 0,
        offersCount = 100
      ),
      Set(set3)
    )
  )

  private def testMWithBuilder(
    label: String
  )(test: YmlBuilder.Service => ZIO[TestEnvironment, Throwable, TestResult]): ZSpec[TestEnvironment, Throwable] = {
    val tempDir = Files.createTempDirectory("yml-builder-spec")
    val filesService = ZLayer.succeed(FilesServiceConfig(tempDir)) >>> FilesService.live
    val builderLayer = filesService ++ ZLayer.succeed(domainConfig) >>> YmlBuilder.live

    testM(label) {
      ZIO
        .service[YmlBuilder.Service]
        .provideLayer(builderLayer)
        .flatMap(test) <* ZIO.serviceWith[FilesService.Service](_.freeAllTemporary()).provideLayer(filesService)
    }
  }

  private def hasSetUrls(expected: Set[String]): Assertion[Elem] =
    Assertion.assertion("hasSetsUrls")(Render.param(expected)) { xml =>
      val actual = (xml \ "shop" \ "sets" \ "set" \ "url").map(_.text).toSet

      actual.size == expected.size && actual.intersect(expected).size == expected.size
    }

  private def hasCategories(expected: CategoryTree.Category*): Assertion[Elem] =
    Assertion.assertion("hasCategories")(Render.param(expected)) { xml =>
      val actual = (xml \ "shop" \ "categories" \ "category").map(_ \@ "id").map(_.toInt).toSet
      val expectedIds = expected.map(_.id).toSet

      actual.size == expectedIds.size && actual.intersect(expectedIds).size == expectedIds.size
    }

  private def hasNonEmptyUniqIds: Assertion[Elem] =
    Assertion.assertion("hasCategories")() { xml =>
      val offerIds = (xml \ "shop" \ "offers" \ "offer").map(_ \@ "id")

      offerIds.nonEmpty && offerIds.toSet.size == offerIds.size
    }

  private def parseXml(path: Path): Task[Elem] = Task.effect {
    val is = Files.newInputStream(path)
    val result = XML.load(is)
    is.close()

    result
  }

  private def makeUrls(sets: WeakYmlSet*): Set[String] =
    for {
      host <- Set(domainConfig.desktopHost)
      path <- sets.map(_.path)
    } yield UrlUtils.enrichPathWithHost(path, host)

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("YmlBuilder")(
      testMWithBuilder("should correctly build feeds") { builder =>
        for {
          feeds <- builder
            .buildYmls(
              ZStream.fromIterable(offers),
              "test",
              NoOpCollector
            )
            .flatMap(ZIO.foreach(_)(parseXml))
        } yield assert(feeds)(hasSize(equalTo(1))) &&
          assert(feeds) {
            forall {
              hasSetUrls(makeUrls(set1, set2, set3)) &&
              hasCategories(CategoryTree.House, CategoryTree.Apartment, CategoryTree.OneRoom, CategoryTree.Site) &&
              hasNonEmptyUniqIds
            }
          }
      }
    )
}
