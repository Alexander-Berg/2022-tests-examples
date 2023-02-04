package ru.yandex.realty.storage

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.model.SocdemSegment
import ru.yandex.realty.model.offer.OfferType
import ru.yandex.realty.model.region.Regions
import scala.jdk.CollectionConverters._

class WizardPriceRangesStorageSpec extends WordSpec with Matchers {

  private val SpbRegions = Seq(Regions.SPB, Regions.SPB_AND_LEN_OBLAST)
  private val MoscowRegions = Seq(Regions.MSK_AND_MOS_OBLAST, Regions.MOSCOW)
  private val OtherRegions = Seq(Regions.AKMOLINSKAYA_OBLAST, Regions.HABAROVSKIY_KRAY)

  val maps = Map(
    OfferType.SELL -> (WizardPriceRangesStorage.SocdemSellPriceFilters, WizardPriceRangesStorage.SocdemSellDefaultPriceFilters),
    OfferType.RENT -> (WizardPriceRangesStorage.SocdemRentPriceFilters, WizardPriceRangesStorage.SocdemRentDefaultPriceFilters)
  )

  implicit class RichRubs(float: Float) {
    def mln: Float = float * math.pow(10, 6).toFloat
  }

  "WizardPriceRangesStorage" should {
    "correctly recover" in {
      for {
        segment <- Seq(SocdemSegment.C2, SocdemSegment.C1, SocdemSegment.B2)
        tp <- Seq(OfferType.SELL, OfferType.RENT)
      } {
        val (byGeo, defaults) = maps(tp)

        for {
          regions <- Seq(MoscowRegions, SpbRegions)
          reg <- regions
        } {
          val range = byGeo(segment)(reg)
          val price = range.getFrom + Option(range.getTo).map(_ - range.getFrom).map(_ / 2).getOrElse(1000f)

          WizardPriceRangesStorage.restoreSocdem(price, reg, tp).asScala shouldBe Set(segment.id)
        }

        for {
          reg <- OtherRegions
        } {
          val range = defaults(segment)
          val price = range.getFrom + Option(range.getTo).map(_ - range.getFrom).map(_ / 2).getOrElse(1000f)

          WizardPriceRangesStorage.restoreSocdem(price, reg, tp).asScala shouldBe Set(segment.id)
        }
      }
    }

    "work in some spec" in {
      val price = 3.5f.mln

      WizardPriceRangesStorage.restoreSocdem(price, MoscowRegions.head, OfferType.SELL).asScala shouldBe empty

    }
  }

}
