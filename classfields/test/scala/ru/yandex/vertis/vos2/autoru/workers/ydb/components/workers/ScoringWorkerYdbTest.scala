package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import java.util.concurrent.atomic.AtomicReference
import com.google.protobuf.util.Timestamps
import com.yandex.ydb.ValueProtos.Type.PrimitiveTypeId
import com.yandex.ydb.ValueProtos.{Column, ResultSet, Type}
import com.yandex.ydb.table.YdbTable.ExecuteQueryResult
import com.yandex.ydb.table.query.DataQueryResult
import com.yandex.ydb.table.values.PrimitiveValue
import org.joda.time.DateTime
import org.mockito.Mockito.{times, verify, verifyNoMoreInteractions}
import org.scalatest.PrivateMethodTester
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.scoring.ScoringModel.{HealthScoring, PriceScoring, ScoringMessage, TransparencyScoring}
import ru.auto.api.vin.VinReportModel.RawVinEssentialsReport
import ru.auto.api.vin.VinResolutionEnums.{ResolutionPart, Status}
import ru.auto.api.vin.VinResolutionModel.{ResolutionEntry, VinIndexResolution}
import ru.yandex.passport.model.api.ApiModel._
import ru.yandex.passport.model.common.CommonModel.{DomainBan, UserModerationStatus}
import ru.yandex.vertis.baker.components.workdistribution.{WorkDistributionData, WorkerToken}
import ru.yandex.vertis.baker.components.workersfactory.workers.WorkersFactory
import ru.yandex.vertis.complaints.proto.ComplaintsModel.Complaint.{Reason => ComplaintsReason}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.moderation.proto.Model.Metadata.ProvenOwnerMetadata.Verdict
import ru.yandex.vertis.moderation.proto.Model.{Reason => ModerationReason}
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport, Traced, TracingSupport}
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.scoring.ScoringWorkerYdb
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.scoring.ScoringWorkerYdb.{TransparencyRangeMax, TransparencyRangeMin}
import ru.yandex.vertis.ydb.skypper.result.{ResultSetWrapper, ResultSetWrapperImpl}
import ru.yandex.vertis.ydb.skypper.{YdbQueryExecutor, YdbWrapper}
import ru.yandex.vos2.AutoruModel.AutoruOffer.{ProvenOwnerModerationState, VinResolution}
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag, OfferStatusHistoryItem, ScoringMessageWithHistory}
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferDao
import ru.yandex.vos2.autoru.model.TestUtils.createOffer
import ru.yandex.vos2.autoru.services.clustering.ClusteringClient
import ru.yandex.vos2.autoru.services.compliance.{ComplaintWithDecision, ComplaintsClient}
import ru.yandex.vos2.autoru.services.moderation.AutoruModerationApiClient
import ru.yandex.vos2.autoru.services.vindecoder.VinDecoderClient
import ru.yandex.vos2.autoru.utils.compare.ProtoModelDiff
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.commonfeatures.VosFeatureTypes.{VosFeature, WithGeneration}
import ru.yandex.vos2.services.moderation.OFResult
import ru.yandex.vos2.services.passport.PassportClient
import ru.yandex.vos2.util.{IO, Protobuf}

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import ru.yandex.vos2.util.Protobuf

/**
  * Created by DmitrySafonov
  */
class ScoringWorkerYdbTest extends AnyWordSpec with Matchers with MockitoSupport {

  implicit val traced: Traced = Traced.empty

  private val mockedVosFeature = mock[VosFeature]

  abstract private class Fixture {
    val offer: Offer
    val mockedClustering: ClusteringClient = mock[ClusteringClient]
    val mockedPassportClient: PassportClient = mock[PassportClient]
    val mockedComplaintsClient: ComplaintsClient = mock[ComplaintsClient]
    val mockedModerationApiClient: AutoruModerationApiClient = mock[AutoruModerationApiClient]
    val mockedVinDecoderClient: VinDecoderClient = mock[VinDecoderClient]
    val mockedFeatureManager = mock[FeaturesManager]

    val scoringWorker = new ScoringWorkerYdb(
      mockedPassportClient,
      mockedModerationApiClient,
      mockedClustering,
      mockedComplaintsClient,
      mockedVinDecoderClient
    ) with YdbWorkerTestImpl {

      override def features: FeaturesManager = mockedFeatureManager

    }
  }

  val offerFromFile: Offer = {
    val res = IO.using(this.getClass.getResourceAsStream("/scoringOffer.json")) { is =>
      IO.readLines(is).foldLeft("") { (acc, el) =>
        acc + el
      }
    }
    (Protobuf.fromJson[Offer](res))
  }

