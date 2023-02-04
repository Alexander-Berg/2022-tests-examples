package ru.yandex.vertis.moderation.service

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.DetailedReason
import ru.yandex.vertis.moderation.model.meta.Metadata
import ru.yandex.vertis.moderation.model.signal.SignalSet
import ru.yandex.vertis.moderation.service.QuotaMetadataTransformerSpec.{QuotaInfo, TestCase}
import ru.yandex.vertis.moderation.service.impl.transformer.QuotaMetadataTransformer
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer._
import ru.yandex.vertis.moderation.model.signal.HoboSignal.Result
import ru.yandex.vertis.moderation.proto.Model.Reason

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class QuotaMetadataTransformerSpec extends SpecBase {

  private val transformer: InstanceTransformer = QuotaMetadataTransformer

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "return isQuota=true if signals contain active UserReseller signal",
        signals =
          SignalSet(
            BanSignalGen
              .suchThat(_.switchOff.isEmpty)
              .next
              .copy(detailedReason = DetailedReason.UserReseller(None, Seq.empty))
          ),
        expectedQuotaInfo =
          QuotaInfo(
            isQuota = true,
            isReturnedQuota = false,
            isOverQuota = false,
            isReturnedOverQuota = false
          )
      ),
      TestCase(
        description = "return isReturnedQuota=true if signals contain only switched off UserReseller signal",
        signals =
          SignalSet(
            WarnSignalGen
              .suchThat(_.switchOff.isDefined)
              .next
              .copy(detailedReason = DetailedReason.UserReseller(None, Seq.empty))
          ),
        expectedQuotaInfo =
          QuotaInfo(
            isQuota = false,
            isReturnedQuota = true,
            isOverQuota = false,
            isReturnedOverQuota = false
          )
      ),
      TestCase(
        description = "return isOverQuota=true if signals contain active OverQuota signal",
        signals =
          SignalSet(
            HoboSignalGen
              .suchThat(_.switchOff.isEmpty)
              .next
              .copy(
                result = Result.Warn(Set(DetailedReason.OverQuota), None)
              )
          ),
        expectedQuotaInfo =
          QuotaInfo(
            isQuota = false,
            isReturnedQuota = false,
            isOverQuota = true,
            isReturnedOverQuota = false
          )
      ),
      TestCase(
        description = "return isReturnedOverQuota=true if signals contain only switched off OverQuota signal",
        signals =
          SignalSet(
            HoboSignalGen
              .suchThat(_.switchOff.isDefined)
              .next
              .copy(
                result = Result.Bad(Set(DetailedReason.OverQuota), None)
              )
          ),
        expectedQuotaInfo =
          QuotaInfo(
            isQuota = false,
            isReturnedQuota = false,
            isOverQuota = false,
            isReturnedOverQuota = true
          )
      ),
      TestCase(
        description = "return all flags = false if there are no UserReseller or OverQuota signals",
        signals =
          SignalSetGen
            .suchThat(
              _.flatMap(_.getDetailedReasons)
                .map(_.reason)
                .toSet
                .intersect(Set(Reason.USER_RESELLER, Reason.OVER_QUOTA))
                .isEmpty
            )
            .next,
        expectedQuotaInfo =
          QuotaInfo(
            isQuota = false,
            isReturnedQuota = false,
            isOverQuota = false,
            isReturnedOverQuota = false
          )
      )
    )

  "QuotaMetadataTransformer" should {
    testCases.foreach { case TestCase(description, signals, expectedQuotaInfo) =>
      description in {
        val instance = InstanceGen.next.copy(signals = signals)
        val actualQuotaMeta = transformer.transform(instance).metadata.get[Metadata.Quota]
        actualQuotaMeta.map(QuotaInfo.apply) shouldBe Some(expectedQuotaInfo)
      }
    }
  }
}

object QuotaMetadataTransformerSpec {

  private case class TestCase(description: String, signals: SignalSet, expectedQuotaInfo: QuotaInfo)

  private case class QuotaInfo(isQuota: Boolean,
                               isReturnedQuota: Boolean,
                               isOverQuota: Boolean,
                               isReturnedOverQuota: Boolean
                              )

  private object QuotaInfo {
    def apply(meta: Metadata.Quota): QuotaInfo =
      QuotaInfo(
        isQuota = meta.isQuota,
        isReturnedQuota = meta.isReturnedQuota,
        isOverQuota = meta.isOverQuota,
        isReturnedOverQuota = meta.isReturnedOverQuota
      )
  }

}
