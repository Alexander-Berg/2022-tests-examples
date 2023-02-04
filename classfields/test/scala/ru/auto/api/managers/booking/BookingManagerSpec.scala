package ru.auto.api.managers.booking

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.OfferOrBuilder
import ru.auto.api.BaseSpec
import ru.auto.api.managers.TestRequest
import ru.auto.api.managers.enrich.EnrichOptions
import ru.auto.api.managers.offers.EnrichedOfferLoader
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.Paging
import ru.auto.api.model.gen.BookingModelGenerators._
import ru.auto.api.services.booking.BookingClient
import ru.auto.api.util.Request
import ru.auto.booking.api.ApiModel._
import ru.yandex.vertis.mockito.MockitoSupport

class BookingManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with TestRequest {

  private val bookingClient = mock[BookingClient]
  private val offerLoader = mock[EnrichedOfferLoader]
  private val manager = new BookingManager(bookingClient, offerLoader)

  "BookingManager.getBookings" should {

    "pass dealer id in 'dealer:id' format to booking-api" in {
      forAll(OfferGen, listBookingsResponseGen) { (offer, response) =>
        reset(bookingClient, offerLoader)
        when(bookingClient.listBookings(?)(?)).thenReturnF(response)
        when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        manager.getBookings(dealerId = 16453, Paging(page = 3, pageSize = 5)).futureValue
        verify(bookingClient).listBookings(argThat[ListBookingsRequest](_.getDealerId == "dealer:16453"))(?)
      }
    }

    "enrich offers with tech params" in {
      forAll(OfferGen, listBookingsResponseGen) { (offer, response) =>
        reset(bookingClient, offerLoader)
        when(bookingClient.listBookings(?)(?)).thenReturnF(response)
        when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        manager.getBookings(dealerId = 16453, Paging(page = 3, pageSize = 5)).futureValue
        verify(offerLoader, times(response.getBookingsCount)).getOffer(
          ?,
          ?,
          argThat[(OfferOrBuilder, Request) => EnrichOptions](f => f(offer, request).techParams),
          ?,
          ?,
          ?
        )(?)
      }
    }
  }

  "BookingManager.updateBookingStatus" should {

    "pass dealer id in 'dealer:id' format to booking-api" in {
      forAll(updateBookingStatusRequestGen) { incomingRequest =>
        reset(bookingClient)
        when(bookingClient.updateBookingStatus(?)(?)).thenReturnF(())
        manager.updateBookingStatus(dealerId = 16453, entity = incomingRequest).futureValue
        verify(bookingClient).updateBookingStatus(argThat[UpdateBookingStatusRequest](_.getDealerId == "dealer:16453"))(
          ?
        )
      }
    }
  }
}
