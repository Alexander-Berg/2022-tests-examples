package ru.yandex.realty.traffic.service.urls

import org.junit.runner.RunWith
import ru.yandex.realty.canonical.base.params.Parameter
import ru.yandex.realty.canonical.base.params.ParameterType.ParameterType
import ru.yandex.realty.canonical.base.request.{Request, RequestType}
import ru.yandex.realty.giraffic.BySourceUrlsRequest
import ru.yandex.realty.model.offer
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.proto.offer.PricingPeriod
import ru.yandex.realty.proto.unified.offer.UnifiedOffer
import ru.yandex.realty.proto.unified.offer.address.LocationUnified
import ru.yandex.realty.proto.unified.offer.offertype.RentOffer
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.traffic.TestData
import ru.yandex.realty.traffic.logic.extractor.RequestsInfoExtractor
import ru.yandex.realty.traffic.model.{ClassifiedPart, ClassifiedPartWithExtraParams, UrlsGenerationRule}
import ru.yandex.realty.traffic.service.{FilterCombinationsSelection, FrontendRouter}
import ru.yandex.realty.traffic.service.FrontendRouter.FrontendRouter
import ru.yandex.realty.traffic.service.urls.SourceUrlsExtractor.{Error, SourceUrlsExtractor}
import ru.yandex.realty.traffic.service.urls.TextRender.TextRender
import ru.yandex.realty.traffic.service.urls.generator.RequestWithTextGenerator
import ru.yandex.realty.traffic.service.urls.generator.live.ByFiltersRequestWithTextGenerator
import ru.yandex.realty.urls.router.model.filter.FilterDeclaration.FilterName
import ru.yandex.realty.urls.router.model.filter.{
  CategoryDeclarations,
  FilterDeclaration,
  ListingFilters,
  TypeSpecification
}
import ru.yandex.realty.urls.router.model.{RouterUrlRequest, RouterUrlResponse}
import ru.yandex.realty.util.Mappings._
import zio._
import zio.magic._
import zio.test.Assertion._
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}
import zio.test.mock.Expectation._
import zio.test.mock.{mockable, Expectation}

@RunWith(classOf[ZTestJUnitRunner])
class SourceUrlsExtractorSpec extends JUnitRunnableSpec {

  import SourceUrlsExtractorSpecData._
  import ru.yandex.realty.canonical.base.params.RequestParameter._

  implicit private val traced: Traced = Traced.empty
  private lazy val allKnownFilters: Map[FilterName, FilterDeclaration] =
    TestData.routerFilters.listings
      .flatMap(_.types)
      .flatMap(_.traverseFilters)
      .flatMap(_.parametersGroups)
      .map(f => f.name -> f)
      .toMap

  private def findFilter(name: String): FilterDeclaration =
    allKnownFilters.getOrElse(
      FilterDeclaration.wrapName(name),
      throw new IllegalArgumentException(s"Bad spec! No filter with name $name found.")
    )

