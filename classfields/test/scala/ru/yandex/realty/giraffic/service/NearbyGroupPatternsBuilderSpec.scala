package ru.yandex.realty.giraffic.service

import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineMV
import org.junit.runner.RunWith
import ru.yandex.realty.canonical.base.params.{Parameter, ParameterType, RequestParameter}
import ru.yandex.realty.canonical.base.request.{Request, RequestType}
import ru.yandex.realty.giraffic.model.GeoObject
import ru.yandex.realty.giraffic.model.links.LinkSelectionStrategy
import ru.yandex.realty.giraffic.service.GroupPatternsBuilder.GroupPatternsBuilder
import ru.yandex.realty.giraffic.service.RequestRender.RequestRender
import ru.yandex.realty.giraffic.service.impl.NearbyRequestRender
import ru.yandex.realty.giraffic.service.impl.patternBuilders.NearbyGroupPatternsBuilder
import ru.yandex.realty.giraffic.service.impl.patternBuilders.NearbyGroupPatternsBuilder.NearbyRequest
import ru.yandex.realty.giraffic.service.mock.TestData
import ru.yandex.realty.graph.core.GeoObjectType
import ru.yandex.realty.misc.enums.{IntEnum, IntEnumResolver}
import ru.yandex.realty.model.location.GeoPoint
import ru.yandex.realty.model.offer.{CategoryType, OfferType}
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.urls.router.model.filter.CategoryDeclarations
import zio._
import zio.random.Random
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}

import scala.collection.JavaConverters._

@RunWith(classOf[ZTestJUnitRunner])
class NearbyGroupPatternsBuilderSpec extends JUnitRunnableSpec {

  import CheckUtils._
  import RequestParameter._

  private val categoriesParams = Set(
    ParameterType.CategoryType,
    ParameterType.HouseType,
    ParameterType.CommercialType
  )

  private val requiredParamTypes = categoriesParams ++ Set(
    ParameterType.Rgid,
    ParameterType.OfferType,
    ParameterType.MetroGeoId,
    ParameterType.SubLocalityType,
    ParameterType.SubLocalityName,
    ParameterType.SubLocality,
    ParameterType.StreetId,
    ParameterType.StreetName
  )

  private val street = GeoObject.Street(1, "street1")

  private lazy val emptyGeoSearcher: GeoSearcher.Service = new GeoSearcher.Service {
    override def nearestWithOffers(query: GeoSearcher.NearestQuery): Task[Seq[GeoObject]] = Task.succeed(Seq.empty)

    override def getPoint(geoObject: GeoObject): Task[Option[GeoPoint]] = ZIO.none

    override def findStreet(id: Int): Task[Option[GeoObject.Street]] =
      if (id == street.id) Task.some(street)
      else Task.none

    override def findStreet(address: String): UIO[Option[Int]] = ZIO.none
  }

  private def serviceLayer(geoSearcher: GeoSearcher.Service): ULayer[GroupPatternsBuilder] = {
    val titleRender: ULayer[RequestRender[NearbyRequest]] =
      TestData.regionServiceLayer >>> (new NearbyRequestRender(_)).toLayer

    titleRender ++ ZLayer.succeed(geoSearcher) ++ ZLayer.succeed(TestData.siteService) ++ TestData.regionServiceLayer >>>
      (new NearbyGroupPatternsBuilder(_, _, _, _)).toLayer
  }

  private type RequestPartGen = Gen[Random, Seq[Parameter]]

  private def intEnumGen[T <: Enum[T] with IntEnum](resolver: IntEnumResolver[T]): Gen[Any, T] =
    Gen.fromIterable(resolver.allOf().asScala.filter(_ != resolver.getUnknownValue))

  private val typePartGen: RequestPartGen =
    intEnumGen(OfferType.R).map(t => Seq(Type(t)))

  private val categoryTypeGen: RequestPartGen =
    Gen.concatAll {
      Iterable(
        intEnumGen(CategoryType.R)
          .filter(_ != CategoryType.UNUSED)
          .map(Category)
          .map(Seq(_)),
        intEnumGen(ru.yandex.realty.model.offer.HouseType.R)
          .map(CategoryDeclarations.HouseWithRestriction)
          .map(_.params),
        intEnumGen(ru.yandex.realty.model.offer.CommercialType.R)
          .map(CategoryDeclarations.CommercialWithRestriction)
          .map(_.params)
      )
    }

