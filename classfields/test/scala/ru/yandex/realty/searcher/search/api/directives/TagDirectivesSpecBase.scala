package ru.yandex.realty.searcher.search.api.directives

import akka.http.scaladsl.server.directives.RouteDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.akka.http.PlayJsonSupport
import ru.yandex.realty.searcher.api.directives.TagDirectives

/**
  * @author nstaroverova
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class TagDirectivesSpecBase
  extends FlatSpec
  with Matchers
  with TagDirectives
  with ScalatestRouteTest
  with RouteDirectives
  with PlayJsonSupport {

  "Directive withTagIds " should " parse list of id (Long type)" in {
    Get("/?id=1&id=2&id=3") ~> withTagIds(v => complete(v)) ~> check {
      responseAs[Seq[Long]] shouldEqual Seq(1, 2, 3)
    }
  }

  it should "not be handled if id does not exists in parameter list" in {
    Get("/?category=ROOMS&text=floor") ~> withTagIds(v => complete(v)) ~> check {
      handled shouldEqual false
    }
  }

  it should "not be handled if id is not a number" in {
    Get("/?id=1&id=something") ~> withTagIds(v => complete(v)) ~> check {
      handled shouldEqual false
    }
  }
}
