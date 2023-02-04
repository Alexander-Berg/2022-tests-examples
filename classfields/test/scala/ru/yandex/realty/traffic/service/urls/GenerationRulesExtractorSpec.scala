package ru.yandex.realty.traffic.service.urls

import org.junit.runner.RunWith
import ru.yandex.common.util.collections.MultiMap
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.canonical.base.params.RequestParameter.{RoomsTotal, RoomsValue}
import ru.yandex.realty.giraffic.BySourceUrlsRequest._
import ru.yandex.realty.graph.core.GeoObjectType
import ru.yandex.realty.model.offer.{CategoryType, CommercialType, OfferType}
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.proto.offer.PricingPeriod
import ru.yandex.realty.proto.unified.offer.offertype.RentOffer
import ru.yandex.realty.traffic.TestData.{routerFilters, _}
import ru.yandex.realty.traffic.model.ClassifiedPart.SubLocality
import ru.yandex.realty.traffic.model.{ClassifiedPart, ClassifiedPartWithExtraParams, UrlsGenerationRule}
import ru.yandex.realty.traffic.service.urls.GenerationRulesExtractor.GenerationRulesExtractor
import ru.yandex.realty.urls.router.model.filter.{CategoryDeclarations, FilterDeclaration, TypeSpecification}
import ru.yandex.realty.util.HighwayLocator
import ru.yandex.realty.util.maps._
import ru.yandex.vertis.mockito.MockitoSupport
import zio._
import zio.test.Assertion._
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}
import zio.magic._
import zio.stream.ZStream

