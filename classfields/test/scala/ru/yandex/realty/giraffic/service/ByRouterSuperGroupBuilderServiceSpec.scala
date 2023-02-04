package ru.yandex.realty.giraffic.service

import org.junit.runner.RunWith
import ru.yandex.realty.canonical.base.params.Parameter
import ru.yandex.realty.canonical.base.request.{Request, RequestType}
import ru.yandex.realty.giraffic.model.links.{
  GroupPartPattern,
  GroupPatterns,
  LinkPattern,
  LinkSelectionStrategy,
  LinksPattern
}
import ru.yandex.realty.giraffic.service.GroupPatternsBuilder.GroupPatternsBuilder
import ru.yandex.realty.giraffic.service.impl.patternBuilders.RouterGroupsPatternBuilder
import ru.yandex.realty.giraffic.service.mock.TestData
import ru.yandex.realty.model.offer.{CategoryType, OfferType}
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.urls.landings.ListingFilterType
import zio.{ULayer, ZLayer}
import zio.test.Assertion._
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}

@RunWith(classOf[ZTestJUnitRunner])
class ByRouterSuperGroupBuilderServiceSpec extends JUnitRunnableSpec {

  import CheckUtils._
  import ru.yandex.realty.canonical.base.params.RequestParameter._

  private lazy val serviceLayer: ULayer[GroupPatternsBuilder] = {
    val env = ZLayer.succeed(TestData.routerFiltersProvider) ++ TestData.regionServiceLayer

    env >>> RouterGroupsPatternBuilder.live
  }

  private def simpleRequest(rgid: Long, tp: OfferType, category: CategoryType, additionParams: Parameter*) =
    Request.Raw(
      RequestType.Search,
      Seq(Rgid(rgid), Type(tp), Category(category)) ++ additionParams
    )

  private def hasParameter(expected: Parameter) =
    Assertion.assertion[Request]("hasParameter")(Render.param(expected)) { actual =>
      actual.params.contains(expected)
    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ByRouterSuperGroupBuilderService should")(
      correctlyReturnForRequestWithoutClassified(),
      correctlyReturnRequestWithClassified(),
      shouldNotContainInputRequest(),
      shouldNotContainFilterForNonMainGeos(),
      correctlyWorkWithCategoryWithClarification(),
      shouldNotDropSiteParams()
    )

  private def correctlyReturnForRequestWithoutClassified() =
    testM("Correctly return for request without classified") {
      val inputRequest = simpleRequest(NodeRgid.MOSCOW, OfferType.SELL, CategoryType.GARAGE)

      val expected = GroupPatterns(
        Iterable(
          GroupPartPattern(
            "Удобства",
            LinksPattern(
              Iterable(
                LinkPattern(
                  inputRequest.addKnowType(ListingFilterType.WithElectricitySupply),
                  "С электричеством"
                ),
                LinkPattern(
                  inputRequest.addKnowType(ListingFilterType.WithWaterSupply),
                  "С водой"
                ),
                LinkPattern(
                  inputRequest.addKnowType(ListingFilterType.WithHeatingSupply),
                  "С отоплением"
                )
              ),
              LinkSelectionStrategy.TakeAllWithOffers
            )
          )
        )
      )

      GroupPatternsBuilder
        .buildGroupsPattern(inputRequest)
        .map(assert(_)(hasSameGroupsPattern(expected)))
        .provideLayer(serviceLayer)
    }

  private def correctlyReturnRequestWithClassified() =
    testM("Correctly return fo request with classified") {
      val input = simpleRequest(NodeRgid.MOSCOW, OfferType.SELL, CategoryType.ROOMS, MetroGeoId(1))

      val expected = GroupPatterns(
        Iterable(
          GroupPartPattern(
            "Размеры и отделка",
            LinksPattern(
              Iterable(
                LinkPattern(
                  input.addParams(AreaMax(10)),
                  "Квартиры до 10 кв.м."
                ),
                LinkPattern(
                  input.addParams(AreaMax(11)),
                  "Квартиры до 11 кв.м."
                ),
                LinkPattern(
                  input.addParams(AreaMax(12)),
                  "До 12 кв.м."
                ),
                LinkPattern(
                  input.addParams(AreaMax(13)),
                  "До 13 кв.м."
                ),
                LinkPattern(
                  input.addParams(AreaMax(15)),
                  "До 15 кв.м."
                ),
                LinkPattern(
                  input.addParams(AreaMax(18)),
                  "До 18 кв.м."
                ),
                LinkPattern(
                  input.addParams(AreaMax(20)),
                  "До 20 кв.м."
                )
              ),
              LinkSelectionStrategy.TakeAllWithOffers
            )
          )
        )
      )
      GroupPatternsBuilder
        .buildGroupsPattern(input)
        .map(assert(_)(hasSameGroupsPattern(expected)))
        .provideLayer(serviceLayer)
    }