  private val geoAdditionPartGen: RequestPartGen =
    Gen.fromIterable(
      Iterable(
        GeoObject.asParameters(street).toSeq,
        GeoObject.asParameters(GeoObject.Metro(20558)).toSeq,
        GeoObject.asParameters(GeoObject.District(193380, "Восточный", Some(GeoObjectType.CITY_DISTRICT))).toSeq,
        Seq.empty
      )
    )

  private val regionGen: RequestPartGen =
    Gen
      .elements(NodeRgid.MOSCOW, NodeRgid.BRIANSKAIA_OBLAST)
      .map(r => Seq(Rgid(r)))

  private val noiseParams: Set[Parameter] = Set(HasPond(true))

  private val noiseGen: RequestPartGen =
    Gen.fromIterable(
      noiseParams.map(Seq(_)) ++ Iterable(Seq.empty)
    )

  private val searchRequestGen: Gen[Random, Request] =
    Gen
      .crossAll(
        Seq(
          regionGen,
          typePartGen,
          categoryTypeGen,
          geoAdditionPartGen,
          noiseGen
        )
      )
      .map { params =>
        Request.Raw(RequestType.Search, params.flatten.distinct)
      }

  private def runSpec[E](geoSearcher: GeoSearcher.Service)(act: => RIO[GroupPatternsBuilder, TestResult]) =
    act.provideLayer(serviceLayer(geoSearcher))

  private def typeSpecs =
    checkAllM(searchRequestGen) { req =>
      runSpec(emptyGeoSearcher) {
        for {
          res <- GroupPatternsBuilder.buildGroupsPattern(req)
        } yield {
          val offerType =
            req.params.collectFirst { case Type(tp) => tp }.get

          val nearType = OfferType.values().find(t => t != OfferType.UNKNOWN && t != offerType).get

          val expected = Iterable(
            req
              .dropParams(
                req.params
                  .map(_.`type`)
                  .toSet
                  .diff(requiredParamTypes)
              )
              .dropParam(ParameterType.OfferType)
              .addParams(Type(nearType))
          )

          assert(res)(
            containsPartWithSameRequest(
              expected,
              LinkSelectionStrategy.TakeAllWithOffers
            )
          )
        }
      }
    }

  private def categorySpecs = {
    checkAllM(searchRequestGen) { req =>
      runSpec(emptyGeoSearcher) {
        for {
          res <- GroupPatternsBuilder.buildGroupsPattern(req)
        } yield {

          val categoryInRequest =
            req.params.collectFirst { case Category(ct) => ct }.get

          val expectedCategories = CategoryType
            .values()
            .filter(ct => ct != CategoryType.UNKNOWN && ct != CategoryType.UNUSED && ct != categoryInRequest)
            .map(CategoryDeclarations.JustCategory)

          val baseRequest =
            req
              .dropParams(
                req.params
                  .map(_.`type`)
                  .toSet
                  .diff(requiredParamTypes.diff(categoriesParams))
              )

          val expected =
            expectedCategories.map { ct =>
              baseRequest.addParams(ct.params: _*)
            }

          assert(res)(
            containsPartWithSameRequest(
              expected,
              LinkSelectionStrategy.TakeTopNWithOffers(refineMV[Positive](2))
            )
          )
        }
      }
    }
  }

  private def siteRequestsSpecs = {
    val params =
      Seq(
        Rgid(NodeRgid.MOSCOW),
        Type(OfferType.SELL),
        Category(CategoryType.APARTMENT),
        SiteId(268486),
        SiteName("Nevermind")
      )
    val req = Request.Raw(RequestType.Search, params)
    runSpec(emptyGeoSearcher) {
      for (res <- GroupPatternsBuilder.buildGroupsPattern(req)) yield {
        assertTrue(res.groups.flatMap(_.linksPattern.links).size == 14)
      }
    }
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("NearbySuperGroupBuilderService")(
      testM("Correctly work for near type")(typeSpecs),
      testM("Correctly work for near category")(categorySpecs),
      testM("Correctly work for requests with site offer")(siteRequestsSpecs)
    )
}
