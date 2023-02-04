package ru.yandex.vertis.moderation.consumer.builder.request.impl

import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.words.ResultOfATypeInvocation
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.client.VosAutoruClient
import ru.yandex.vertis.moderation.consumer.VinConsumer.ModerationRequestAccumulator
import ru.yandex.vertis.moderation.consumer.builder.request.ModerationRequestBuilder
import ru.yandex.vertis.moderation.consumer.builder.request.impl.CompositeModerationRequestBuilderSpec.{
  BuildersTestCase,
  FlakyRequestsTestCase,
  MergeAppendSignalsTestCase
}
import ru.yandex.vertis.moderation.dao.InstanceDao
import ru.yandex.vertis.moderation.dao.impl.inmemory.{InMemoryInstanceDao, InMemoryStorageImpl}
import ru.yandex.vertis.moderation.feature.EmptyFeatureRegistry
import ru.yandex.vertis.moderation.httpclient.clustering.ClusteringClient
import ru.yandex.vertis.moderation.httpclient.vin.VinDecoderClient
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain, ModerationRequest}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  AppendSignalsRequestGen,
  ExternalIdGen,
  RemoveSignalsRequestGen,
  UpdateJournalRecordGen
}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.Diff.Autoru
import ru.yandex.vertis.moderation.model.instance.UpdateJournalRecord
import ru.yandex.vertis.moderation.model.signal.{AutomaticSource, SignalInfoSet, WarnSignalSource}
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Diff.Autoru.Value._
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.scheduler.task.vin.{
  VinAutocodeDecider,
  VinDuplicateDecider,
  VinHistoryDecider,
  VinResolutionDecider
}
import ru.yandex.vertis.moderation.util.DateTimeUtil

import scala.concurrent.Future
import scala.reflect.ClassTag

@RunWith(classOf[JUnitRunner])
class CompositeModerationRequestBuilderSpec extends SpecBase {

  implicit val materializer: Materializer =
    ActorMaterializer(
      ActorMaterializerSettings(actorSystem)
        .withSupervisionStrategy(_ => Supervision.Resume)
    )(actorSystem)

  private def warnSignalSource(info: String, reason: DetailedReason, name: String) =
    WarnSignalSource(
      domain = Domain.default(Service.AUTORU),
      source = AutomaticSource(Application.MODERATION, tag = Some(name)),
      info = Some(info),
      detailedReason = reason,
      weight = 1.0,
      ttl = None,
      timestamp = None,
      outerComment = None,
      auxInfo = SignalInfoSet.Empty
    )

  import scala.concurrent.ExecutionContext.Implicits.global

  val now = DateTimeUtil.now

  implicit val featureRegistry: FeatureRegistry = EmptyFeatureRegistry
  val vinAutocodeDecider: VinAutocodeDecider = VinAutocodeDecider.forService(Service.AUTORU)
  val vinDuplicatedecider: VinDuplicateDecider = VinDuplicateDecider.forService(Service.AUTORU)
  val vinHistoryDecider: VinHistoryDecider = VinHistoryDecider.forService(Service.AUTORU)
  val vinResolutionDecider: VinResolutionDecider = VinResolutionDecider.forService(Service.AUTORU)

  private val clusteringCLient: ClusteringClient = mock[ClusteringClient]
  private val vinDecoderClient: VinDecoderClient = mock[VinDecoderClient]
  private val vosAutoruClient: VosAutoruClient = mock[VosAutoruClient]

  val instanceDao: InstanceDao[Future] = {
    val storage = new InMemoryStorageImpl
    new InMemoryInstanceDao(Service.AUTORU, storage)
  }

  val compositeModerationRequestBuilder =
    CompositeModerationRequestBuilder.forService(
      Service.AUTORU,
      featureRegistry,
      instanceDao,
      vinAutocodeDecider,
      vinDuplicatedecider,
      vinHistoryDecider,
      vinResolutionDecider,
      clusteringCLient,
      vinDecoderClient,
      vosAutoruClient
    )

  val diffAll = Autoru(Set(VIN))
  val diffHistory = Autoru(Set(MILEAGE))
  val diffDuplicate = Autoru(Set(CONTEXT))
  val diffResolution = Autoru(Set(SECTION))
  val diffAutocodeAndResolution = Autoru(Set(VIN_RESOLUTION_UPDATED))
  val diffAutocodeDuplicateResolution = Autoru(Set(SELLER_TYPE))
  val diffAutocodeHistoryResolution = Autoru(Set(MARK))

