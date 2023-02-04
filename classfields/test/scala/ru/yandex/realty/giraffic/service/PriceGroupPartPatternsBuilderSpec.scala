package ru.yandex.realty.giraffic.service

import org.junit.runner.RunWith
import ru.yandex.realty.canonical.base.params.Parameter
import ru.yandex.realty.canonical.base.params.RequestParameter._
import ru.yandex.realty.canonical.base.request.{Request, RequestType}
import ru.yandex.realty.giraffic.model.links._
import ru.yandex.realty.giraffic.service.impl.patternBuilders.PriceGroupsPatternBuilder
import ru.yandex.realty.graph.core.GeoObjectType
import ru.yandex.realty.model.offer
import ru.yandex.realty.model.offer.{CategoryType, OfferType}
import ru.yandex.realty.model.region.NodeRgid
import zio.ZIO
import zio.random.Random
import zio.test.Assertion._
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}

@RunWith(classOf[ZTestJUnitRunner])
class PriceGroupPartPatternsBuilderSpec extends JUnitRunnableSpec {

  import CheckUtils._

  private val service = PriceGroupsPatternBuilder

  private type ParameterGen = Gen[Random, Parameter]
  private type ParametersGen = Gen[Random, Seq[Parameter]]

  private val rgidGen: ParameterGen =
    Gen.elements(Rgid(NodeRgid.MOSCOW), Rgid(NodeRgid.CHELYABINSKAYA_OBLAST))

  private val typeGen: ParametersGen =
    Gen.elements(
      Seq(Type(OfferType.SELL)),
      Seq(Type(OfferType.RENT)),
      Seq(Type(OfferType.RENT), RentTime(offer.RentTime.SHORT))
    )

  private val categoryGen: ParametersGen = {
    val justCategory: Seq[Seq[Parameter]] =
      CategoryType
        .values()
        .filter(c => c != CategoryType.UNKNOWN && c != CategoryType.UNUSED)
        .map(c => Seq(Category(c)))

    val commercial: Seq[Seq[Parameter]] =
      offer.CommercialType
        .values()
        .filter(_ != offer.CommercialType.UNKNOWN)
        .map(ct => Seq(Category(CategoryType.COMMERCIAL), CommercialType(ct)))

    val house: Seq[Seq[Parameter]] =
      offer.HouseType
        .values()
        .filter(_ != offer.HouseType.UNKNOWN)
        .map(ht => Seq(Category(CategoryType.HOUSE), HouseType(ht)))

    Gen.fromIterable(
      justCategory ++ commercial ++ house
    )
  }

  private val geoPartGen: ParametersGen =
    Gen.elements(
      Seq(StreetId(1), StreetName("street")),
      Seq(SubLocality(1), SubLocalityName("subname"), SubLocalityType(GeoObjectType.CITY_DISTRICT)),
      Seq(MetroGeoId(1))
    )

  private val noiseGen: ParametersGen =
    Gen.elements(Seq(LotAreaMin(10)))

  private val siteGen: ParametersGen =
    Gen.elements(Seq(SiteId(1), SiteName("site name")))

  def withEmptyGen[T](gen: Gen[Random, Seq[T]]): Gen[Random, Seq[T]] =
    Gen.concatAll(Iterable(gen, Gen.elements(Seq.empty[T])))