  private def makeOffer(rgid: Long, isShortRent: Boolean = false) = {
    UnifiedOffer
      .newBuilder()
      .setLocation(LocationUnified.newBuilder().setRgid(rgid))
      .applyTransformIf(
        isShortRent,
        _.setRent(
          RentOffer
            .newBuilder()
            .setPricingPeriod(PricingPeriod.PRICING_PERIOD_PER_DAY)
        )
      )
      .build()
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("SourceUrlsExtractor")(
    extractSpec("return empty urls response", Map.empty, Iterable.empty) {
      SourceUrlsExtractor
        .extractUrls(BySourceUrlsRequest.ByOfferRequest(UnifiedOffer.getDefaultInstance))
        .map(assert(_)(isEmpty))
    },
    extractSpec(
      "correctly return for MSK",
      Map(NodeRgid.MOSCOW -> Set(NodeRgid.MOSCOW, NodeRgid.MOS_OBLAST, NodeRgid.MOSCOW_AND_MOS_OBLAST)),
      Iterable(
        UrlsGenerationRule(
          TypeSpecification.offers(Some(offer.OfferType.SELL)),
          CategoryDeclarations.JustCategory(offer.CategoryType.APARTMENT),
          Map(
            ClassifiedPartWithExtraParams(ClassifiedPart.Empty) -> Seq(
              findFilter("s-remontom"),
              findFilter("novostroyki")
            ),
            ClassifiedPartWithExtraParams(ClassifiedPart.Rooms(RoomsTotal(RoomsValue.Studio))) -> Seq(
              findFilter("studii-11m"),
              findFilter("monolit")
            )
          )
        )
      )
    ) {

      def makeRequest(rgid: Long, filterParams: Parameter*): Request = {
        val params = Seq(Rgid(rgid), Type(offer.OfferType.SELL), Category(offer.CategoryType.APARTMENT)) ++ filterParams
        val usedP = scala.collection.mutable.Set.empty[ParameterType]
        Request.Raw(RequestType.Search, params.filter(p => usedP.add(p.`type`)))
      }

      val expectedKeys = Seq(
        makeRequest(NodeRgid.MOSCOW),
        makeRequest(NodeRgid.MOSCOW, findFilter("s-remontom").parameters: _*),
        makeRequest(NodeRgid.MOSCOW, findFilter("novostroyki").parameters: _*),
        makeRequest(NodeRgid.MOSCOW, findFilter("s-remontom").parameters ++ findFilter("novostroyki").parameters: _*),
        makeRequest(NodeRgid.MOSCOW, RoomsTotal(RoomsValue.Studio)),
        makeRequest(NodeRgid.MOSCOW, findFilter("studii-11m").parameters ++ Seq(RoomsTotal(RoomsValue.Studio)): _*),
        makeRequest(NodeRgid.MOSCOW, findFilter("monolit").parameters ++ Seq(RoomsTotal(RoomsValue.Studio)): _*),
        makeRequest(
          NodeRgid.MOSCOW,
          findFilter("monolit").parameters ++ findFilter("studii-11m").parameters ++ Seq(RoomsTotal(RoomsValue.Studio)): _*
        ),
        makeRequest(NodeRgid.MOS_OBLAST),
        makeRequest(NodeRgid.MOS_OBLAST, findFilter("s-remontom").parameters: _*),
        makeRequest(NodeRgid.MOS_OBLAST, findFilter("novostroyki").parameters: _*),
        makeRequest(
          NodeRgid.MOS_OBLAST,
          findFilter("s-remontom").parameters ++ findFilter("novostroyki").parameters: _*
        ),
        makeRequest(NodeRgid.MOS_OBLAST, RoomsTotal(RoomsValue.Studio)),
        makeRequest(NodeRgid.MOS_OBLAST, findFilter("monolit").parameters ++ Seq(RoomsTotal(RoomsValue.Studio)): _*),
        makeRequest(NodeRgid.MOSCOW_AND_MOS_OBLAST),
        makeRequest(NodeRgid.MOSCOW_AND_MOS_OBLAST, findFilter("s-remontom").parameters: _*),
        makeRequest(NodeRgid.MOSCOW_AND_MOS_OBLAST, findFilter("novostroyki").parameters: _*),
        makeRequest(
          NodeRgid.MOSCOW_AND_MOS_OBLAST,
          findFilter("s-remontom").parameters ++ findFilter("novostroyki").parameters: _*
        ),
        makeRequest(NodeRgid.MOSCOW_AND_MOS_OBLAST, RoomsTotal(RoomsValue.Studio)),
        makeRequest(
          NodeRgid.MOSCOW_AND_MOS_OBLAST,
          findFilter("monolit").parameters ++ Seq(RoomsTotal(RoomsValue.Studio)): _*
        )
      ).map(_.toString)

      SourceUrlsExtractor
        .extractUrls(BySourceUrlsRequest.ByOfferRequest(makeOffer(NodeRgid.MOSCOW)))
        .map(_.map(_.urlPath))
        .map(assert(_)(hasSameElements(expectedKeys)))
    },
    extractSpec(
      "correctly work with ekonom-klass",
      Map(NodeRgid.MOSCOW -> Set(NodeRgid.MOSCOW)),
      Iterable(
        UrlsGenerationRule(
          TypeSpecification.offers(Some(offer.OfferType.SELL)),
          CategoryDeclarations.JustCategory(offer.CategoryType.APARTMENT),
          Map(
            ClassifiedPartWithExtraParams(ClassifiedPart.Empty) -> Seq(findFilter("ekonom-klass")),
            ClassifiedPartWithExtraParams(ClassifiedPart.Rooms(RoomsTotal(RoomsValue.Studio))) -> Seq(
              findFilter("ekonom-klass")
            )
          )
        )
      )
    ) {
      val expectedKeys = Seq(
        Request.Raw(
          RequestType.Search,
          Seq(Rgid(NodeRgid.MOSCOW), Type(offer.OfferType.SELL), Category(offer.CategoryType.APARTMENT))
        ),
        Request.Raw(
          RequestType.Search,
          Seq(
            Rgid(NodeRgid.MOSCOW),
            Type(offer.OfferType.SELL),
            Category(offer.CategoryType.APARTMENT),
            RoomsTotal(RoomsValue.Studio)
          )
        ),
        Request.Raw(
          RequestType.Search,
          Seq(
            Rgid(NodeRgid.MOSCOW),
            Type(offer.OfferType.SELL),
            Category(offer.CategoryType.APARTMENT),
            RoomsTotal(RoomsValue.Studio, RoomsValue.OneRoom),
            NewFlat(false)
          )
        ),
        Request.Raw(
          RequestType.Search,
          Seq(
            Rgid(NodeRgid.MOSCOW),
            Type(offer.OfferType.SELL),
            Category(offer.CategoryType.APARTMENT),
            RoomsTotal(RoomsValue.Studio, RoomsValue.OneRoom),
            NewFlat(false)
          )
        )
      ).map(_.toString)

      SourceUrlsExtractor
        .extractUrls(BySourceUrlsRequest.ByOfferRequest(makeOffer(NodeRgid.MOSCOW)))
        .map(_.map(_.urlPath))
        .map(assert(_)(forall(containsIn(expectedKeys.toSet))))
    },
    extractSpec(
      "return all urls with posutochno",
      Map(NodeRgid.MOSCOW -> Set(NodeRgid.MOSCOW)),
      Iterable(
        UrlsGenerationRule(
          TypeSpecification.offers(Some(offer.OfferType.RENT)),
          CategoryDeclarations.JustCategory(offer.CategoryType.APARTMENT),
          Map(
            ClassifiedPartWithExtraParams(ClassifiedPart.Empty) -> Seq(
              findFilter("posutochno"),
              findFilter("s-parkom"),
              findFilter("s-mebeliu")
            )
          )
        )
      )
    ) {

      val expected = Seq(
        Seq("posutochno"),
        Seq("posutochno", "s-parkom"),
        Seq("posutochno", "s-mebeliu")
      ).map(_.map(findFilter))
        .map { filters =>
          Request.Raw(
            RequestType.Search,
            Seq(
              Rgid(NodeRgid.MOSCOW),
              Category(offer.CategoryType.APARTMENT),
              Type(offer.OfferType.RENT)
            ) ++ filters.flatMap(_.parameters)
          )
        }
        .map(_.toString)
        .toSet

      SourceUrlsExtractor
        .extractUrls(BySourceUrlsRequest.ByOfferRequest(makeOffer(NodeRgid.MOSCOW, isShortRent = true)))
        .map(_.map(_.urlPath))
        .map(assert(_)(forall(containsIn(expected))))
    }
  )

  private def extractSpec(name: String, regionExtractor: Map[Long, Set[Long]], rules: Iterable[UrlsGenerationRule])(
    spec: => ZIO[SourceUrlsExtractor, SourceUrlsExtractor.Error, TestResult]
  ) = {
    testM(name) {
      spec
        .mapError {
          case Error.CouldNotCreateOfferRequest(id) => new RuntimeException(s"Missing create $id")
          case Error.RouterError(exception) => exception
          case Error.UnrecognizedError(exception) => exception
        }
        .provideLayer(
          ZLayer.wire[SourceUrlsExtractor](
            if (rules.isEmpty) TextRenderMock.empty else TextRenderMock.mock,
            if (rules.isEmpty) FrontendRouterMock.empty else FrontendRouterMock.mock,
            ZLayer.succeed[RequestRegionExtractor.Service](StubRequestRegionExtractor(regionExtractor)),
            ZLayer.succeed[GenerationRulesExtractor.Service](StubGenerationRulesExtractor(rules)),
            RequestsInfoExtractor.byRouterFiltersLive,
            ByFiltersRequestWithTextGenerator.live,
            ZLayer.fromService[RequestWithTextGenerator.Service, Seq[RequestWithTextGenerator.Service]](Seq(_)),
            TestData.regionServiceLayer,
            SourceUrlsExtractor.live,
            ZLayer.succeed(TestData.filtersProvider),
            FilterCombinationsSelection.live
          )
        )

    }
  }
}

object SourceUrlsExtractorSpecData {

