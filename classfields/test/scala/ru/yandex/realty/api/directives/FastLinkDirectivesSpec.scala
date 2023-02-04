package ru.yandex.realty.api.directives

import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.RouteDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.directives.FastLinkDirectives
import ru.yandex.realty.urls.router.model.ViewType

@RunWith(classOf[JUnitRunner])
class FastLinkDirectivesSpec extends SpecBase with ScalatestRouteTest with RouteDirectives {

  private val dirs = new FastLinkDirectives {}

  override def testConfig: Config = ConfigFactory.empty()

  "FastLinkDirectives" should {

    "pass valid view type" in {
      Seq(
        ViewType.Desktop,
        ViewType.TouchPhone
      ).foreach { vt =>
        Get(s"/?viewType=${vt.entryName}") ~> dirs.viewType(v => complete(v.entryName)) ~> check {
          responseAs[String] shouldEqual vt.entryName
        }
      }
    }

    "reject invalid view type" in {
      Get(s"/?viewType=mobile") ~> dirs.viewType(v => complete(v.entryName)) ~> check {
        rejection shouldBe a[ValidationRejection]
      }
    }
  }

}
