package ru.yandex.realty.directives

import akka.http.scaladsl.server.directives.RouteDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.akka.http.PlayJsonSupport

@RunWith(classOf[JUnitRunner])
class PhoneDirectivesSpec
  extends SpecBase
  with PhoneDirectives
  with Matchers
  with ScalatestRouteTest
  with RouteDirectives
  with PlayJsonSupport {

  "PhoneDirectives withTag" should {
    "parse tag " in {
      Get("/?tag=site_1234") ~> withPhoneTag(v => complete(v)) ~> check {
        responseAs[Option[String]] shouldEqual Some("site_1234")
      }
    }

    "parse tag with no tag present" in {
      Get("/?updateCalls=YES") ~> withPhoneTag(v => complete(v)) ~> check {
        responseAs[Option[String]] shouldEqual None
      }
    }

  }

  "PhoneDirectives withPhoneTagInfo" should {
    "parse tag info" in {
      Get("/?tagInfo=newData%3Dval") ~> withPhoneTagInfo(v => complete(v)) ~> check {
        responseAs[Set[String]] shouldEqual Set("newData=val")
      }
    }

    "parse tag info with several occurences" in {
      Get("/?tagInfo=adid%3Djeghje34jb5h43b&tagInfo=meta%3Dasad") ~> withPhoneTagInfo(v => complete(v)) ~> check {
        responseAs[Set[String]] shouldEqual Set("adid=jeghje34jb5h43b", "meta=asad")
      }
    }

    "parse tag info with no tagInfo present" in {
      Get("/?tag=someTT&updateCalls=YES") ~> withPhoneTagInfo(v => complete(v)) ~> check {
        responseAs[Set[String]] shouldEqual Set.empty
      }
    }

  }

}
