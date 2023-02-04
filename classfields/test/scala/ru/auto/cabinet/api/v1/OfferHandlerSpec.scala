package ru.auto.cabinet.api.v1

import akka.http.scaladsl.model.ContentType.Binary
import akka.http.scaladsl.model.MediaTypes
import org.mockito.Mockito._
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatestplus.mockito.MockitoSugar.mock
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.ResponseModel.OfferResponse
import ru.auto.cabinet.api.v1.offer.OfferHandler
import ru.auto.cabinet.reporting.pdf.PdfGenSettings
import ru.auto.cabinet.service.OfferService

import scala.concurrent.Future
import scala.concurrent.duration._

class OfferHandlerSpec extends FlatSpec with HandlerSpecTemplate {

  private val offerIdFound = "123-abc"
  private val offerService = mock[OfferService]
  private val pdfBytes = Array.empty[Byte]

  private val route =
    wrapRequestMock {
      new OfferHandler(offerService, PdfGenSettings("", 1.second)) {

        override def generateCarPricePdf(
            offerResponse: OfferResponse): Future[Array[Byte]] = {
          Future.successful(pdfBytes)
        }

      }.route
    }

  private val offerResponse: OfferResponse = OfferResponse
    .newBuilder()
    .setOffer(Offer.newBuilder().build())
    .build()

  when(offerService.getOfferInfo(eq(offerIdFound), eq("cars"), eq(None))(any()))
    .thenReturn(Future.successful(offerResponse))

  it should "return PDF for offer" in {
    Get(s"/offer/$offerIdFound/cars/price-pdf") ~> route ~> check {
      contentType shouldBe Binary(MediaTypes.`application/pdf`)
    }
  }
}
