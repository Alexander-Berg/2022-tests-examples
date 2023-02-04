package ru.auto.cabinet.api.v1

import akka.http.scaladsl.model.StatusCodes._
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.when
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatestplus.mockito.MockitoSugar.mock
import ru.auto.cabinet.api.v1.view.AgreementMarshaller._
import ru.auto.cabinet.dao.entities.AgreementStatus
import ru.auto.cabinet.dao.jdbc.{AgreementDao, UpdateResult}

import scala.concurrent.Future.successful

/** Specs [[AgreementHandler]]
  */
class AgreementHandlerSpec extends FlatSpec with HandlerSpecTemplate {

  private val auth = new SecurityMocks
  import auth._

  private val agreementDao = mock[AgreementDao]

  private val route = wrapRequestMock {
    new AgreementHandler(agreementDao).route
  }

  val agreementOfferId = 1L
  val agreed = AgreementStatus(agreementOfferId, true)
  val notagreed = AgreementStatus(agreementOfferId, false)
  val dummy = AgreementParams(agreementOfferId)

  when(agreementDao.getAgreement(anyLong)(any()))
    .thenReturn(successful(notagreed))
  when(agreementDao.getAgreement(eq(client1Id))(any()))
    .thenReturn(successful(agreed))
  when(agreementDao.addAgreement(anyLong, anyLong, anyLong)(any()))
    .thenReturn(successful(UpdateResult(1L, 1)))

  "Client Agreement API" should "get client agreement" in {

    Get(s"/client/$client1Id/offer/agreement") ~> headers1 ~> route ~> check {
      responseAs[AgreementStatus] should be(agreed)
    }
  }

  it should "get client agreement (slash-ended)" in {
    Get(s"/client/$client1Id/offer/agreement/") ~> headers1 ~> route ~> check {
      responseAs[AgreementStatus] should be(agreed)
    }
  }

  it should "get false for the random client" in {
    Get(s"/client/$client2Id/offer/agreement") ~> headers2 ~> route ~> check {
      responseAs[AgreementStatus] should be(notagreed)
    }
  }

  it should "add client agreement" in {
    Post(
      s"/client/$client1Id/offer/agreement",
      dummy) ~> headers1 ~> route ~> check {
      status should be(OK)
    }
  }

  it should "add client agreement (slash-ened)" in {
    Post(
      s"/client/$client1Id/offer/agreement/",
      dummy) ~> headers1 ~> route ~> check {
      status should be(OK)
    }
  }

}