  def containsIn[A](values: Set[A]): Assertion[A] =
    Assertion.assertion(s"containsIn")(Render.param(values))(x => values.contains(x))

  @mockable[TextRender.Service]
  object TextRenderMock {

    val mock: Expectation[TextRender] =
      TextRenderMock
        .Render(anything, value("mocked text"))
        .atLeast(-1)
  }

  @mockable[FrontendRouter.Service]
  object FrontendRouterMock {
    private def requestsToKeys(requests: Iterable[RouterUrlRequest]): Iterable[RouterUrlResponse] =
      requests.map(req => RouterUrlResponse(req, Some(req.req.toString)))

    val mock: Expectation[FrontendRouter] =
      FrontendRouterMock
        .Translate(forall(anything), valueF(requestsToKeys))
        .atLeast(-1)
  }

  case class StubRequestRegionExtractor(resultByRgids: Map[Long, Set[Long]]) extends RequestRegionExtractor.Service {
    override def getRegionsByClassifiedPart(
      sourceRgid: Long,
      classifiedPart: ClassifiedPart,
      siteId: Option[Long]
    ): Task[Set[Long]] =
      if (resultByRgids.contains(sourceRgid)) Task.succeed(resultByRgids(sourceRgid))
      else Task.fail(new IllegalArgumentException(s"Bad spec! Result for rgid $sourceRgid not set."))

    override def getForSite(siteId: Long): IO[Option[Nothing], Long] = IO.fail(None)
  }

  case class StubGenerationRulesExtractor(rules: Iterable[UrlsGenerationRule])
    extends GenerationRulesExtractor.Service {
    override def extractRules(
      request: BySourceUrlsRequest,
      filters: ListingFilters
    ): Task[Iterable[UrlsGenerationRule]] =
      Task.succeed(rules)

    override def enrichRule(rule: UrlsGenerationRule): IO[Option[Nothing], UrlsGenerationRule] = UIO(rule)
  }

}