@RunWith(classOf[ZTestJUnitRunner])
class GenerationRulesExtractorSpec extends JUnitRunnableSpec {
  import GenerationRulesExtractorSpecData._

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("GenerationRulesExtractor")(
      testM(s"correctly return for salarevo-park") {
        val site = loadSite(Salarevo)

        val nearMetro = Seq("ryadom-metro")
        val deliveryDate = Seq("sdacha-2022", "sdacha-2023")
        val expectedFilters = Seq(
          "chistovaya-otdelka",
          "s-parkovkoy",
          "monolit",
          "panel",
          "voennaya-ipoteka",
          "s-ipotekoy",
          "s-matkapitalom",
          "zhk-comfort",
          "s-parkom",
          "s-vodoyomom"
        )

        val expectedRule = ExpectedGenerationRule(
          TypeSpecification.Newbuilding,
          CategoryDeclarations.NoParams,
          Map[ClassifiedPart, Seq[String]](
            ClassifiedPart.Empty -> (expectedFilters ++ nearMetro ++ deliveryDate),
            ClassifiedPart.Metro(144826) -> expectedFilters,
            ClassifiedPart.Metro(218467) -> expectedFilters,
            ClassifiedPart.Metro(144612) -> expectedFilters,
            ClassifiedPart.Metro(218466) -> expectedFilters,
            ClassifiedPart.Street(199508, "Саларьевская улица") -> (expectedFilters ++ nearMetro),
            ClassifiedPart.Developer(52308, "Company 1") -> (expectedFilters ++ nearMetro ++ deliveryDate),
            ClassifiedPart.SubLocality(17385368L, GeoObjectType.CITY_DISTRICT, "") -> (expectedFilters ++ nearMetro),
            ClassifiedPart.Rooms(RoomsTotal(RoomsValue.OneRoom)) -> Seq.empty,
            ClassifiedPart.RoomWithOther(ClassifiedPart.Metro(144826), RoomsTotal(RoomsValue.OneRoom)) -> Seq.empty,
            ClassifiedPart.RoomWithOther(ClassifiedPart.Metro(218467), RoomsTotal(RoomsValue.OneRoom)) -> Seq.empty,
            ClassifiedPart.RoomWithOther(ClassifiedPart.Metro(144612), RoomsTotal(RoomsValue.OneRoom)) -> Seq.empty,
            ClassifiedPart.RoomWithOther(ClassifiedPart.Metro(218466), RoomsTotal(RoomsValue.OneRoom)) -> Seq.empty,
            ClassifiedPart.RoomWithOther(
              ClassifiedPart.Street(199508, "Саларьевская улица"),
              RoomsTotal(RoomsValue.OneRoom)
            ) -> Seq.empty,
            ClassifiedPart
              .RoomWithOther(ClassifiedPart.Developer(52308, "Company 1"), RoomsTotal(RoomsValue.OneRoom)) -> Seq.empty,
            ClassifiedPart
              .RoomWithOther(
                ClassifiedPart.SubLocality(17385368L, GeoObjectType.CITY_DISTRICT, ""),
                RoomsTotal(RoomsValue.OneRoom)
              ) -> Seq.empty
          ).withParams()
        )

        GenerationRulesExtractor
          .extractRules(BySiteRequest(site), routerFilters)
          .provideLayer(testEnv)
          .map { rules =>
            assert(rules)(hasSize(equalTo(1))) &&
            assert(rules.head)(isSameGenerationRule(expectedRule))
          }
      },
      testM("correctly return for village") {

        val expectedFilters = Seq(
          "s-kluchami",
          "s-gazom",
          "s-kanalizaciej",
          "ryadom-vodoyom",
          "ryadom-park",
          "derevo",
          "kp-comfort"
        )

        val expected =
          ExpectedGenerationRule(
            TypeSpecification.Villages,
            CategoryDeclarations.NoParams,
            Map[ClassifiedPart, Seq[String]](
              ClassifiedPart.Empty -> expectedFilters,
              ClassifiedPart.SubLocality(230739L, GeoObjectType.CITY_DISTRICT, "") -> expectedFilters
            ).withParams()
          )

        GenerationRulesExtractor
          .extractRules(ByVillageRequest(VillagePresidentModel), routerFilters)
          .provideLayer(testEnv)
          .map { rules =>
            assert(rules)(hasSize(equalTo(1))) &&
            assert(rules.head)(isSameGenerationRule(expected))
          }
      },
      testM("correctly return for source with municipal district") {

        val fixedVillageDistrict = VillagePresidentModel.toBuilder
          .setLocation(
            VillagePresidentModel.getLocation.toBuilder
              .setDistrict(407450) // Кировский район
              .setRegionGraphId(NodeRgid.SPB)
          )
          .build()

        val expectedFilters = Seq(
          "s-kluchami",
          "s-gazom",
          "s-kanalizaciej",
          "ryadom-vodoyom",
          "ryadom-park",
          "derevo",
          "kp-comfort"
        )

        val expected =
          ExpectedGenerationRule(
            TypeSpecification.Villages,
            CategoryDeclarations.NoParams,
            Map[ClassifiedPart, Seq[String]](
              ClassifiedPart.Empty -> expectedFilters
            ).withParams()
          )

        GenerationRulesExtractor
          .extractRules(ByVillageRequest(fixedVillageDistrict), routerFilters)
          .provideLayer(testEnv)
          .map { rules =>
            assert(rules)(hasSize(equalTo(1))) &&
            assert(rules.head)(isSameGenerationRule(expected))
          }
      },
      testM("correctly return for offer with category with restriction") {

        val offerMetros = Seq(
          189449,
          218551,
          152925,
          189450
        )

        val districtPart = ClassifiedPart.SubLocality(193302, GeoObjectType.CITY_DISTRICT, "")

        val expectedCommercial = ExpectedGenerationRule(
          TypeSpecification.offers(Some(OfferType.SELL)),
          CategoryDeclarations.JustCategory(CategoryType.COMMERCIAL),
          (Seq(
            districtPart -> Seq.empty,
            ClassifiedPart.Empty -> Seq("biznes-center")
          ) ++ offerMetros.map(ClassifiedPart.Metro).map(_ -> Seq("biznes-center")))
            .toMap[ClassifiedPart, Seq[String]]
            .withParams()
        )

        val expectedOffice = ExpectedGenerationRule(
          TypeSpecification.offers(Some(OfferType.SELL)),
          CategoryDeclarations.CommercialWithRestriction(CommercialType.OFFICE),
          (Seq(
            districtPart -> Seq.empty,
            ClassifiedPart.Empty -> Seq("ryadom-metro", "biznes-center", "class-b-plus")
          ) ++ offerMetros.map(ClassifiedPart.Metro).map(_ -> Seq("biznes-center", "class-b-plus")))
            .toMap[ClassifiedPart, Seq[String]]
            .withParams()
        )

        GenerationRulesExtractor
          .extractRules(ByOfferRequest(officeOffer), routerFilters)
          .provideLayer(testEnv)
          .map { rules =>
            assert(rules)(hasSize(equalTo(2))) &&
            assert(rules)(exists(isSameGenerationRule(expectedCommercial))) &&
            assert(rules)(exists(isSameGenerationRule(expectedOffice)))
          }
      },
      testM("correctly return for short rent offer") {

        val metrosParts: Seq[ClassifiedPart] = Seq(
          20373,
          152918,
          189492,
          189451
        ).map(ClassifiedPart.Metro)

        val districtPart: ClassifiedPart = ClassifiedPart.SubLocality(12441, GeoObjectType.CITY_DISTRICT, "")

        val dailyRentFilter = Seq("posutochno")

        val emptyFiltersExpected = Seq(
          "bez-posrednikov",
          "do-10000",
          "do-15000",
          "do-20000",
          "do-8000",
          "ryadom-metro",
          "s-bolshoy-kuhney",
          "s-parkom",
          "s-parkovkoy",
          "s-vodoyomom",
          "s-vysokimi-potolkami",
          "v-novostroyke",
          "bez-komissii"
        )

        val districtFiltersExpected = Seq(
          "bez-posrednikov",
          "s-parkom",
          "s-vodoyomom",
          "s-vysokimi-potolkami",
          "s-bolshoy-kuhney",
          "v-novostroyke",
          "bez-komissii"
        )
        val offerRooms = RoomsTotal(RoomsValue.OneRoom)

        val expected = ExpectedGenerationRule(
          TypeSpecification.offers(Some(OfferType.RENT)),
          CategoryDeclarations.JustCategory(CategoryType.APARTMENT),
          (Map(
            ClassifiedPart.Empty -> (dailyRentFilter ++ emptyFiltersExpected),
            districtPart -> (dailyRentFilter ++ districtFiltersExpected),
            ClassifiedPart.Rooms(offerRooms) ->
              (dailyRentFilter ++ emptyFiltersExpected).diff(Seq("s-parkovkoy"))
          ) ++ metrosParts
            .map(
              _ -> (dailyRentFilter ++ emptyFiltersExpected).diff(Seq("ryadom-metro", "s-parkovkoy"))
            )
            .toMap ++
            (Seq(districtPart) ++ metrosParts)
              .map(ClassifiedPart.RoomWithOther(_, offerRooms))
              .map(_ -> dailyRentFilter))
            .toMap[ClassifiedPart, Seq[String]]
            .withParams()
        )

        GenerationRulesExtractor
          .extractRules(ByOfferRequest(shortRentOffer), routerFilters)
          .provideLayer(testEnv)
          .map { rules =>
            assert(rules)(hasSize(equalTo(1))) &&
            assert(rules.head)(isSameGenerationRule(expected))
          }
      },
      testM("correctly return for sell studio") {
        val offerRooms = RoomsTotal(RoomsValue.Studio)

        val metroParts: Seq[ClassifiedPart] = Seq(
          20466,
          20467,
          20480,
          20487,
          20502
        ).map(ClassifiedPart.Metro)

        val districtPart: ClassifiedPart = ClassifiedPart.SubLocality(193389, GeoObjectType.CITY_DISTRICT, "")

        val emptyFilters = Seq(
          "100m",
          "50m",
          "bez-posrednikov",
          "do-6000000",
          "kirpich",
          "ryadom-metro",
          "s-parkom",
          "s-parkovkoy",
          "v-pyatietazhnom-dome",
          "vtorichniy-rynok"
        )
        val metroWithRoomsFilters = Seq("ekonom-klass", "do-6000000", "bolshie-studii", "50m")
        val onlyMetroFilters =
          Seq("bez-posrednikov", "do-6000000", "100m", "vtorichniy-rynok", "v-pyatietazhnom-dome", "50m")
        val districtFilters = Seq("bez-posrednikov", "do-6000000", "vtorichniy-rynok", "50m")
        val roomFilters = Seq(
          "bez-posrednikov",
          "bolshie-studii",
          "do-6000000",
          "ekonom-klass",
          "kirpich",
          "ryadom-metro",
          "s-parkom",
          "s-parkovkoy",
          "vtorichniy-rynok",
          "ekonom-klass",
          "50m"
        )

        val expected = ExpectedGenerationRule(
          TypeSpecification.offers(Some(OfferType.SELL)),
          CategoryDeclarations.JustCategory(CategoryType.APARTMENT),
          (Map(
            ClassifiedPart.Empty -> emptyFilters,
            ClassifiedPart.RoomWithOther(districtPart, offerRooms) -> Seq("do-6000000", "50m"),
            districtPart -> districtFilters,
            ClassifiedPart.Rooms(offerRooms) -> roomFilters
          ) ++ metroParts.map(ClassifiedPart.RoomWithOther(_, offerRooms) -> metroWithRoomsFilters) ++
            metroParts.map(_ -> onlyMetroFilters)).toMap[ClassifiedPart, Seq[String]].withParams()
        )

        GenerationRulesExtractor
          .extractRules(ByOfferRequest(studioSellOffer), routerFilters)
          .provideLayer(testEnv)
          .map { rules =>
            assert(rules)(hasSize(equalTo(1))) &&
            assert(rules.head)(isSameGenerationRule(expected))
          }
      },
      testM("return the number of sublocalities after enrichment") {
        val enrichedRule = for {
          rule <- ZStream.fromIterableM(
            GenerationRulesExtractor
              .extractRules(ByOfferRequest(studioSellOffer), routerFilters)
              .provideLayer(testEnv)
          )
          enrichedRule <- ZStream.fromEffectOption(
            GenerationRulesExtractor
              .enrichRule(rule)
              .provideLayer(testEnv)
          )
        } yield enrichedRule
        enrichedRule.runCollect.map { rules =>
          val testValue = rules.flatMap(_.byClassifiedMap.toSeq).filter(_._1.part.isInstanceOf[SubLocality])
          assert(testValue.size)(equalTo(1))
        }
      },
      testM("Shouldn't return `dolgosrochnaya-arenda` on rent offers") {

        val offerGen =
          for {
            pp <- Gen.fromIterable(
              Iterable(
                PricingPeriod.PRICING_PERIOD_PER_MONTH,
                PricingPeriod.PRICING_PERIOD_PER_QUARTER,
                PricingPeriod.PRICING_PERIOD_PER_YEAR,
                PricingPeriod.PRICING_PERIOD_WHOLE_LIFE
              )
            )
          } yield {
            studioSellOffer.toBuilder
              .clearSell()
              .setRent(
                RentOffer
                  .newBuilder()
                  .setPricingPeriod(pp)
              )
              .build()
          }

        checkAllM(offerGen.map(ByOfferRequest)) { r =>
          GenerationRulesExtractor
            .extractRules(r, routerFilters)
            .provideLayer(testEnv)
            .map { rules =>
              rules
                .flatMap(_.byClassifiedMap.values.flatten)
                .map(_.name)
                .map(FilterDeclaration.unwrapName)
                .toSet
            }
            .map { filters =>
              assertTrue(!filters.contains("dolgosrochnaya-arenda"))
            }
        }
      }
    )

