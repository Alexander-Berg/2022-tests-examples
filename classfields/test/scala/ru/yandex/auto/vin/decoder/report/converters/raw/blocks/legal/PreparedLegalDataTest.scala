package ru.yandex.auto.vin.decoder.report.converters.raw.blocks.legal

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.proto.VinHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory.Status

class PreparedLegalDataTest extends AnyWordSpecLike {

  private val nbkiPledges = VinHistory.Pledge
    .newBuilder()
    .setPledgeType("")
    .setPerformanceDate(222222)
    .setInPledge(true)

  private val fnpPledges = VinHistory.Pledge
    .newBuilder()
    .setPledgeType("LEGAL")
    .setDate(222)
    .setPerformanceDate(222222)
    .setInPledge(true)

  private val confirmationTTL = java.util.Calendar.getInstance().getTimeInMillis + concurrent.duration.DAYS.toMillis(90)
  implicit private val t = Traced.empty

  private val validNbkiPledgesEvent =
    VinInfoHistory.newBuilder().setEventType(EventType.NBKI_PLEDGE).addPledges(nbkiPledges).build()

  private val validFnpPledgesEvent =
    VinInfoHistory.newBuilder().setEventType(EventType.FNP_PLEDGE).addPledges(fnpPledges).build()

  private val pledgesFromAllSources = List(validFnpPledgesEvent, validNbkiPledgesEvent)

  private val isNbkiOn = true
  private val isNbkiUpdating = false

  "PreparedLegalDataTest".can {
    "calculate pledges status" should {
      "Convert to ERROR status when all pledges are on" in {
        val data = PreparedLegalData.apply("", pledgesFromAllSources, None, None, isNbkiOn, isNbkiUpdating)
        assert(data.summaryStatus == Status.ERROR)
      }

      "Convert to ERROR status when having existing pledges only from FNP" in {
        val data = PreparedLegalData.apply("", List(validFnpPledgesEvent), None, None, isNbkiOn, isNbkiUpdating = true)
        assert(data.pledgesStatus == Status.ERROR)
      }
      "Convert to ERROR status when having existing pledges only from NBKI" in {
        val data = PreparedLegalData.apply("", List(validNbkiPledgesEvent), None, None, isNbkiOn, isNbkiUpdating)
        assert(data.pledgesStatus == Status.ERROR)
      }

      "Convert to UNKNOWN status when having no pledges and NBKI is updating" in {
        val data = PreparedLegalData.apply("", List(), None, None, isNbkiOn, isNbkiUpdating = true)
        assert(data.pledgesStatus == Status.UNKNOWN)
      }

      "Convert to OK status when having no pledges from NBKI only" in {
        val nbkiWithEmptyPledges = validNbkiPledgesEvent.toBuilder.clearPledges().build()
        val data = PreparedLegalData.apply("", List(nbkiWithEmptyPledges), None, None, isNbkiOn, isNbkiUpdating)
        assert(data.pledgesStatus == Status.OK)
      }

      "Convert to OK status when having no pledges from FNP only and NBKI not requested" in {
        val fnpWithEmptyPledges = validFnpPledgesEvent.toBuilder.clearPledges().build()
        val data = PreparedLegalData.apply("", List(fnpWithEmptyPledges), None, None, isNbkiOn, isNbkiUpdating)
        assert(data.pledgesStatus == Status.OK)
      }
    }
  }

}