  def nextCheckAnswer(dateTime: DateTime): Iterator[ResultSetWrapper] = {
    val epoch = dateTime.getMillis / 1000
    val resultSetsList = ExecuteQueryResult
      .newBuilder()
      .addResultSets(
        ResultSet
          .newBuilder()
          .addColumns(
            Column
              .newBuilder()
              .setName("next_check")
              .setType(Type.newBuilder().setTypeId(PrimitiveTypeId.TIMESTAMP))
              .build()
          )
          .addRows(
            PrimitiveValue.timestamp(epoch).toPb
          )
      )
      .build()
      .getResultSetsList
    val data = new DataQueryResult("2", resultSetsList)
    val resultSet: ResultSetWrapper = new ResultSetWrapperImpl(
      data.getResultSet(0)
    )
    Iterator(
      resultSet
    )
  }

  "Scoring Worker YDB" should {

    "Offer with clean resolution " in new Fixture {
      private val TestOfferID = "123-abc"
      val scoringMessageList = List.fill(29)(ScoringMessage.newBuilder().build())
      val scoringWithHistory = ScoringMessageWithHistory.newBuilder().addAllScoringHistory(scoringMessageList.asJava)
      val offerBuilder =
        Offer.newBuilder(offerFromFile).setOfferID(TestOfferID).setScoring(scoringWithHistory)
      offerBuilder.getOfferAutoruBuilder.clearVinResolution()
      val offer: Offer = offerBuilder.build
      when(mockedClustering.getUserCluster(?, ?)(?)).thenReturn(Seq("2"))
      val user: User = User.newBuilder().setRegistrationDate(new DateTime().toString()).build
      when(mockedPassportClient.getUser(?)(?)).thenReturn(Success(user))
      val bans = Map("test" -> DomainBan.newBuilder().addReasons(ModerationReason.DO_NOT_EXIST.name()).build)
      val moderation = UserModerationStatus
        .newBuilder()
        .putAllBans(bans.asJava)
        .build
      when(mockedPassportClient.getModeration(?)(?)).thenReturn(Some(moderation))

      val complaints = Map(
        ComplaintsReason.WRONG_ADDRESS -> Seq(
          ComplaintWithDecision("1", "1", Some("1"), "someType", "1", "1", Some("1"))
        )
      )
      when(mockedComplaintsClient.getHistory(?, ?)(?)).thenReturn(complaints)
      when(mockedModerationApiClient.getOFData(?, ?, ?)(?)).thenReturn(OFResult(None, None))
      when(mockedVinDecoderClient.getRawEssentialsReport(?)(?)).thenReturn(
        Success(
          Some(
            RawVinEssentialsReport.newBuilder().build()
          )
        )
      )

      val result = scoringWorker.process(offer, Some(Protobuf.toJson(scoringWithHistory.build())))
      val newScoring = Protobuf.fromJson[ScoringMessageWithHistory](result.nextState.get)
      assert(
        result.updateOfferFunc.isEmpty &&
          !newScoring.hasCurrentScoring &&
          newScoring.getNextScoring.getTransparencyScoring.getProvenOwnerScore == 0d &&
          newScoring.getNextScoring.getTransparencyScoring.getNoComplaintsScore == 0d
      )
      (result.nextCheck.get.getMillis shouldBe new DateTime()
        .plusHours(ScoringWorkerYdb.ScoringRetryTimeHours)
        .getMillis +- 1000)
    }

    "successful scoring with previous badvin state without carfax report" in new Fixture {
      private val TestOfferID = "123-abc"

      private val scoringMessageSeq = List.fill(29)(ScoringMessage.newBuilder().build())

      private val scoringWithHistory =
        ScoringMessageWithHistory.newBuilder().addAllScoringHistory(scoringMessageSeq.asJava)
      scoringWithHistory.getCurrentScoringBuilder.getHealthScoringBuilder.setBadVin(true)

      val user = User.newBuilder().setRegistrationDate(new DateTime().toString()).build

      val offer: Offer =
        Offer.newBuilder(offerFromFile).setOfferID(TestOfferID).setScoring(scoringWithHistory).build
      when(mockedClustering.getUserCluster(?, ?)(?)).thenReturn(Seq("2"))
      when(mockedPassportClient.getUser(?)(?)).thenReturn(Failure(new NoSuchElementException("test")))
      val bans = Map("test" -> DomainBan.newBuilder().addReasons(ModerationReason.DO_NOT_EXIST.name()).build)

      val moderation = UserModerationStatus
        .newBuilder()
        .putAllBans(bans.asJava)
        .build

      when(mockedPassportClient.getModeration(?)(?)).thenReturn(Some(moderation))

      val complaints = Map(
        ComplaintsReason.WRONG_ADDRESS -> Seq(
          ComplaintWithDecision("1", "1", Some("1"), "someType", "1", "1", Some("1"))
        )
      )
      when(mockedComplaintsClient.getHistory(?, ?)(?)).thenReturn(complaints)

      when(mockedModerationApiClient.getOFData(?, ?, ?)(?)).thenReturn(OFResult(None, None))

      when(mockedVosFeature.value).thenReturn(WithGeneration(false, 1))

      val result = scoringWorker.process(offer, Some(Protobuf.toJson(scoringWithHistory.build())))
      val newScoring = Protobuf.fromJson[ScoringMessageWithHistory](result.nextState.get)
      assert(
        result.updateOfferFunc.nonEmpty &&
          newScoring.hasCurrentScoring &&
          newScoring.getCurrentScoring.getHealthScoring.hasBadVin &&
          newScoring.getCurrentScoring.getHealthScoring.getBadVin &&
          !newScoring.hasNextScoring
      )
      (result.nextCheck.get.getMillis shouldBe new DateTime()
        .plusHours(ScoringWorkerYdb.ScoringRenewTimeHours)
        .getMillis +- 1000)
      verifyNoMoreInteractions(mockedVinDecoderClient)
    }

    "Offer with not ready resolution" in new Fixture {
      private val TestOfferID = "123-abc"
      val scoringMessageList = List.fill(29)(ScoringMessage.newBuilder().build())
      val scoringWithHistory = ScoringMessageWithHistory.newBuilder().addAllScoringHistory(scoringMessageList.asJava)
      val offerBuilder =
        Offer.newBuilder(offerFromFile).setOfferID(TestOfferID).setScoring(scoringWithHistory)
      val autoRuBuilder = offerBuilder.getOfferAutoruBuilder
      autoRuBuilder.clearVinResolution()
      val resolutionEntry = ResolutionEntry.newBuilder().setPart(ResolutionPart.SUMMARY).setStatus(Status.IN_PROGRESS)
      val vinIndexResolution = VinIndexResolution.newBuilder().addEntries(resolutionEntry)
      val vinResolution = VinResolution.newBuilder().setResolution(vinIndexResolution).setVersion(4)
      autoRuBuilder.setVinResolution(vinResolution)

      val offer: Offer = offerBuilder.build

      when(mockedClustering.getUserCluster(?, ?)(?)).thenReturn(Seq("2"))
      val user: User = User.newBuilder().setRegistrationDate(new DateTime().toString()).build
      when(mockedPassportClient.getUser(?)(?)).thenReturn(Success(user))
      val bans = Map("test" -> DomainBan.newBuilder().addReasons(ModerationReason.DO_NOT_EXIST.name()).build)
      val moderation = UserModerationStatus
        .newBuilder()
        .putAllBans(bans.asJava)
        .build
      when(mockedPassportClient.getModeration(?)(?)).thenReturn(Some(moderation))

      val complaints = Map(
        ComplaintsReason.WRONG_ADDRESS -> Seq(
          ComplaintWithDecision("1", "1", Some("1"), "someType", "1", "1", Some("1"))
        )
      )
      when(mockedComplaintsClient.getHistory(?, ?)(?)).thenReturn(complaints)
      when(mockedModerationApiClient.getOFData(?, ?, ?)(?)).thenReturn(OFResult(None, None))
      when(mockedVinDecoderClient.getRawEssentialsReport(?)(?)).thenReturn(
        Success(
          Some(
            RawVinEssentialsReport.newBuilder().build()
          )
        )
      )

      val result = scoringWorker.process(offer, Some(Protobuf.toJson(scoringWithHistory.build())))
      val newScoring = Protobuf.fromJson[ScoringMessageWithHistory](result.nextState.get)
      assert(
        result.updateOfferFunc.isEmpty &&
          !newScoring.hasCurrentScoring &&
          newScoring.getNextScoring.getTransparencyScoring.getProvenOwnerScore == 0d &&
          newScoring.getNextScoring.getTransparencyScoring.getNoComplaintsScore == 0d
      )

      (result.nextCheck.get.getMillis shouldBe new DateTime()
        .plusHours(ScoringWorkerYdb.ScoringRetryTimeHours)
        .getMillis +- 1000)
    }

    "Offer with bad ready resolution" in new Fixture {
      private val TestOfferID = "123-abc"
      val scoringMessageList = List.fill(29)(ScoringMessage.newBuilder().build())
      val scoringWithHistory = ScoringMessageWithHistory.newBuilder().addAllScoringHistory(scoringMessageList.asJava)
      val offerBuilder =
        Offer.newBuilder(offerFromFile).setOfferID(TestOfferID).setScoring(scoringWithHistory)
      val autoRuBuilder = offerBuilder.getOfferAutoruBuilder
      autoRuBuilder.clearVinResolution()
      val resolutionEntry = ResolutionEntry.newBuilder().setPart(ResolutionPart.SUMMARY).setStatus(Status.UNTRUSTED)
      val vinIndexResolution = VinIndexResolution.newBuilder().addEntries(resolutionEntry)
      val vinResolution = VinResolution.newBuilder().setResolution(vinIndexResolution).setVersion(4)
      autoRuBuilder.setVinResolution(vinResolution)
      val offer: Offer = offerBuilder.build

      when(mockedClustering.getUserCluster(?, ?)(?)).thenReturn(Seq("2"))
      val user: User = User.newBuilder().setRegistrationDate(new DateTime().toString()).build
      when(mockedPassportClient.getUser(?)(?)).thenReturn(Success(user))
      val bans = Map("test" -> DomainBan.newBuilder().addReasons(ModerationReason.DO_NOT_EXIST.name()).build)
      val moderation = UserModerationStatus
        .newBuilder()
        .putAllBans(bans.asJava)
        .build
      when(mockedPassportClient.getModeration(?)(?)).thenReturn(Some(moderation))

      val complaints = Map(
        ComplaintsReason.WRONG_ADDRESS -> Seq(
          ComplaintWithDecision("1", "1", Some("1"), "someType", "1", "1", Some("1"))
        )
      )
      when(mockedComplaintsClient.getHistory(?, ?)(?)).thenReturn(complaints)
      when(mockedModerationApiClient.getOFData(?, ?, ?)(?)).thenReturn(OFResult(None, None))
      when(mockedVinDecoderClient.getRawEssentialsReport(?)(?)).thenReturn(
        Success(
          Some(
            RawVinEssentialsReport.newBuilder().build()
          )
        )
      )

      val result = scoringWorker.process(offer, Some(Protobuf.toJson(scoringWithHistory.build())))
      val updatedOffer = result.updateOfferFunc.get(offer)
      assert(
        updatedOffer.getScoring.hasCurrentScoring &&
          updatedOffer.getScoring.getCurrentScoring.getTransparencyScoring.hasSameVinTthScore &&
          updatedOffer.getScoring.getCurrentScoring.getTransparencyScoring.getSameVinTthScore == 0d &&
          updatedOffer.getScoring.getCurrentScoring.getTransparencyScoring.getProvenOwnerScore == 0d &&
          updatedOffer.getScoring.getCurrentScoring.getTransparencyScoring.getNoComplaintsScore == 0d
      )

      (result.nextCheck.get.getMillis shouldBe new DateTime()
        .plusHours(ScoringWorkerYdb.ScoringRenewTimeHours)
        .getMillis +- 1000)
    }

    "Successful scoring " in new Fixture {
      private val TestOfferID = "123-abc"
      val scoringMessageList = List.fill(29)(ScoringMessage.newBuilder().build())
      val scoringWithHistory = ScoringMessageWithHistory.newBuilder().addAllScoringHistory(scoringMessageList.asJava)
      val offerBuilder =
        Offer.newBuilder(offerFromFile).setOfferID(TestOfferID)
      val offer = offerBuilder.build()
      when(mockedClustering.getUserCluster(?, ?)(?)).thenReturn(Seq("2"))
      val user: User = User.newBuilder().setRegistrationDate(new DateTime().toString()).build
      when(mockedPassportClient.getUser(?)(?)).thenReturn(Success(user))
      val bans = Map("test" -> DomainBan.newBuilder().addReasons(ModerationReason.DO_NOT_EXIST.name()).build)
      val moderation = UserModerationStatus
        .newBuilder()
        .putAllBans(bans.asJava)
        .build
      when(mockedPassportClient.getModeration(?)(?)).thenReturn(Some(moderation))

      val complaints = Map(
        ComplaintsReason.WRONG_ADDRESS -> Seq(
          ComplaintWithDecision("1", "1", Some("1"), "someType", "1", "1", Some("1"))
        )
      )
      when(mockedComplaintsClient.getHistory(?, ?)(?)).thenReturn(complaints)
      when(mockedModerationApiClient.getOFData(?, ?, ?)(?)).thenReturn(OFResult(None, None))
      when(mockedVinDecoderClient.getRawEssentialsReport(?)(?)).thenReturn(
        Success(
          Some(
            RawVinEssentialsReport.newBuilder().build()
          )
        )
      )

      val result = scoringWorker.process(offer, Some(Protobuf.toJson(scoringWithHistory.build())))
      val updatedOffer = result.updateOfferFunc.get(offer)
      assert(
        updatedOffer.hasScoring &&
          updatedOffer.getScoring.hasCurrentScoring &&
          updatedOffer.getScoring.getScoringHistoryCount == 10 &&
          updatedOffer.getScoring.getCurrentScoring.getTransparencyScoring.getProvenOwnerScore == 0d &&
          updatedOffer.getScoring.getCurrentScoring.getTransparencyScoring.getNoComplaintsScore == 0d
      )
      (result.nextCheck.get.getMillis shouldBe new DateTime()
        .plusHours(ScoringWorkerYdb.ScoringRenewTimeHours)
        .getMillis +- 1000)
    }

    "Successful scoring without changes" in new Fixture {
      private val TestOfferID = "123-abc"

      val statusHistory = OfferStatusHistoryItem
        .newBuilder()
        .setTimestamp(System.currentTimeMillis())
        .setOfferStatus(CompositeStatus.CS_ACTIVE)

      val currentScoring = ScoringMessage
        .newBuilder()
        .setHealthScoring(
          HealthScoring
            .newBuilder()
            .setScoringValue(0f)
        )
        .setPriceScoring(
          PriceScoring
            .newBuilder()
            .setScoringValue(0.5f)
        )
        .setTransparencyScoring(
          TransparencyScoring
            .newBuilder()
            .setScoringValue(38f)
        )
        .setTimestampCreated(Timestamps.fromMillis(new DateTime().getMillis))
        .build()
      val scoringMessageList = List(
        currentScoring
      )
      val scoringWithHistory = ScoringMessageWithHistory
        .newBuilder()
        .addAllScoringHistory(scoringMessageList.asJava)
        .setEssentialsReportNextCheck(new DateTime().minusDays(1).getMillis)
        .setCurrentScoring(currentScoring)
      val offerBuilder =
        Offer.newBuilder(offerFromFile).setOfferID(TestOfferID).addStatusHistory(statusHistory)
      offerBuilder.getScoringBuilder.setCurrentScoring(currentScoring)
      val offer = offerBuilder.build()
      when(mockedClustering.getUserCluster(?, ?)(?)).thenReturn(Seq("2"))
      val user: User = User.newBuilder().setRegistrationDate(new DateTime().toString()).build
      when(mockedPassportClient.getUser(?)(?)).thenReturn(Success(user))
      val bans = Map("test" -> DomainBan.newBuilder().addReasons(ModerationReason.DO_NOT_EXIST.name()).build)
      val moderation = UserModerationStatus
        .newBuilder()
        .putAllBans(bans.asJava)
        .build
      when(mockedPassportClient.getModeration(?)(?)).thenReturn(Some(moderation))

      val complaints = Map(
        ComplaintsReason.WRONG_ADDRESS -> Seq(
          ComplaintWithDecision("1", "1", Some("1"), "someType", "1", "1", Some("1"))
        )
      )
      when(mockedComplaintsClient.getHistory(?, ?)(?)).thenReturn(complaints)
      when(mockedModerationApiClient.getOFData(?, ?, ?)(?)).thenReturn(OFResult(None, None))
      when(mockedVinDecoderClient.getRawEssentialsReport(?)(?)).thenReturn(
        Success(
          Some(
            RawVinEssentialsReport.newBuilder().build()
          )
        )
      )

      val result = scoringWorker.process(offer, Some(Protobuf.toJson(scoringWithHistory.build())))
      assert(
        result.updateOfferFunc.isEmpty
      )
      (result.nextCheck.get.getMillis shouldBe new DateTime()
        .plusHours(ScoringWorkerYdb.ScoringRenewTimeHours)
        .getMillis +- 1000)
      verify(mockedVinDecoderClient, times(1)).getRawEssentialsReport(?)(?)
    }
    "Successful scoring without changes but outdated" in new Fixture {
      private val TestOfferID = "123-abc"

      val statusHistory = OfferStatusHistoryItem
        .newBuilder()
        .setTimestamp(new DateTime().minusDays(1).getMillis)
        .setOfferStatus(CompositeStatus.CS_ACTIVE)

      val currentScoring = ScoringMessage
        .newBuilder()
        .setHealthScoring(
          HealthScoring
            .newBuilder()
            .setScoringValue(0f)
        )
        .setPriceScoring(
          PriceScoring
            .newBuilder()
            .setScoringValue(0.5f)
        )
        .setTransparencyScoring(
          TransparencyScoring
            .newBuilder()
            .setScoringValue(36f)
        )
        .setTimestampCreated(Timestamps.fromMillis(new DateTime().minusHours(25).getMillis))
        .build()
      val scoringMessageList = List(
        currentScoring
      )
      val scoringWithHistory = ScoringMessageWithHistory
        .newBuilder()
        .addAllScoringHistory(scoringMessageList.asJava)
        .setEssentialsReportNextCheck(new DateTime().plusHours(1).getMillis)
        .setCurrentScoring(currentScoring)
      val offerBuilder =
        Offer.newBuilder(offerFromFile).setOfferID(TestOfferID).addStatusHistory(statusHistory)
      val offer = offerBuilder.build()
      when(mockedClustering.getUserCluster(?, ?)(?)).thenReturn(Seq("2"))
      val user: User = User.newBuilder().setRegistrationDate(new DateTime().toString()).build
      when(mockedPassportClient.getUser(?)(?)).thenReturn(Success(user))
      val bans = Map("test" -> DomainBan.newBuilder().addReasons(ModerationReason.DO_NOT_EXIST.name()).build)
      val moderation = UserModerationStatus
        .newBuilder()
        .putAllBans(bans.asJava)
        .build
      when(mockedPassportClient.getModeration(?)(?)).thenReturn(Some(moderation))

      val complaints = Map(
        ComplaintsReason.WRONG_ADDRESS -> Seq(
          ComplaintWithDecision("1", "1", Some("1"), "someType", "1", "1", Some("1"))
        )
      )
      when(mockedComplaintsClient.getHistory(?, ?)(?)).thenReturn(complaints)
      when(mockedModerationApiClient.getOFData(?, ?, ?)(?)).thenReturn(OFResult(None, None))
      when(mockedVinDecoderClient.getRawEssentialsReport(?)(?)).thenReturn(
        Success(
          Some(
            RawVinEssentialsReport.newBuilder().build()
          )
        )
      )

      val result = scoringWorker.process(offer, Some(Protobuf.toJson(scoringWithHistory.build())))
      assert(
        result.updateOfferFunc.nonEmpty
      )
      (result.nextCheck.get.getMillis shouldBe new DateTime()
        .plusHours(ScoringWorkerYdb.ScoringRenewTimeHours)
        .getMillis +- 1000)

      verify(mockedVinDecoderClient, times(0)).getRawEssentialsReport(?)(?)

    }

    "unsuccessful scoring with exceptions" in new Fixture {
      private val TestOfferID = "123-abc"

      val user = User.newBuilder().setRegistrationDate(new DateTime().toString()).build

      val offer: Offer =
        Offer.newBuilder(offerFromFile).setOfferID(TestOfferID).build

      when(mockedClustering.getUserCluster(?, ?)(?)).thenThrow(new RuntimeException("test1"))
      when(mockedComplaintsClient.getHistory(?, ?)(?)).thenThrow(new RuntimeException("test4"))
      when(mockedModerationApiClient.getOFData(?, ?, ?)(?)).thenThrow(new RuntimeException("test5"))
      when(mockedPassportClient.getUser(?)(?)).thenReturn(Success(user))
      when(mockedVinDecoderClient.getRawEssentialsReport(?)(?)).thenReturn(
        Success(
          Some(
            RawVinEssentialsReport.newBuilder().build()
          )
        )
      )

      val result = scoringWorker.process(offer, None)
      assert(result.updateOfferFunc.isEmpty)
      (result.nextCheck.get.getMillis shouldBe new DateTime()
        .plusHours(ScoringWorkerYdb.ScoringRetryTimeHours)
        .getMillis +- 1000)
    }
    "successful scoring with passport not found" in new Fixture {
      private val TestOfferID = "123-abc"

      val user = User.newBuilder().setRegistrationDate(new DateTime().toString()).build

      val offerBuilder = Offer.newBuilder(offerFromFile).setOfferID(TestOfferID)
      offerBuilder.getOfferAutoruBuilder.setProvenOwnerModerationState(
        ProvenOwnerModerationState.newBuilder().setState(Verdict.PROVEN_OWNER_OK).build()
      )
      val offer: Offer =
        offerBuilder.build
      when(mockedClustering.getUserCluster(?, ?)(?)).thenReturn(Seq("2"))
      when(mockedPassportClient.getUser(?)(?)).thenReturn(Failure(new NoSuchElementException("test")))
      val bans = Map("test" -> DomainBan.newBuilder().addReasons(ModerationReason.DO_NOT_EXIST.name()).build)

      val moderation = UserModerationStatus
        .newBuilder()
        .putAllBans(bans.asJava)
        .build

      when(mockedPassportClient.getModeration(?)(?)).thenReturn(Some(moderation))

      val complaints = Map.empty[ComplaintsReason, Seq[ComplaintWithDecision]]
      when(mockedComplaintsClient.getHistory(?, ?)(?)).thenReturn(complaints)

      when(mockedModerationApiClient.getOFData(?, ?, ?)(?)).thenReturn(OFResult(None, None))
      when(mockedVinDecoderClient.getRawEssentialsReport(?)(?)).thenReturn(
        Success(
          Some(
            RawVinEssentialsReport.newBuilder().build()
          )
        )
      )

      val result = scoringWorker.process(offer, None)
      val updatedOffer = result.updateOfferFunc.get(offer)
      assert(
        updatedOffer.getScoring.hasCurrentScoring &&
          !updatedOffer.getScoring.hasNextScoring &&
          updatedOffer.getScoring.getCurrentScoring.getTransparencyScoring.getProvenOwnerScore == ScoringWorkerYdb.Transparency.ProvenOwnerMultiplier.value &&
          updatedOffer.getScoring.getCurrentScoring.getTransparencyScoring.getNoComplaintsScore == ScoringWorkerYdb.Transparency.NoComplaintsMultiplier.value
      )
      (result.nextCheck.get.getMillis shouldBe new DateTime()
        .plusHours(ScoringWorkerYdb.ScoringRenewTimeHours)
        .getMillis +- 1000)
    }
    "unsuccessful scoring with passport unsuccessful" in new Fixture {
      private val TestOfferID = "123-abc"

      val user = User.newBuilder().setRegistrationDate(new DateTime().toString()).build

      val offer: Offer =
        Offer.newBuilder(offerFromFile).setOfferID(TestOfferID).build
      when(mockedClustering.getUserCluster(?, ?)(?)).thenReturn(Seq("2"))
      when(mockedPassportClient.getUser(?)(?)).thenReturn(Failure(new IllegalArgumentException("test")))
      val bans = Map("test" -> DomainBan.newBuilder().addReasons(ModerationReason.DO_NOT_EXIST.name()).build)
      val moderation = UserModerationStatus
        .newBuilder()
        .putAllBans(bans.asJava)
        .build
      when(mockedPassportClient.getModeration(?)(?)).thenReturn(Some(moderation))

      val complaints = Map(
        ComplaintsReason.WRONG_ADDRESS -> Seq(
          ComplaintWithDecision("1", "1", Some("1"), "someType", "1", "1", Some("1"))
        )
      )
      when(mockedComplaintsClient.getHistory(?, ?)(?)).thenReturn(complaints)

      when(mockedModerationApiClient.getOFData(?, ?, ?)(?)).thenReturn(OFResult(None, None))
      when(mockedVinDecoderClient.getRawEssentialsReport(?)(?)).thenReturn(
        Success(
          Some(
            RawVinEssentialsReport.newBuilder().build()
          )
        )
      )
      val result = scoringWorker.process(offer, None)
      assert(result.updateOfferFunc.isEmpty)
      (result.nextCheck.get.getMillis shouldBe new DateTime()
        .plusHours(ScoringWorkerYdb.ScoringRetryTimeHours)
        .getMillis +- 1000)
    }

    "successful scoring with carfax returns vin error" in new Fixture {
      private val TestOfferID = "123-abc"

      val statusHistory = OfferStatusHistoryItem
        .newBuilder()
        .setTimestamp(System.currentTimeMillis())
        .setOfferStatus(CompositeStatus.CS_ACTIVE)
      val offer: Offer =
        Offer.newBuilder(offerFromFile).setOfferID(TestOfferID).addStatusHistory(statusHistory).build
      when(mockedClustering.getUserCluster(?, ?)(?)).thenReturn(Seq("2"))
      when(mockedPassportClient.getUser(?)(?)).thenReturn(Failure(new NoSuchElementException("test")))
      val bans = Map("test" -> DomainBan.newBuilder().addReasons(ModerationReason.DO_NOT_EXIST.name()).build)

      val moderation = UserModerationStatus
        .newBuilder()
        .putAllBans(bans.asJava)
        .build

      when(mockedPassportClient.getModeration(?)(?)).thenReturn(Some(moderation))

      val complaints = Map(
        ComplaintsReason.WRONG_ADDRESS -> Seq(
          ComplaintWithDecision("1", "1", Some("1"), "someType", "1", "1", Some("1"))
        )
      )
      when(mockedComplaintsClient.getHistory(?, ?)(?)).thenReturn(complaints)

      when(mockedModerationApiClient.getOFData(?, ?, ?)(?)).thenReturn(OFResult(None, None))
      when(mockedVinDecoderClient.getRawEssentialsReport(?)(?)).thenReturn(
        Failure(new IllegalArgumentException("test"))
      )
      val result = scoringWorker.process(offer, None)
      val updatedOffer = result.updateOfferFunc.get(offer)
      (result.nextCheck.get.getMillis shouldBe new DateTime()
        .plusHours(ScoringWorkerYdb.ScoringRenewTimeHours)
        .getMillis +- 1000)
      assert(
        updatedOffer.hasScoring &&
          updatedOffer.getScoring.hasCurrentScoring &&
          updatedOffer.getScoring.getCurrentScoring.getHealthScoring.hasBadVin &&
          updatedOffer.getScoring.getCurrentScoring.getHealthScoring.getBadVin &&
          !updatedOffer.getScoring.hasNextScoring
      )

    }

    "unsuccessful scoring with failed carfax request" in new Fixture {
      private val TestOfferID = "123-abc"

      val user = User.newBuilder().setRegistrationDate(new DateTime().toString()).build

      val offer: Offer =
        Offer.newBuilder(offerFromFile).setOfferID(TestOfferID).build
      when(mockedClustering.getUserCluster(?, ?)(?)).thenReturn(Seq("2"))
      when(mockedPassportClient.getUser(?)(?)).thenReturn(Failure(new IllegalArgumentException("test")))
      val bans = Map("test" -> DomainBan.newBuilder().addReasons(ModerationReason.DO_NOT_EXIST.name()).build)
      val moderation = UserModerationStatus
        .newBuilder()
        .putAllBans(bans.asJava)
        .build
      when(mockedPassportClient.getModeration(?)(?)).thenReturn(Some(moderation))

      val complaints = Map(
        ComplaintsReason.WRONG_ADDRESS -> Seq(
          ComplaintWithDecision("1", "1", Some("1"), "someType", "1", "1", Some("1"))
        )
      )
      when(mockedComplaintsClient.getHistory(?, ?)(?)).thenReturn(complaints)

      when(mockedModerationApiClient.getOFData(?, ?, ?)(?)).thenReturn(OFResult(None, None))
      when(mockedVinDecoderClient.getRawEssentialsReport(?)(?)).thenReturn(
        Failure(new RuntimeException("test"))
      )
      val result = scoringWorker.process(offer, None)
      (result.nextCheck.get.getMillis shouldBe new DateTime()
        .plusHours(ScoringWorkerYdb.ScoringRetryTimeHours)
        .getMillis +- 1000)
      assert(result.updateOfferFunc.isEmpty)
    }

    "unsuccessful scoring with empty vin and passport excpt" in new Fixture {
      private val TestOfferID = "123-abc"

      val user = User.newBuilder().setRegistrationDate(new DateTime().toString()).build

      val offerBuilder = Offer.newBuilder(offerFromFile)
      offerBuilder.setOfferID(TestOfferID)
      offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setVin("")
      val offer: Offer =
        offerBuilder.build
      when(mockedClustering.getUserCluster(?, ?)(?)).thenReturn(Seq("2"))
      when(mockedPassportClient.getUser(?)(?)).thenReturn(Failure(new IllegalArgumentException("test")))
      val bans = Map("test" -> DomainBan.newBuilder().addReasons(ModerationReason.DO_NOT_EXIST.name()).build)
      val moderation = UserModerationStatus
        .newBuilder()
        .putAllBans(bans.asJava)
        .build
      when(mockedPassportClient.getModeration(?)(?)).thenReturn(Some(moderation))

      val complaints = Map(
        ComplaintsReason.WRONG_ADDRESS -> Seq(
          ComplaintWithDecision("1", "1", Some("1"), "someType", "1", "1", Some("1"))
        )
      )
      when(mockedComplaintsClient.getHistory(?, ?)(?)).thenReturn(complaints)

      when(mockedModerationApiClient.getOFData(?, ?, ?)(?)).thenReturn(OFResult(None, None))
      when(mockedVinDecoderClient.getRawEssentialsReport(?)(?)).thenReturn(
        Failure(new RuntimeException("test"))
      )
      val result = scoringWorker.process(offer, None)
      assert(result.updateOfferFunc.isEmpty)

      (result.nextCheck.get.getMillis shouldBe new DateTime()
        .plusHours(ScoringWorkerYdb.ScoringRetryTimeHours)
        .getMillis +- 1000)
    }

    "generation changed, should process offer" in new Fixture {
      val scoringMessageList = List.fill(29)(ScoringMessage.newBuilder().build())
      val scoringWithHistory =
        ScoringMessageWithHistory.newBuilder().addAllScoringHistory(scoringMessageList.asJava).setScoringGeneration(0)
      when(mockedFeatureManager.ScoringYdb).thenReturn(mockedVosFeature)
      when(mockedVosFeature.value).thenReturn(WithGeneration(false, 2))

      val offerBuilder = createOffer().addFlag(OfferFlag.OF_INACTIVE)
      offerBuilder.setScoring(scoringWithHistory)
      val offer = offerBuilder.build()
      assert(scoringWorker.shouldProcess(offer, None).shouldProcess)
    }

    "generation not exists, should process offer" in new Fixture {
      val scoringMessageList = List.fill(29)(ScoringMessage.newBuilder().build())
      val scoringWithHistory = ScoringMessageWithHistory.newBuilder().addAllScoringHistory(scoringMessageList.asJava)

      val offerBuilder = createOffer().addFlag(OfferFlag.OF_INACTIVE)
      offerBuilder.setScoring(scoringWithHistory)
      val offer = offerBuilder.build()
      scoringWorker.shouldProcess(offer, None)

    }

    "private, inactive offer with old generation" in new Fixture {
      val scoringMessageList = List.fill(29)(ScoringMessage.newBuilder().build())
      val scoringWithHistory = ScoringMessageWithHistory.newBuilder().addAllScoringHistory(scoringMessageList.asJava)
      when(mockedFeatureManager.ScoringYdb).thenReturn(mockedVosFeature)
      when(mockedVosFeature.value).thenReturn(WithGeneration(false, 2))

      val offerBuilder = createOffer().addFlag(OfferFlag.OF_INACTIVE)
      offerBuilder.getScoringBuilder.setScoringGeneration(0)
      val offer = offerBuilder.build()
      assert(scoringWorker.shouldProcess(offer, None).shouldProcess)

    }

    "offer is dealer, should not process" in new Fixture {

      override val offer = createOffer(dealer = true).build()

      assert(!scoringWorker.shouldProcess(offer, None).shouldProcess)
    }

    "private, inactive offer" in new Fixture {

      override val offer = createOffer().addFlag(OfferFlag.OF_INACTIVE).build()
      assert(!scoringWorker.shouldProcess(offer, None).shouldProcess)
    }

    "is not car offer" in new Fixture {
      override val offer = createOffer(category = Category.TRUCKS).build()
      assert(!scoringWorker.shouldProcess(offer, None).shouldProcess)

    }
    "range check" in new Fixture {
      assert(ScoringWorkerYdb.transformToRange(TransparencyRangeMin, TransparencyRangeMax, TransparencyRangeMax) == 100)
      assert(ScoringWorkerYdb.transformToRange(TransparencyRangeMin, TransparencyRangeMax, TransparencyRangeMin) == 0)
      assert(ScoringWorkerYdb.transformToRange(TransparencyRangeMin, TransparencyRangeMax, -1) == 0)
      override val offer: Offer = null
    }

  }
}