  private def hasCategory(ct: CategoryDeclarations): Assertion[UrlsGenerationRule] =
    hasField("category", _.categoryDeclarations, equalTo(ct))

  private def hasType(tp: TypeSpecification): Assertion[UrlsGenerationRule] =
    hasField("type", _.typeSpecification, equalTo(tp))

  private def hasFilters(
    byCls: Map[ClassifiedPartWithExtraParams, Seq[String]],
    strategy: FiltersCheckStrategy
  ): Assertion[UrlsGenerationRule] =
    hasField("filters", _.byClassifiedMap, hasSameFilters(byCls, strategy))

  private def isSameGenerationRule(
    expected: ExpectedGenerationRule,
    filtersCheckStrategy: FiltersCheckStrategy = FiltersCheckStrategy.FullyCheck
  ): Assertion[UrlsGenerationRule] =
    hasType(expected.tp) && hasCategory(expected.categoryDecl) && hasFilters(expected.filters, filtersCheckStrategy)
}

object GenerationRulesExtractorSpecData extends MockitoSupport {

  case class ExpectedGenerationRule(
    tp: TypeSpecification,
    categoryDecl: CategoryDeclarations,
    filters: Map[ClassifiedPartWithExtraParams, Seq[String]]
  )

  private def classifiedToString(cl: ClassifiedPart): String =
    cl match {
      case ClassifiedPart.Empty => "empty"
      case ClassifiedPart.Rooms(rooms) => s"rooms:${rooms.mask}"
      case ClassifiedPart.Metro(metroGeoId) => s"metro:$metroGeoId"
      case ClassifiedPart.Street(id, _) => s"street:$id"
      case ClassifiedPart.Direction(id, _) => s"direction:$id"
      case ClassifiedPart.SubLocality(rgid, _, _) => s"sublocality:$rgid"
      case ClassifiedPart.Site(siteId, _) => s"site:$siteId"
      case ClassifiedPart.Developer(devId, _) => s"dev:$devId"
      case ClassifiedPart.RoomWithOther(otherPart, rooms) => s"rooms:${rooms.mask};${classifiedToString(otherPart)}"
    }

