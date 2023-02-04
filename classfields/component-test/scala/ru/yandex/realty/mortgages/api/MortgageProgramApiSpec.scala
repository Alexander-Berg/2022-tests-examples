package ru.yandex.realty.mortgages.api

import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.mortgages.AbstractMortgagesComponentTestSpec
import ru.yandex.realty.CommonConstants.REALTY_BASE_URL
import ru.yandex.realty.componenttest.data.mortgageprograms.{MortgageProgram_2418785, MortgageProgram_2419311}
import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.mortgageProgramCanonicalUrl
import ru.yandex.realty.proto.mortgage.api.{
  MortgageProgramCalculatorResponse,
  MortgageProgramCardResponse,
  MortgageProgramListingResponse
}

import scala.collection.JavaConverters._
import scala.language.implicitConversions

@RunWith(classOf[JUnitRunner])
class MortgageProgramApiSpec extends AbstractMortgagesComponentTestSpec {

  "GET /mortgagePrograms" should {

    "return mortgage programs" in {

      Get("/mortgagePrograms") ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val response = entityAs[MortgageProgramListingResponse]
          response.getResponse.getMortgageProgramsCount should (be > 0)

          val mortgageProgram = response.getResponse.getMortgageProgramsList.asScala.head
          mortgageProgram.getUrl.getValue.nonEmpty should be(true)
        }

    }

    "return mortgage program by id" in {

      val mortgageProgram = MortgageProgram_2418785
      val expectedMortgageProgramUrl = REALTY_BASE_URL + mortgageProgramCanonicalUrl(mortgageProgram.Proto)

      Get(s"/mortgagePrograms/${mortgageProgram.Id}") ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val response = entityAs[MortgageProgramCardResponse]
          response.hasResponse should be(true)

          val mortgageProgramCard = response.getResponse
          mortgageProgramCard.getMortgageProgram.getUrl.getValue should be(expectedMortgageProgramUrl)
        }

    }

    "return mortgage program calculator with overpayment" in {

      val mortgageProgramId = MortgageProgram_2419311.Id
      val query = "propertyCost=1000000&downPaymentSum=150000&periodYears=10"
      Get(s"/mortgagePrograms/$mortgageProgramId/calculator?$query") ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val response = entityAs[MortgageProgramCalculatorResponse].getResponse
          response.getOverpayment shouldEqual 350120
        }

    }

  }

}
