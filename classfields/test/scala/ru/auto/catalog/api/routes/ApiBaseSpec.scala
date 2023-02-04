package ru.auto.catalog.api.routes

import akka.http.scaladsl.server.Directives.{extractRequestContext, pathPrefix}
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Suite, SuiteMixin}
import ru.auto.catalog.BaseSpec
import ru.auto.catalog.api.handlers.ApiHandler
import ru.auto.catalog.api.handlers.human_info.HumanInfoHandler
import ru.auto.catalog.api.handlers.options.OptionsHandler
import ru.auto.catalog.api.handlers.preset_groups.PresetGroupsHandler
import ru.auto.catalog.api.handlers.raw.RawCatalogHandler
import ru.auto.catalog.core.managers._
import ru.auto.catalog.core.managers.tech_info.HumanInfoManager
import ru.auto.catalog.core.model.counters.{OfferCountersStatHolder, ReviewCounterStatHolder}
import ru.auto.catalog.core.model.preset_groups.PresetGroupsHolder
import ru.auto.catalog.core.model.raw.cars.CarsCatalogWrapper
import ru.auto.catalog.core.model.raw.moto.MotoCatalogWrapper
import ru.auto.catalog.core.model.raw.trucks.TrucksCatalogWrapper
import ru.auto.catalog.core.model.verba.VerbaCars
import ru.auto.catalog.core.testkit._
import ru.yandex.vertis.baker.util.api.directives.RequestDirectives
import ru.yandex.vertis.baker.util.api.routes.features.FeaturesHandler
import ru.yandex.vertis.baker.util.api.routes.{ApiExceptionHandler, ApiRejectionHandler}
import ru.yandex.vertis.baker.util.extdata.geo.RegionTree
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, CompositeFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport

import scala.concurrent.Future

trait ApiBaseSpec
  extends BaseSpec
  with SuiteMixin
  with ScalatestRouteTest
  with MockitoSupport
  with RequestDirectives
  with ProtobufSupport {
  suite: Suite =>

  // noinspection ScalaStyle
  implicit protected val td: TildeArrow[RequestContext, Future[RouteResult]] {
    type Out = RouteTestResult
  } = TildeArrow.injectIntoRoute

  lazy val featureRegistry = new InMemoryFeatureRegistry(
    new CompositeFeatureTypes(Seq(BasicFeatureTypes))
  )

  val AddNameplateToTechParamHumanName: Feature[Boolean] =
    featureRegistry.register("add_nameplate_to_tech_param_human_name", true)

  lazy val carsCatalog: CarsCatalogWrapper = TestCardCatalogWrapper
  lazy val trucksCatalog: TrucksCatalogWrapper = TestTruckCardCatalogWrapper
  lazy val motoCatalog: MotoCatalogWrapper = TestMotoCardCatalogWrapper
  lazy val carsVerba: VerbaCars = VerbaCars.from(TestDataEngine)
  lazy val presetGroupsHolder: PresetGroupsHolder = PresetGroupsHolder.from(TestDataEngine)

  lazy val offerCountersHolder: OfferCountersStatHolder =
    OfferCountersStatHolder.from(TestDataEngine)

  lazy val reviewCountersHolder: ReviewCounterStatHolder =
    ReviewCounterStatHolder.from(TestDataEngine)
  lazy val regionTree: RegionTree = RegionTree.from(TestDataEngine)

  lazy val countersEnricher =
    new CountersEnricher(offerCountersHolder, reviewCountersHolder, regionTree)

  lazy val presetGroupsManager = new PresetGroupsManager(presetGroupsHolder)

  lazy val verbaCarsManager = new VerbaCarsManager(carsVerba)

  lazy val rawCatalogManager: RawCatalogManager =
    new RawCatalogManager(
      carsCatalog,
      motoCatalog,
      trucksCatalog,
      verbaCarsManager,
      countersEnricher
    )

  lazy val humanInfoManager: HumanInfoManager =
    new HumanInfoManager(carsCatalog, carsVerba, regionTree)

  lazy val optionsManager: OptionsManager =
    new OptionsManager(carsCatalog)

  val rawCatalogHandler: RawCatalogHandler = new RawCatalogHandler(rawCatalogManager)
  val humanInfoHandler: HumanInfoHandler = new HumanInfoHandler(humanInfoManager)
  val presetGroupsHandler: PresetGroupsHandler = new PresetGroupsHandler(presetGroupsManager)
  val featureHandler: FeaturesHandler = new FeaturesHandler(featureRegistry)
  val optionsHandler: OptionsHandler = new OptionsHandler(optionsManager)

  val apiHandler: ApiHandler =
    new ApiHandler(rawCatalogHandler, humanInfoHandler, presetGroupsHandler, featureHandler, optionsHandler)

  private def sealRoute(r: Route): Route =
    extractRequestContext { ctx =>
      Route.seal(r)(
        ctx.settings,
        ctx.parserSettings,
        ApiRejectionHandler.handler,
        ApiExceptionHandler.handler
      )
    }

  val route: Route = wrapRequest {
    pathPrefix("api") {
      sealRoute {
        apiHandler.route
      }
    }
  }
}
