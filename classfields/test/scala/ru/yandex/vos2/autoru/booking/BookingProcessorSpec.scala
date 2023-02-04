package ru.yandex.vos2.autoru.booking

import org.mockito.Mockito.{times, verify}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.Suite
import ru.auto.booking.broker.BrokerModel
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.autoru.booking.impl.BookingProcessorImpl
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferDao

import scala.concurrent.ExecutionContext.Implicits.global

class BookingProcessorSpec extends AnyWordSpec with Matchers with Suite with ScalaFutures with MockitoSupport {

  private val mockedAutoruOfferDao = mock[AutoruOfferDao]
  when(mockedAutoruOfferDao.useOfferID(?, ?, ?)(?)(?)).thenReturn(true)

  private val mockedBookingDecider = mock[BookingDecider]
  private val bookingProcessor = new BookingProcessorImpl(() => mockedAutoruOfferDao, mockedBookingDecider)

  "BookingProcessorImpl" should {
    "mockedAutoruOfferDao.useOfferID invocation" in {
      val data = {
        val builder = BrokerModel.BookingChangeEvent.newBuilder
        builder.getCurrentStateBuilder.getOfferBuilder.setId("1221-ba4f")
        builder.build
      }
      bookingProcessor.process(data).futureValue
      verify(mockedAutoruOfferDao, times(1)).useOfferID(?, ?, ?)(?)(?)
    }
  }
}
