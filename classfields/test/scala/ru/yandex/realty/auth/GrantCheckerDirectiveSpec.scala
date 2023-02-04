package ru.yandex.realty.auth

import akka.http.scaladsl.server.directives.RouteDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.akka.http.directives.GrantCheckerDirective
import ru.yandex.realty.request.{Request, RequestImpl}

import scala.concurrent.Future

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class GrantCheckerDirectiveSpec
  extends SpecBase
  with ScalatestRouteTest
  with GrantCheckerDirective
  with RouteDirectives {

  "GrantCheckerDirective" should {
    "checkSuperUserGrant passes SuperUser" in {
      val r = new RequestImpl
      r.setAuthInfo(
        new AuthInfo(
          uidOpt = Some("1234"),
          roles = Set(Roles.MANAGEMENT_EDITOR, Roles.WALLET_EDITOR, Roles.PAYMENT_TESTER)
        )
      )

      passCase(r)
    }

    "checkSuperUserGrant passes SuperUser if one of roles is UNKNOWN" in {
      val r = new RequestImpl
      r.setAuthInfo(
        new AuthInfo(uidOpt = Some("1234"), roles = Set(Roles.MANAGEMENT_EDITOR, Roles.WALLET_EDITOR, Roles.UNKNOWN))
      )

      passCase(r)
    }

    "checkSuperUserGrant reject not SuperUser if user has only one UNKNOWN role" in {
      val r = new RequestImpl
      r.setAuthInfo(new AuthInfo(uidOpt = Some("1234"), roles = Set(Roles.UNKNOWN)))

      rejectCase(r)
    }

    "checkSuperUserGrant reject user without roles" in {
      val r = new RequestImpl
      r.setAuthInfo(new AuthInfo(uidOpt = Some("1234")))

      rejectCase(r)
    }

    def rejectCase(r: Request): Unit = {
      val route = checkSuperUserGrant(r) {
        complete("The request is received")
      }

      Get() ~> route ~> check {
        handled shouldEqual false
      }
    }

    def passCase(r: Request): Unit = {
      val route = checkSuperUserGrant(r) {
        complete("The request is received")
      }

      Get() ~> route ~> check {
        handled shouldEqual true
        responseAs[String] shouldEqual "The request is received"
      }
    }
  }

}
