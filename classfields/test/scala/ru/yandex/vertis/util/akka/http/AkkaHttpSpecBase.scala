package ru.yandex.vertis.util.akka.http

import akka.http.scaladsl.testkit.{RouteTest, ScalatestRouteTest}
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures

/**
  *
  * @author zvez
  */
trait AkkaHttpSpecBase
  extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with RouteTest
