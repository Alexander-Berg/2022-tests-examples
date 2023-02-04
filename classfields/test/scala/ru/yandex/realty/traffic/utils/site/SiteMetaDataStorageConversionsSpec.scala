package ru.yandex.realty.traffic.utils.site

import eu.timepit.refined.api.Refined
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.traffic.model.offer.OfferRooms
import ru.yandex.realty.traffic.model.site.{SiteByRoomInfo, SiteMetaData, SiteMetaDataItem, StatItem}
import ru.yandex.realty.traffic.model.site.SiteMetaData.MoreThan2Elems
import ru.yandex.realty.traffic.model.site.StatItem.StatItemValue
import eu.timepit.refined.auto._
import org.testcontainers.shaded.org.apache.commons.io.output.ByteArrayOutputStream
import ru.yandex.realty.traffic.service.site.SiteMetaDataStorage

import java.io.ByteArrayInputStream
import java.time.Instant

@RunWith(classOf[JUnitRunner])
class SiteMetaDataStorageConversionsSpec extends WordSpec with Matchers {

  private val now = Instant.ofEpochMilli(Instant.now().toEpochMilli)

  private val ItemStub =
    StatItemValue(
      1000L,
      1000L,
      1000
    )

  private def byRoomInfoStub(item: StatItem) =
    SiteByRoomInfo(
      item,
      "/site/mainpage/",
      "/site/listing/"
    )

  private val MetaDataStub: SiteMetaData = {
    SiteMetaData(
      Map(
        OfferRooms._1 -> byRoomInfoStub(StatItem.PrimaryStat(ItemStub)),
        OfferRooms._2 -> byRoomInfoStub(StatItem.SecondaryStat(ItemStub)),
        OfferRooms._3 -> byRoomInfoStub(StatItem.AllStat(ItemStub, ItemStub))
      ),
      isPaid = true,
      "main-photo",
      Refined.unsafeApply[Seq[String], MoreThan2Elems](Seq("1", "2", "3")),
      "table-title",
      "description",
      1,
      1,
      byRoomInfoStub {
        StatItem.AllStat(
          StatItemValue(
            1000L,
            2000L,
            1000
          ),
          StatItemValue(
            1000L,
            2000L,
            1000
          )
        )
      },
      SiteMetaData.Stat(now, 1, 1)
    )
  }

  "SiteMetaDataStorageConversions" should {
    "correctly convert" in {
      val storage =
        new SiteMetaDataStorage.Live(
          Map(
            1L -> SiteMetaDataItem(Some(MetaDataStub), 1L),
            2L -> SiteMetaDataItem(None, 2L)
          )
        )

      val os = new ByteArrayOutputStream()
      SiteMetaDataStorageConversions.writeToOutputStream(storage)(os)
      os.close()

      val is = new ByteArrayInputStream(os.toByteArray)
      val read = SiteMetaDataStorageConversions.fromInputStream(is)
      is.close()

      read.asMap should contain theSameElementsAs storage.asMap
      read.getForSite(2L) shouldBe defined
    }
  }
}
