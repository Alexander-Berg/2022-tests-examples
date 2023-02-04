package ru.yandex.realty.giraffic.service

import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.{refineMV, refineV}
import org.junit.runner.RunWith
import ru.yandex.realty.canonical.base.request.{Request, RequestType}
import ru.yandex.realty.giraffic.GirafficGroupsRequest
import ru.yandex.realty.giraffic.model.links._
import ru.yandex.realty.giraffic.service.FastLinkGroupsBuilder.FastLinkGroupsBuilder
import ru.yandex.realty.giraffic.service.mock.TestData
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.traffic.service.FrontendRouter
import ru.yandex.realty.traffic.service.FrontendRouter.FrontendRouter
import ru.yandex.realty.urls.router.model.{RouterUrlRequest, RouterUrlResponse, ViewType}
import ru.yandex.vertis.mockito.MockitoSupport
import zio.random.Random
import zio.test.Assertion._
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}
import zio._

import scala.util.Try

@RunWith(classOf[ZTestJUnitRunner])
class FastLinkGroupsBuilderSpec extends JUnitRunnableSpec with MockitoSupport {

  private val OfferInRequestInfoSeparator = ";"

  private def simpleRequest(offersCount: Int, translated: String): Request = {
    val result = mock[Request]
    when(result.key).thenReturn(s"$offersCount$OfferInRequestInfoSeparator$translated")
    result
  }

  private object DummyRouterService extends FrontendRouter.Service {

    override def parse(urlPath: String, viewType: ViewType): Task[Try[Request]] =
      Task.succeed(Try(simpleRequest(100, "input-url")))

    override def translate(requests: Iterable[RouterUrlRequest]): Task[Iterable[RouterUrlResponse]] =
      Task.succeed(
        requests.map { r =>
          val split = r.req.key.split(OfferInRequestInfoSeparator)

          val url =
            if (split.size != 2) {
              require(split.size < 2, s"Bad request ${r.req.key}")
              None
            } else split.lastOption

          RouterUrlResponse(
            r,
            url
          )
        }
      )
  }

  private object DummyCountManager$ extends CountManager.Service {
    override def getCountsMap(requests: Seq[Request]): RIO[Has[Traced], Map[String, Int]] = Task.succeed {
      requests.map { r =>
        r.key -> r.key.split(OfferInRequestInfoSeparator).headOption.map(_.toInt).getOrElse(0)
      }.toMap
    }
  }

  private def serviceLayer(builders: Seq[GroupPatternsBuilder.Service]): ULayer[FastLinkGroupsBuilder] = {
    val buildersLayer = ZLayer.succeed(builders)
    val routerLayer: ULayer[FrontendRouter] = ZLayer.succeed[FrontendRouter.Service](DummyRouterService)
    val nonEmptyListingManager =
      routerLayer ++
        TestData.actionObserver ++
        ZLayer.succeed[CountManager.Service](DummyCountManager$) >>> NonEmptyListingsManager.live

    buildersLayer ++
      nonEmptyListingManager ++
      TestData.actionObserver >>> FastLinkGroupsBuilder.live
  }

  private def serviceCall(builders: GroupPatternsBuilder.Service*): Task[FastLinkGroups] = {
    (for {
      builder <- ZIO.service[FastLinkGroupsBuilder.Service]
      res <- builder.buildFastLinkGroups(
        GirafficGroupsRequest(ViewType.Desktop, Request.Raw(RequestType.Search, Seq.empty))
      )
    } yield res).provideLayer(ZLayer.succeed[Traced](Traced.empty) ++ serviceLayer(builders))
  }

  private def patternBuilder(returning: GroupPatterns): GroupPatternsBuilder.Service =
    (_: Request) => Task.succeed(returning)

  private def groupPartTakeAll(name: String, links: Iterable[LinkPattern]): GroupPartPattern =
    GroupPartPattern(
      name,
      LinksPattern(
        links,
        LinkSelectionStrategy.TakeAllWithOffers
      )
    )

  //noinspection ScalaStyle
  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("FastLinkGroupsBuilder")(
      testM("should return empty response when no builders") {
        serviceCall().map(g => assert(g.groups)(isEmpty))
      },
      testM("correctly return for single builder") {
        val builderReturn = GroupPatterns(
          Iterable(
            groupPartTakeAll(
              "group1",
              Seq(
                LinkPattern(simpleRequest(1, "/with-furniture"), "С мебелью"),
                LinkPattern(simpleRequest(2, "/with-lot"), "С участком")
              )
            )
          )
        )
        val expected = FastLinkGroups(
          Seq(
            FastLinkGroup(
              "group1",
              refineV[NonEmpty](
                Seq(
                  FastLink("С участком", "/with-lot", refineMV[Positive](2)),
                  FastLink("С мебелью", "/with-furniture", refineMV[Positive](1))
                )
              ).right.get
            )
          )
        )

        serviceCall(patternBuilder(builderReturn))
          .map(assert(_)(equalTo(expected)))
      },
      testM("correctly join groups by name") {

        val withFurniture = LinkPattern(simpleRequest(1, "/with-furniture"), "С мебелью")
        val withPark = LinkPattern(simpleRequest(2, "/with-park"), "Рядом с парком")

        val buildersGen: Gen[Random, Seq[GroupPatternsBuilder.Service]] =
          Gen.fromIterable(
            Iterable(
              Seq( // по факту тут нечего сливать
                patternBuilder(
                  GroupPatterns(
                    Iterable(
                      groupPartTakeAll("group1", Seq(withFurniture, withPark))
                    )
                  )
                )
              ),
              Seq(
                // билдер вернул две разные группы
                patternBuilder(
                  GroupPatterns(
                    Iterable(
                      groupPartTakeAll("group1", Seq(withFurniture)),
                      groupPartTakeAll("group1", Seq(withPark))
                    )
                  )
                )
              ),
              Seq(
                // разные билдеры вернули части одной группы
                patternBuilder(GroupPatterns(Iterable(groupPartTakeAll("group1", Seq(withFurniture))))),
                patternBuilder(GroupPatterns(Iterable(groupPartTakeAll("group1", Seq(withPark)))))
              )
            )
          )

        val expected = FastLinkGroups(
          Seq(
            FastLinkGroup(
              "group1",
              refineV[NonEmpty](
                Seq(
                  FastLink(
                    "Рядом с парком",
                    "/with-park",
                    refineMV[Positive](2)
                  ),
                  FastLink(
                    "С мебелью",
                    "/with-furniture",
                    refineMV[Positive](1)
                  )
                )
              ).right.get
            )
          )
        )

        checkAllM(buildersGen) {
          serviceCall(_: _*).map(assert(_)(equalTo(expected)))
        }
      },
      testM("should not use urls where no translation or no offers") {
        val noOffers = LinkPattern(simpleRequest(0, "/som/url"), "no offers")

        val noUrl = LinkPattern(simpleRequest(10, ""), "no url")

        val urlAndOffers = LinkPattern(simpleRequest(10, "/some/url"), "url and offers")

        val builder = patternBuilder(
          GroupPatterns(
            Iterable(
              groupPartTakeAll("group1", Seq(noOffers, noUrl, urlAndOffers))
            )
          )
        )

        val expected = FastLinkGroup(
          "group1",
          refineV[NonEmpty](
            Seq(
              FastLink(
                "url and offers",
                "/some/url",
                refineMV[Positive](10)
              )
            )
          ).right.get
        )

        serviceCall(builder).map(assert(_)(equalTo(FastLinkGroups(Seq(expected)))))
      }
    )
}
