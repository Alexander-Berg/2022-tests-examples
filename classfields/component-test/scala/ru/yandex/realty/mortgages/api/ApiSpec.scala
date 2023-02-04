package ru.yandex.realty.mortgages.api

import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.mortgages.AbstractMortgagesComponentTestSpec

import scala.language.implicitConversions

@RunWith(classOf[JUnitRunner])
class ApiSpec extends AbstractMortgagesComponentTestSpec {

  "GET /ping" should {

    "return OK" in {

      Get("/ping") ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }

    }

  }

}
