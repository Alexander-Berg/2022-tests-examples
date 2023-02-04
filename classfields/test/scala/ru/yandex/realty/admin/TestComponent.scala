package ru.yandex.realty.admin

import akka.http.scaladsl.server.Route
import ru.yandex.realty.admin.application.{AdminApiConfig, AdminApiConfigProvider}
import ru.yandex.realty.admin.backend.AdminApiBuilder

object TestComponent extends AdminApiBuilder(AdminApiConfig(AdminApiConfigProvider.provide())) {

  def appRoute: Route = handler.route

}
