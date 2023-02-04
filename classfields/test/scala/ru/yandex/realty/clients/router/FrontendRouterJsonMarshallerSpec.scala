package ru.yandex.realty.clients.router

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json._
import ru.yandex.realty.canonical.base.params.{Parameter, ParameterType, RequestParameter}
import ru.yandex.realty.clients.router.parser._
import ru.yandex.realty.model.offer.{CategoryType, CommercialBuildingType, CommercialType, OfferType}
import ru.yandex.realty.urls.landings.ListingFilterType
import ru.yandex.realty.urls.router.model.filter._
import ru.yandex.realty.urls.router.model.filter.CategoryDeclarations.{CommercialWithRestriction, JustCategory}
import ru.yandex.realty.urls.router.model.filter.FilterDeclaration.FilterName
import ru.yandex.realty.util.maps._

import scala.annotation.tailrec

@RunWith(classOf[JUnitRunner])
class FrontendRouterJsonMarshallerSpec extends WordSpec with Matchers {

  private def parseResource(name: String) =
    Json.parse(getClass.getClassLoader.getResourceAsStream(name))
  private lazy val routerFiltersRaw =
    parseResource("frontend-router-filters.json")

  private def getWhenUniqOrFail(filters: Seq[FilterDeclaration]) = {

    val paramsSets = filters.map { f =>
      f.parameters
        .sortBy(_.`type`)
        .map(_.toString)
        .mkString(";")
    }.toSet

    withClue(s"expected only 1 declaration for ${filters.head.name}: [$paramsSets]") {
      paramsSets should have size 1
    }

    filters.head
  }

  @tailrec
  private def normalizeValue(jsValue: JsValue): JsValue =
    jsValue match {
      case JsNumber(n) => JsString(n.toString())
      case JsArray(n) if n.size == 1 => normalizeValue(n.head)
      case JsArray(n) => JsArray(n.sortBy(_.toString()))
      case o => o
    }

  private def normalizeValues(mp: Map[String, JsValue]) =
    mp.map {
      case (n, v) =>
        n -> normalizeValue(v)
    }

  private def assertFilterEquals(filterDeclaration: FilterDeclaration, filterObj: JsObject) = {
    val rawParams = filterObj.value.toMap
    val actual =
      filterDeclaration.parameters
        .map(p => p.`type`.toString -> p.jsParam)
        .toMap

    normalizeValues(actual) should contain theSameElementsAs normalizeValues(rawParams)
  }

  private def assertDistinctClassifieds(fs: Seq[TraverseFilters]) =
    fs.map(_.classifiedParameters.mask).toSet should have size fs.size

  private def assertTraverseFiltersEquals(expected: Seq[TraverseFilters])(actual: Seq[TraverseFilters]) = {
    actual should have size expected.size

    withClue("incorrect test setup") {
      assertDistinctClassifieds(expected)
    }

    withClue("bad input classifieds") {
      assertDistinctClassifieds(actual)
    }
  }

  private def filter(name: String, params: Parameter*) =
    ListingFilterType
      .findByFilterName(name)
      .getOrElse(FilterDeclaration.RawFilter(FilterDeclaration.wrapName(name), params.toSet))

  private def assertSubSet[A](a: Set[A], sub: Set[A]): Unit =
    sub.foreach { e =>
      if (!a.contains(e)) fail(s"Missed $e")
    }

  "FrontendRouterJsonMarshaller" should {
    "be correctly configured" in {
      Option(routerFiltersRaw) shouldBe defined
    }

    "correctly parse filter declarations" in {
      val parsed = ListingFiltersParser.parse(routerFiltersRaw).get

      val parsedAliasToFilter = parsed.listings
        .flatMap(_.types)
        .flatMap(_.traverseFilters)
        .flatMap(_.parametersGroups)
        .groupBy(_.name)
        .map {
          case (name, filters) => FilterDeclaration.wrapName(name) -> getWhenUniqOrFail(filters)
        }

      val rawActualFilters = (routerFiltersRaw \ "declaration").as[Map[FilterName, JsObject]]

      assertSubSet(rawActualFilters.keySet, parsedAliasToFilter.keySet)

      parsedAliasToFilter.join(rawActualFilters).foreach {
        case (filterName, (filterDecl, rawFilter)) =>
          withClue(s"on `$filterName`") {
            assertFilterEquals(filterDecl, rawFilter)
          }
      }
    }

    "correctly parse commercial sell" in {
      val parsed = ListingFiltersParser.parse(routerFiltersRaw).get

      val commercial = parsed.listings.find { l =>
        l.typeSpecification == TypeSpecification.offers(Some(OfferType.SELL)) && l.types.exists {
          case TypeDeclaration(JustCategory(CategoryType.COMMERCIAL), _) => true
          case _ => false
        }
      }

      val manufacturing = parsed.listings.find { l =>
        l.typeSpecification == TypeSpecification.offers(Some(OfferType.SELL)) && l.types.exists {
          case TypeDeclaration(CommercialWithRestriction(CommercialType.MANUFACTURING), _) => true
          case _ => false
        }
      }

      commercial shouldBe defined
      manufacturing shouldBe defined

      def cbtFilter(name: String, cbt: CommercialBuildingType): FilterDeclaration =
        FilterDeclaration.RawFilter(
          FilterDeclaration.wrapName(name),
          Set[Parameter](RequestParameter.CommercialBuildingType(cbt))
        )

      val expectedFilters = Seq(
        cbtFilter("biznes-center", CommercialBuildingType.BUSINESS_CENTER),
        cbtFilter("sklad", CommercialBuildingType.WAREHOUSE),
        cbtFilter("torgoviy-center", CommercialBuildingType.SHOPPING_CENTER),
        cbtFilter("zdaniye", CommercialBuildingType.DETACHED_BUILDING),
        cbtFilter("zhiloy-dom", CommercialBuildingType.RESIDENTIAL_BUILDING)
      )

      val expectedFilterGroups = Seq(
        ClassifiedParameters.Empty,
        ClassifiedParameters(ParameterType.MetroGeoId),
        ClassifiedParameters(ParameterType.StreetId, ParameterType.StreetName)
      ).map { clPart =>
        TraverseFilters(
          clPart,
          expectedFilters
        )
      }

      assertTraverseFiltersEquals(expectedFilterGroups) {
        commercial.get.types.collectFirst {
          case TypeDeclaration(_, filters) => filters
        }.get
      }

      val expectedManufacturing =
        expectedFilters ++
          Seq(
            ListingFilterType.NearMetro,
            ListingFilterType.HasFurniture,
            ListingFilterType.DirectRent,
            ListingFilterType.SubRent,
            ListingFilterType.SaleOfLeaseRights,
            ListingFilterType.SeparateEntrance
          ).map(FilterDeclaration.KnownFilter)

      val expectedManufacturingFilterGroups = Seq(
        ClassifiedParameters.Empty,
        ClassifiedParameters(ParameterType.MetroGeoId),
        ClassifiedParameters(ParameterType.StreetId, ParameterType.StreetName)
      ).map { clPart =>
        TraverseFilters(
          clPart,
          expectedManufacturing
        )
      }

      assertTraverseFiltersEquals(expectedManufacturingFilterGroups) {
        manufacturing.get.types.collectFirst {
          case TypeDeclaration(_, filters) => filters
        }.get
      }
    }
  }
}
