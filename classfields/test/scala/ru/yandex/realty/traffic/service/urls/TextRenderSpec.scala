package ru.yandex.realty.traffic.service.urls

import org.junit.runner.RunWith
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.canonical.base.params.Parameter
import ru.yandex.realty.canonical.base.params.RequestParameter.{RoomsTotal, RoomsValue}
import ru.yandex.realty.graph.core.GeoObjectType
import ru.yandex.realty.model.offer.{CategoryType, CommercialType, HouseType, OfferType}
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.sites.{CompaniesStorage, SitesGroupingService}
import ru.yandex.realty.traffic.TestData
import ru.yandex.realty.traffic.model.{ClassifiedPart, RenderRequest}
import ru.yandex.realty.urls.router.model.filter.{CategoryDeclarations, FilterDeclaration, TypeSpecification}
import zio.ZLayer
import zio.test.Assertion._
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}

@RunWith(classOf[ZTestJUnitRunner])
class TextRenderSpec extends JUnitRunnableSpec {

  private case class TestCase(
    request: RenderRequest,
    expected: String
  )

  private def filter(code: String, params: Set[Parameter] = Set.empty): FilterDeclaration =
    FilterDeclaration.RawFilter(FilterDeclaration.wrapName(code), params)

  private def renderRequest(
    rgid: Long = NodeRgid.MOSCOW,
    typeSpec: TypeSpecification = TypeSpecification.offers(Some(OfferType.SELL)),
    categoryDecl: CategoryDeclarations = CategoryDeclarations.JustCategory(CategoryType.APARTMENT),
    classifiedPart: ClassifiedPart = ClassifiedPart.Empty,
    filters: Seq[FilterDeclaration] = Seq.empty
  ): RenderRequest =
    RenderRequest(
      Some(rgid),
      Some(typeSpec),
      Some(categoryDecl),
      classifiedPart,
      filters
    )

  private lazy val tests: Seq[TestCase] = Seq(
    TestCase(
      renderRequest(),
      "Купить квартиру в Москве"
    ),
    TestCase(
      renderRequest(
        classifiedPart = ClassifiedPart.Rooms(RoomsTotal(RoomsValue.OneRoom))
      ),
      "Купить 1-комнатную квартиру в Москве"
    ),
    TestCase(
      renderRequest(
        classifiedPart = ClassifiedPart.Rooms(RoomsTotal(RoomsValue.OneRoom)),
        filters = Seq(filter("s-remontom"))
      ),
      "Купить 1-комнатную квартиру с ремонтом в Москве"
    ),
    TestCase(
      renderRequest(
        classifiedPart = ClassifiedPart.Rooms(RoomsTotal(RoomsValue.OneRoom)),
        filters = Seq(filter("s-remontom"), filter("khrushevskiy"))
      ),
      "Купить 1-комнатную квартиру с ремонтом и в хрущёвке в Москве"
    ),
    TestCase(
      renderRequest(
        classifiedPart = ClassifiedPart.Rooms(RoomsTotal(RoomsValue.Plus4)),
        filters = Seq(filter("s-remontom"), filter("khrushevskiy"))
      ),
      "Купить многокомнатную квартиру с ремонтом и в хрущёвке в Москве"
    ),
    TestCase(
      renderRequest(
        classifiedPart = ClassifiedPart.Empty,
        filters = Seq(filter("s-remontom"), filter("kirpich"))
      ),
      "Купить квартиру с ремонтом и в кирпичном доме в Москве"
    ),
    TestCase(
      renderRequest(
        classifiedPart = ClassifiedPart.Metro(20475),
        filters = Seq()
      ),
      "Купить квартиру в Москве у метро Павелецкая"
    ),
    TestCase(
      renderRequest(
        classifiedPart = ClassifiedPart.Metro(20475),
        filters = Seq(filter("s-remontom"))
      ),
      "Купить квартиру с ремонтом в Москве у метро Павелецкая"
    ),
    TestCase(
      renderRequest(
        classifiedPart = ClassifiedPart.RoomWithOther(
          ClassifiedPart.Metro(20475),
          RoomsTotal(RoomsValue.OneRoom)
        ),
        filters = Seq(filter("s-remontom"))
      ),
      "Купить 1-комнатную квартиру с ремонтом в Москве у метро Павелецкая"
    ),
    TestCase(
      renderRequest(
        classifiedPart = ClassifiedPart.RoomWithOther(
          ClassifiedPart.Metro(20475),
          RoomsTotal(RoomsValue.OneRoom)
        )
      ),
      "Купить 1-комнатную квартиру в Москве у метро Павелецкая"
    ),
    TestCase(
      renderRequest(
        classifiedPart = ClassifiedPart.SubLocality(193391, GeoObjectType.CITY_DISTRICT, "")
      ),
      "Купить квартиру в Москве в Богородском"
    ),
    TestCase(
      renderRequest(
        classifiedPart = ClassifiedPart.Street(1, "улица Ленина")
      ),
      "Купить квартиру в Москве улица Ленина"
    ),
    TestCase(
      renderRequest(
        rgid = NodeRgid.MOSCOW_AND_MOS_OBLAST,
        classifiedPart = ClassifiedPart.Direction(39, "Красногорское шоссе")
      ),
      "Купить квартиру в Москве и МО Красногорское шоссе"
    ),
    TestCase(
      renderRequest(typeSpec = TypeSpecification.offers(Some(OfferType.RENT))),
      "Снять квартиру в Москве"
    ),
    TestCase(
      renderRequest(categoryDecl = CategoryDeclarations.CommercialWithRestriction(CommercialType.OFFICE)),
      "Купить офис в Москве"
    ),
    TestCase(
      renderRequest(categoryDecl = CategoryDeclarations.HouseWithRestriction(HouseType.DUPLEX)),
      "Купить дуплекс в Москве"
    ),
    TestCase(
      renderRequest(
        filters = Seq(filter("bolshie-kvartiry"))
      ),
      "Купить квартиру большие квартиры в Москве" // todo VERTISTRAF-2325
    )
  )

  private def makeTest(testCase: TestCase) =
    testM(s"correctly generate '${testCase.expected}'") {
      TextRender
        .render(testCase.request)
        .map { res =>
          assert(res)(equalTo(testCase.expected))
        }
        .provideLayer {
          ZLayer.succeed(TestData.filtersProvider) ++
            TestData.regionServiceLayer ++
            ZLayer.succeed[Provider[CompaniesStorage]](() => null) ++
            ZLayer.succeed[SitesGroupingService](null) >>> TextRender.live
        }
    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("TextRender")(
      tests.map(makeTest): _*
    )
}