  private val searchRequestGen: Gen[Random, Request] =
    for {
      rgid <- rgidGen
      tp <- typeGen
      cat <- categoryGen
      geo <- withEmptyGen(geoPartGen)
      site <- withEmptyGen(siteGen)
      noise <- withEmptyGen(noiseGen)
    } yield Request.Raw(RequestType.Search, Seq(rgid) ++ tp ++ cat ++ geo ++ site ++ noise)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("PriceSuperGroupBuilder")(
      testM("should correctly return for sell") {
        val request = Request
          .Raw(RequestType.Search, Seq(Rgid(NodeRgid.MOSCOW), Type(OfferType.SELL), Category(CategoryType.APARTMENT)))

        def patternWithPrice(price: Long, text: String) =
          LinkPattern(
            Request.Raw(request.`type`, request.params ++ Seq(PriceMax(price))),
            s"до $text рублей"
          )

        for {
          res <- service.buildGroupsPattern(request)
        } yield assert(res) {
          hasSameGroupsPattern(
            GroupPatterns(
              Iterable(
                GroupPartPattern(
                  "Цены",
                  LinksPattern(
                    Iterable(
                      patternWithPrice(500000, "500 тысяч"),
                      patternWithPrice(800000, "800 тысяч"),
                      patternWithPrice(1000000, "миллиона"),
                      patternWithPrice(1500000, "1,5 миллионов"),
                      patternWithPrice(2000000, "2 миллионов"),
                      patternWithPrice(2500000, "2,5 миллионов"),
                      patternWithPrice(3000000, "3 миллионов"),
                      patternWithPrice(3500000, "3,5 миллионов"),
                      patternWithPrice(4000000, "4 миллионов"),
                      patternWithPrice(5000000, "5 миллионов"),
                      patternWithPrice(6000000, "6 миллионов")
                    ),
                    LinkSelectionStrategy.TakeAllWithOffers
                  )
                )
              )
            )
          )
        }
      },
      testM("should correctly return for rent") {
        val request = Request
          .Raw(RequestType.Search, Seq(Rgid(NodeRgid.MOSCOW), Type(OfferType.RENT), Category(CategoryType.APARTMENT)))

        def patternWithPrice(price: Long, text: String) =
          LinkPattern(
            Request.Raw(request.`type`, request.params ++ Seq(PriceMax(price))),
            s"до $text рублей"
          )

        for {
          res <- service.buildGroupsPattern(request)
        } yield assert(res) {
          hasSameGroupsPattern(
            GroupPatterns(
              Iterable(
                GroupPartPattern(
                  "Цены",
                  LinksPattern(
                    Iterable(
                      patternWithPrice(5000, "5 тысяч"),
                      patternWithPrice(8000, "8 тысяч"),
                      patternWithPrice(10000, "10 тысяч"),
                      patternWithPrice(15000, "15 тысяч"),
                      patternWithPrice(20000, "20 тысяч"),
                      LinkPattern(
                        Request.Raw(
                          request.`type`,
                          request.params ++ Seq(PriceMax(1000), RentTime(ru.yandex.realty.model.offer.RentTime.SHORT))
                        ),
                        "до тысячи рублей посуточно"
                      )
                    ),
                    LinkSelectionStrategy.TakeAllWithOffers
                  )
                )
              )
            )
          )
        }
      },
      testM("should correctly stay params") {
        checkAllM(searchRequestGen) { req =>
          val shouldStay = req.params
            .map(_.`type`)
            .filter(PriceGroupsPatternBuilder.RequiredToStayParams.contains)

          service
            .buildGroupsPattern(req)
            .map { groupsPattern =>
              groupsPattern.groups
                .flatMap(g => extractRequests(g.linksPattern).map(_.params.map(_.`type`).toSet))
            }
            .map(assert(_)(forall(hasSubset(shouldStay))))
        }
      },
      testM("should return empty parts for short rent") {
        val request = Request
          .Raw(
            RequestType.Search,
            Seq(
              Rgid(NodeRgid.MOSCOW),
              Type(OfferType.RENT),
              Category(CategoryType.APARTMENT),
              RentTime(ru.yandex.realty.model.offer.RentTime.SHORT)
            )
          )

        service.buildGroupsPattern(request).map(assert(_)(hasSameGroupsPattern(GroupPatterns(Iterable.empty))))
      },
      testM("should not return with some params") {
        def makeRequest(params: Seq[Parameter]): Request =
          Request.Raw(RequestType.Search, params)

        val requests = Seq(
          Seq(Category(CategoryType.ROOMS), Type(OfferType.SELL)),
          Seq(Category(CategoryType.LOT), Type(OfferType.SELL)),
          Seq(Category(CategoryType.GARAGE), Type(OfferType.SELL)),
          Seq(Category(CategoryType.COMMERCIAL), Type(OfferType.SELL)),
          Seq(
            Type(OfferType.RENT),
            RentTime(ru.yandex.realty.model.offer.RentTime.SHORT),
            Category(CategoryType.APARTMENT)
          )
        ).map(makeRequest)

        ZIO
          .foreach(requests)(service.buildGroupsPattern)
          .map { groups =>
            assert(groups)(forall(hasField("parts", _.groups, isEmpty)))
          }

      }
    )
}
