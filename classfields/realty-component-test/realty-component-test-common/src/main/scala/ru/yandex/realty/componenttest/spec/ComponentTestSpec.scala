package ru.yandex.realty.componenttest.spec

import akka.http.scaladsl.server.directives.BasicDirectives
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.componenttest.env.{ComponentTestEnvironment, ComponentTestEnvironmentProvider}
import ru.yandex.realty.model.user.UserRefGenerators

trait ComponentTestSpec[T <: ComponentTestEnvironment[_]]
  extends ComponentTestEnvironmentProvider[T]
  with SpecBase
  with PropertyChecks
  with UserRefGenerators
  with BasicDirectives {

  env

}

trait AsyncComponentTestSpec[T <: ComponentTestEnvironment[_]] extends ComponentTestSpec[T] with AsyncSpecBase