  val updAll = UpdateJournalRecordGen.next.copy(diff = diffAll)
  val updHistory = UpdateJournalRecordGen.next.copy(diff = diffHistory)
  val updDuplicate = UpdateJournalRecordGen.next.copy(diff = diffDuplicate)
  val updResolution = UpdateJournalRecordGen.next.copy(diff = diffResolution)
  val updAutocodeAndResolution = UpdateJournalRecordGen.next.copy(diff = diffAutocodeAndResolution)
  val updAutocodeDuplicateResolution = UpdateJournalRecordGen.next.copy(diff = diffAutocodeDuplicateResolution)
  val updAutocodeHistoryResolution = UpdateJournalRecordGen.next.copy(diff = diffAutocodeHistoryResolution)

  val externalId1 = ExternalIdGen.next
  val externalId2 = ExternalIdGen.next

  val warnSignalSource11 = warnSignalSource("signalSourceInfo11", DetailedReason.AutocodeMismatch, "vinAutocode")
  val warnSignalSource12 = warnSignalSource("signalSourceInfo12", DetailedReason.AutocodeMismatch, "vinResolution")
  val warnSignalSource13 =
    warnSignalSource("signalSourceInfo11;signalSourceInfo12", DetailedReason.AutocodeMismatch, "vinAutocode")

  val warnSignalSource21 = warnSignalSource("signalSourceInfo21", DetailedReason.WrongVin, "vinHistory")
  val warnSignalSource22 = warnSignalSource("signalSourceInfo22", DetailedReason.WrongVin, "vinResolution")
  val warnSignalSource23 = warnSignalSource("signalSourceInfo23", DetailedReason.AutocodeMismatch, "vinAutocode")
  val warnSignalSource24 =
    warnSignalSource("signalSourceInfo21;signalSourceInfo22", DetailedReason.WrongVin, "vinHistory")

  val appModReq11 =
    AppendSignalsRequestGen.next.copy(
      externalId = externalId1,
      signalSources = Seq(warnSignalSource11),
      timestamp = now,
      depth = 1
    )
  val appModReq12 =
    appModReq11.copy(
      signalSources = Seq(warnSignalSource12)
    )
  val appModReq13 =
    appModReq11.copy(
      signalSources = Seq(warnSignalSource13)
    )

  val appModReq21 =
    AppendSignalsRequestGen.next.copy(
      externalId = externalId2,
      signalSources = Seq(warnSignalSource21),
      timestamp = now,
      depth = 1
    )
  val appModReq22 =
    appModReq21.copy(
      signalSources = Seq(warnSignalSource22)
    )
  val appModReq23 =
    appModReq21.copy(
      signalSources = Seq(warnSignalSource23)
    )
  val appModReq24 =
    appModReq21.copy(
      signalSources = Seq(warnSignalSource24)
    )

  val remModReq11 =
    RemoveSignalsRequestGen.next.copy(
      externalId = externalId1,
      signalKeys = Set(warnSignalSource11.getKey, "random_KKKey!1"),
      timestamp = now,
      depth = 1
    )
  val remModReq12 =
    remModReq11.copy(
      signalKeys = Set("random_KKKey!1")
    )

  val remModReq21 =
    RemoveSignalsRequestGen.next.copy(
      externalId = externalId2,
      signalKeys = Set(warnSignalSource21.getKey, "random_KKKey!2"),
      timestamp = now,
      depth = 1
    )
  val remModReq22 =
    remModReq21.copy(
      signalKeys = Set("random_KKKey!2")
    )

