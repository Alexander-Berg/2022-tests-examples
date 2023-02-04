package ru.yandex.realty.componenttest.env

import ru.yandex.realty.application.ng.{AppConfigProvider, DefaultTypesafeConfigProvider}
import ru.yandex.realty.componenttest.env.config.ComponentTestConfigProvider
import ru.yandex.realty.componenttest.env.initializers.{
  ComponentTestApiPortInitializer,
  ComponentTestExtdataInitializer,
  ComponentTestUsernameInitializer
}
import ru.yandex.realty.componenttest.http.DefaultExternalHttpStubConfigProvider
import ru.yandex.realty.componenttest.utils.RandomPortProvider
import ru.yandex.realty.componenttest.wiremock.{DefaultWireMockProvider, WireMockExternalHttpStub}

trait ComponentTestEnvironment[T] {

  def component: T

}

trait DefaultComponentTestEnvironment[TComponent, TConfig]
  extends ComponentTestEnvironment[TComponent]
  with ComponentTestConfigProvider[TConfig]
  with ComponentTestApiPortInitializer
  with ComponentTestUsernameInitializer
  with DefaultExternalHttpStubConfigProvider
  with DefaultWireMockProvider
  with WireMockExternalHttpStub
  with AppConfigProvider
  with DefaultTypesafeConfigProvider
  with RandomPortProvider {

  protected def buildComponent(): TComponent

  override lazy val component: TComponent = buildComponent()

}

trait DefaultExtdataComponentTestEnvironment[TComponent, TConfig]
  extends DefaultComponentTestEnvironment[TComponent, TConfig]
  with ComponentTestExtdataInitializer
