package ru.yandex.vertis.billing.event

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.PaidOffer
import ru.yandex.vertis.billing.settings.AutoRuTasksServiceComponents
import ru.yandex.vertis.billing.util.DateTimeInterval

import scala.collection._

/**
  * Tests for [[UnsafePaidOfferRecordsReader]]
  *
  * @author alesavin
  */
class UnsafePaidOfferRecordsReaderSpec extends AnyWordSpec with Matchers with EventsProviders {

  private val PaidOffersRecordsReader =
    new HandleTryReader(
      new UnsafePaidOfferRecordsReader(
        randomEventsReader(100),
        AutoRuTasksServiceComponents.getOfferId
      )
    )

  "PaidOffersRecordsReader" should {
    "get PaidOfferRecords" in {
      val records = mutable.ListBuffer[Iterable[PaidOffer]]()
      PaidOffersRecordsReader.read(DateTimeInterval.currentDay)(accumulate(records))
      records.flatten.size should be > 0
    }
  }
}