  val buildersTestCases: Seq[BuildersTestCase] =
    Seq(
      BuildersTestCase(
        "correctly define all builders",
        updAll,
        Vector(
          a[VinAutocodeModerationRequestBuilder],
          a[VinDuplicateModerationRequestBuilder],
          a[VinHistoryModerationRequestBuilder],
          a[VinResolutionModerationRequestBuilder],
          a[WrongVinModerationRequestBuilder]
        )
      ),
      BuildersTestCase(
        "correctly define only history builder",
        updHistory,
        Vector(a[VinHistoryModerationRequestBuilder])
      ),
      BuildersTestCase(
        "correctly define only duplicate builder",
        updDuplicate,
        Vector(a[VinDuplicateModerationRequestBuilder])
      ),
      BuildersTestCase(
        "correctly define only resolution builder",
        updResolution,
        Vector(a[VinResolutionModerationRequestBuilder])
      ),
      BuildersTestCase(
        "correctly define only autocode and resolution builder",
        updAutocodeAndResolution,
        Vector(a[VinAutocodeModerationRequestBuilder], a[VinResolutionModerationRequestBuilder])
      ),
      BuildersTestCase(
        "correctly define autocode, duplicate and resolution builders",
        updAutocodeDuplicateResolution,
        Vector(
          a[VinAutocodeModerationRequestBuilder],
          a[VinDuplicateModerationRequestBuilder],
          a[VinResolutionModerationRequestBuilder]
        )
      ),
      BuildersTestCase(
        "correctly define autocode, history and resolution builders",
        updAutocodeHistoryResolution,
        Vector(
          a[VinAutocodeModerationRequestBuilder],
          a[VinHistoryModerationRequestBuilder],
          a[VinResolutionModerationRequestBuilder]
        )
      )
    )

  val mergingTestCases: Seq[MergeAppendSignalsTestCase] =
    Seq(
      MergeAppendSignalsTestCase(
        "correctly merge two append requests without losing info in case of same externalId, same signal keys",
        Seq(appModReq11, appModReq12),
        Seq(appModReq13)
      ),
      MergeAppendSignalsTestCase(
        "correctly merge two append requests without losing info and third stays the same",
        Seq(appModReq21, appModReq23, appModReq22),
        Seq(appModReq23, appModReq24)
      ),
      MergeAppendSignalsTestCase(
        "correctly merge two pair of append requests",
        Seq(appModReq21, appModReq11, appModReq12, appModReq22),
        Seq(appModReq13, appModReq24)
      )
    )

  val flakyTestCases: Seq[FlakyRequestsTestCase] =
    Seq(
      FlakyRequestsTestCase(
        "correctly get rid of flaky requests",
        ModerationRequestAccumulator(
          toRemove = Seq(remModReq11),
          toAppend = Seq(appModReq11)
        ),
        ModerationRequestAccumulator(
          toRemove = Seq(remModReq12),
          toAppend = Seq(appModReq11)
        )
      ),
      FlakyRequestsTestCase(
        "correctly get rid of flaky requests in more complicated case",
        ModerationRequestAccumulator(
          toRemove = Seq(remModReq11, remModReq21),
          toAppend = Seq(appModReq11, appModReq21)
        ),
        ModerationRequestAccumulator(
          toRemove = Seq(remModReq12, remModReq22),
          toAppend = Seq(appModReq11, appModReq21)
        )
      )
    )

  "CompositeModerationRequestBuilder for builder test cases" should {

    buildersTestCases.foreach { case BuildersTestCase(description, updateJournalRecord, expected) =>
      description in {
        val result = compositeModerationRequestBuilder.defineBuilders(updateJournalRecord).sortBy(_.toString)
        result.length shouldBe expected.length
        result.zip(expected).foreach { case (r, e) => r shouldBe e }
      }
    }
  }

  "CompositeModerationRequestBuilder for merging test cases" should {
    mergingTestCases.foreach { case MergeAppendSignalsTestCase(description, toCheck, expected) =>
      description in {
        val result = compositeModerationRequestBuilder.mergeAppendRequests(toCheck, now, 1).sortBy(_.toString)
        result shouldBe expected.sortBy(_.toString)
      }
    }
  }

  "CompositeModerationRequestBuilder for flaky test cases" should {
    flakyTestCases.foreach { case FlakyRequestsTestCase(description, toCheck, expected) =>
      description in {
        compositeModerationRequestBuilder.getRidOfFlakyRequests(toCheck) shouldBe expected
      }
    }
  }
}

object CompositeModerationRequestBuilderSpec {

  case class BuildersTestCase(description: String,
                              updateJournalRecord: UpdateJournalRecord,
                              expected: Vector[ResultOfATypeInvocation[_ <: ModerationRequestBuilder]]
                             )

  case class MergeAppendSignalsTestCase(description: String,
                                        toCheck: Seq[ModerationRequest.AppendSignals],
                                        expected: Seq[ModerationRequest.AppendSignals]
                                       )

  case class FlakyRequestsTestCase(description: String,
                                   toCheck: ModerationRequestAccumulator,
                                   expected: ModerationRequestAccumulator
                                  )

}
