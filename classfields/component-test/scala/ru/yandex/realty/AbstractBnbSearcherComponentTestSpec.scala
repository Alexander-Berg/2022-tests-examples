package ru.yandex.realty

import _root_.akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import ai.x.play.json.Jsonx
import play.api.libs.json.{Format, JsBoolean, JsError, JsResult, JsString, JsSuccess, JsValue, OFormat}
import ru.yandex.realty.akka.http.PlayJsonSupport
import ru.yandex.realty.componenttest.env.ComponentTestEnvironmentProvider
import ru.yandex.realty.componenttest.spec.{HttpComponentTestSpec, WireMockComponentTestSpec}
import ru.yandex.realty.componenttest.telepony.TeleponyHttpStub
import ru.yandex.realty.experiments.AllSupportedExperiments
import ru.yandex.realty.model.serialization.json.EnumJsonFormat
import ru.yandex.realty.render.search.RenderableSiteSnippet
import ru.yandex.realty.render.search.query.RenderableSearchQuery
import ru.yandex.realty.serialization.PlayJsonFormats

abstract class AbstractBnbSearcherComponentTestSpec
  extends ComponentTestEnvironmentProvider[BnbSearcherEnvironment.type]
  with HttpComponentTestSpec[BnbSearcherEnvironment.type]
  with WireMockComponentTestSpec[BnbSearcherEnvironment.type]
  with BnbSearcherComponentTestJsonFormats
  with TeleponyHttpStub {

  override lazy val env: BnbSearcherEnvironment.type = BnbSearcherEnvironment

  protected lazy val component: BnbSearcherComponent = env.component

  override lazy val routeUnderTest: Route = component.route

  override lazy val exceptionHandler: ExceptionHandler = component.exceptionHandler
  override lazy val rejectionHandler: RejectionHandler = component.rejectionHandler

  implicit lazy val excHandler: ExceptionHandler = exceptionHandler
  implicit lazy val rejHandler: RejectionHandler = rejectionHandler

  env

}

trait BnbSearcherComponentTestJsonFormats extends PlayJsonFormats with PlayJsonSupport {

  implicit lazy val CustomBooleanFormat: Format[Boolean] = new Format[Boolean] {
    override def reads(json: JsValue): JsResult[Boolean] = json match {
      case JsString(value) => JsSuccess(readCustomBoolean(value))
      case JsBoolean(value) => JsSuccess(value)
      case m => JsError(s"can't deserialize passage from value $m")
    }

    override def writes(o: Boolean): JsValue = JsString(writeCustomBoolean(o))
  }

  implicit override lazy val RenderableSiteSnippetFormat: OFormat[RenderableSiteSnippet] =
    Jsonx.formatCaseClassUseDefaults[RenderableSiteSnippet]

  implicit private lazy val RenderableAllSupportedExperimentsFormat: Format[AllSupportedExperiments] =
    new EnumJsonFormat[AllSupportedExperiments]

  implicit override lazy val RenderableSearchQueryFormat: OFormat[RenderableSearchQuery] =
    Jsonx.formatCaseClassUseDefaults[RenderableSearchQuery]

}