  private def shouldNotContainInputRequest() =
    testM("Should not contain input request") {
      val inputRequest = simpleRequest(
        NodeRgid.MOSCOW,
        OfferType.SELL,
        CategoryType.GARAGE,
        HasWater(true)
      )

      val inputBase = simpleRequest(
        NodeRgid.MOSCOW,
        OfferType.SELL,
        CategoryType.GARAGE
      )

      val expected = GroupPatterns(
        Iterable(
          GroupPartPattern(
            "Удобства",
            LinksPattern(
              Iterable(
                LinkPattern(
                  inputBase.addKnowType(ListingFilterType.WithElectricitySupply),
                  "С электричеством"
                ),
                LinkPattern(
                  inputBase.addKnowType(ListingFilterType.WithHeatingSupply),
                  "С отоплением"
                )
              ),
              LinkSelectionStrategy.TakeAllWithOffers
            )
          )
        )
      )

      GroupPatternsBuilder
        .buildGroupsPattern(inputRequest)
        .map(assert(_)(hasSameGroupsPattern(expected)))
        .provideLayer(serviceLayer)
    }

  private def shouldNotContainFilterForNonMainGeos() =
    testM("Should not contain filter for non main geos") {
      val input = simpleRequest(
        NodeRgid.CHELYABINSKAYA_OBLAST,
        OfferType.SELL,
        CategoryType.ROOMS,
        StreetId(1),
        StreetName("street")
      )

      GroupPatternsBuilder
        .buildGroupsPattern(input)
        .map(res => assert(res.groups)(isEmpty))
        .provideLayer(serviceLayer)
    }
  //noinspection ScalaStyle
  private def correctlyWorkWithCategoryWithClarification() =
    testM("Correctly work with category with clarification") {

      val justCategoryReq = simpleRequest(
        NodeRgid.MOSCOW,
        OfferType.RENT,
        CategoryType.HOUSE,
        SubLocality(1)
      )

      val clarificationReq =
        simpleRequest(
          NodeRgid.MOSCOW,
          OfferType.RENT,
          CategoryType.HOUSE,
          HouseType(ru.yandex.realty.model.offer.HouseType.TOWNHOUSE),
          SubLocality(1)
        )

      def parkPattern(baseRequest: Request) = LinkPattern(
        baseRequest.addParams(HasPark(true)),
        "Рядом с парком"
      )
      def pondPattern(baseRequest: Request) = LinkPattern(
        baseRequest.addParams(HasPond(true)),
        "Рядом с водоёмом"
      )

      def bigLotPattern(baseRequest: Request) = LinkPattern(
        baseRequest.addParams(LotAreaMin(10)),
        "С большим участком"
      )

      val expectedJustCategory =
        GroupPatterns(
          Iterable(
            GroupPartPattern(
              "Удобства",
              LinksPattern(
                Iterable(
                  bigLotPattern(justCategoryReq)
                ),
                LinkSelectionStrategy.TakeAllWithOffers
              )
            ),
            GroupPartPattern(
              "Инфраструктура",
              LinksPattern(
                Iterable(
                  pondPattern(justCategoryReq),
                  parkPattern(justCategoryReq)
                ),
                LinkSelectionStrategy.TakeAllWithOffers
              )
            )
          )
        )

      val expectedClarificationCategory =
        GroupPatterns(
          Iterable(
            GroupPartPattern(
              "Удобства",
              LinksPattern(
                Iterable(
                  bigLotPattern(clarificationReq)
                ),
                LinkSelectionStrategy.TakeAllWithOffers
              )
            )
          )
        )

      (for {
        justCategoryRes <- GroupPatternsBuilder.buildGroupsPattern(justCategoryReq)
        clarificationRes <- GroupPatternsBuilder.buildGroupsPattern(clarificationReq)
      } yield {
        assert(justCategoryRes)(hasSameGroupsPattern(expectedJustCategory)) &&
        assert(clarificationRes)(hasSameGroupsPattern(expectedClarificationCategory))
      }).provideLayer(serviceLayer)
    }

  private def shouldNotDropSiteParams() =
    testM("Shouldn't drop site params") {
      val request = Request.Raw(
        RequestType.Search,
        Seq(
          Rgid(NodeRgid.MOSCOW),
          Type(OfferType.SELL),
          Category(CategoryType.APARTMENT),
          SiteId(1)
        )
      )

      for {
        result <- GroupPatternsBuilder.buildGroupsPattern(request).provideLayer(serviceLayer)
      } yield {
        val allRequests = result.groups
          .map(_.linksPattern)
          .flatMap(extractRequests)

        assert(allRequests)(forall(hasParameter(SiteId(1))))
      }
    }
}