  private def classifiedToString(cl: ClassifiedPartWithExtraParams): String =
    classifiedToString(cl.part)

  sealed trait FiltersCheckStrategy

  object FiltersCheckStrategy {
    case object DoNotCheck extends FiltersCheckStrategy
    case object IgnoreMissing extends FiltersCheckStrategy
    case object FullyCheck extends FiltersCheckStrategy
  }

  def hasSameFilters(
    expected: Map[ClassifiedPartWithExtraParams, Seq[String]],
    strategy: FiltersCheckStrategy
  ): Assertion[Map[ClassifiedPartWithExtraParams, Seq[FilterDeclaration]]] = {
    Assertion.assertion("hasSameFilters")(Render.param(expected)) { actual =>
      def internalCheck(failOnUnexpected: Boolean) = {
        val normExpected =
          expected.map {
            case (cl, value) => classifiedToString(cl) -> value
          }

        val normActual =
          actual.map {
            case (cl, value) => classifiedToString(cl) -> value.map(_.name)
          }

        val errors = normExpected.fullOuterJoin(normActual).flatMap {
          case (cl, (Some(e), Some(a))) =>
            if (a.toSet == e.toSet) None
            else
              Some(
                s"On classified [$cl], missed: ${e.diff(a).mkString("[", ", ", "]")}, unexpected: ${a.diff(e).mkString("[", ", ", "]")}"
              )
          case (cl, (None, Some(a))) =>
            Some(s"On classified [$cl] unexpected ${a.mkString("[", ", ", "]")}")
              .filter(_ => failOnUnexpected)
          case (cl, (Some(e), None)) =>
            Some(s"On classified [$cl] missed ${e.mkString("[", ", ", "]")}")
        }

        errors.foreach(System.err.println)
        errors.isEmpty
      }

      strategy match {
        case FiltersCheckStrategy.DoNotCheck => true
        case FiltersCheckStrategy.IgnoreMissing => internalCheck(failOnUnexpected = false)
        case FiltersCheckStrategy.FullyCheck => internalCheck(failOnUnexpected = true)
      }

    }
  }

  def loadSite(id: Long): Site = {
    val res = sitesService.getSiteById(id)
    require(res != null)
    res
  }

  lazy val testEnv: ULayer[GenerationRulesExtractor] =
    ZLayer.wire[GenerationRulesExtractor](
      ZLayer.succeed(sitesService),
      providerLayer(companies),
      NewbuildingClassifiedService.live,
      testPrometheus,
      ParamChecker.live,
      ZLayer.succeed(streetStorage),
      providerLayer(siteInfoStorage),
      regionServiceLayer,
      GenerationRulesExtractor.live,
      ZLayer.succeed(EmptyBuildingStorage),
      ZLayer.succeed[Provider[HighwayLocator]](() => new HighwayLocator(new MultiMap))
    )

  implicit class Sugar(data: Map[ClassifiedPart, Seq[String]]) {

    def withParams(): Map[ClassifiedPartWithExtraParams, Seq[String]] = data.map {
      case (part, filters) => ClassifiedPartWithExtraParams(part) -> filters
    }
  }

}
