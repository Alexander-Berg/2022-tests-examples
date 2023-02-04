package ru.auto.cabinet.service

import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import ru.auto.api.ApiOfferModel.{Offer, SellerType}
import ru.auto.api.ResponseModel.OfferResponse
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}
import ru.auto.cabinet.service.public_api.HttpPublicApiClient
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.Mockito.when
import ru.auto.cabinet.trace.Context

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OfferServiceSpec
    extends FlatSpec
    with Matchers
    with PropertyChecks
    with ScalaFutures {
  implicit private val rc = Context.unknown
  private val offerIdFound = "123-abc"
  private val publicApi = mock[HttpPublicApiClient]

  implicit private val instr: Instr = new EmptyInstr("test")

  private val service = new OfferService(publicApi)

  private val dealerOfferResponse: OfferResponse = OfferResponse
    .newBuilder()
    .setOffer(Offer.newBuilder().setSellerType(SellerType.COMMERCIAL).build())
    .build()

  private val privateOfferResponse: OfferResponse = OfferResponse
    .newBuilder()
    .setOffer(Offer.newBuilder().setSellerType(SellerType.PRIVATE).build())
    .build()

  it should "return dealer offer" in {
    when(publicApi.getOffer(offerIdFound, "cars", None))
      .thenReturn(Future.successful(dealerOfferResponse))

    service
      .getOfferInfo(offerIdFound, "cars")
      .futureValue shouldBe dealerOfferResponse
  }

  it should "return private offer" in {
    when(publicApi.getOffer(offerIdFound, "cars", None))
      .thenReturn(Future.successful(privateOfferResponse))

    service
      .getOfferInfo(offerIdFound, "cars")
      .futureValue shouldBe privateOfferResponse
  }
}
