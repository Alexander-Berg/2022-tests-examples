package ru.yandex.realty.searcher.offerstat

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.proto.RoomsType
import ru.yandex.realty.proto.search.{ApartmentPriceInfo, SiteOfferStat}
import ru.yandex.realty.search.site.offerstat.SiteOfferStatConverters.PrimarySaleStatEntryRenderer
import ru.yandex.realty.search.site.offerstat.{SiteOfferStatConverters, StatEntry, Stats}
import ru.yandex.realty.searcher.filters.offer.TurnoverOccurrencesObserver
import ru.yandex.realty.util.Mappings.MapAny

import scala.collection.JavaConverters._
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class SiteOfferStatConvertersSpec extends SpecBase with Matchers {

  "SiteOfferStatConverters" should {
    "handle stats() coalescing" when {
      "5+ rooms into 4 rooms entry" in {
        val stats = new Stats(
          byRooms = mutable.Map(
            5 -> new StatEntry(rooms = 5, floors = Set(2, 3)),
            6 -> new StatEntry(rooms = 6, floors = Set(8)),
            1 -> new StatEntry(rooms = 1, floors = Set(1)),
            4 -> new StatEntry(rooms = 4, floors = Set(9))
          )
        )

        val oneRoomsExpectedResult = apartmentRoomsEntryByRooms(1, 1)
        val fourRoomsExpectedResult = apartmentRoomsEntryByRooms(4, 2, 3, 8, 9)
        val expectedResult = oneRoomsExpectedResult ++ fourRoomsExpectedResult
        val turnoverOccurrencesObserver = mock[TurnoverOccurrencesObserver]

        val result =
          SiteOfferStatConverters
            .stats(stats, PrimarySaleStatEntryRenderer, turnoverOccurrencesObserver)
            .sortBy(_._1.getRooms.getCount)

        result shouldBe expectedResult
      }
    }
  }

  private def apartmentRoomsEntryByRooms(rooms: Int, values: Int*): Seq[(RoomsType, SiteOfferStat.StatItem)] = {
    val roomsType = RoomsType
      .newBuilder()
      .applySideEffect(_.getRoomsBuilder.setCount(rooms))
      .build()

    val statItem = SiteOfferStat.StatItem
      .newBuilder()
      .setPriceInfo(ApartmentPriceInfo.newBuilder())
      .addAllFloor(values.sorted.map(Int.box).asJava)
      .build()

    Seq(roomsType -> statItem)
  }
}
