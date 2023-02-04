package ru.yandex.realty.rent.stage.flat

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.startrack.StarTrackClient
import ru.yandex.realty.clients.startrack.model.{CreateIssueRequest, CreateIssueResponse}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.implicits.RentEnumImplicitConverters.OwnerRequestConverters
import ru.yandex.realty.rent.model.Flat
import ru.yandex.realty.rent.model.enums.OwnerRequestStatus
import ru.yandex.realty.rent.model.enums.OwnerRequestStatus.OwnerRequestStatus
import ru.yandex.realty.rent.proto.model.owner_request.{OwnerRequestData, OwnerRequestStatusHistoryRecord}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.TimeUtils
import ru.yandex.realty.watching.ProcessingState

import java.time.{Clock, Instant}
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ContentStageSpec extends AsyncSpecBase with RentModelsGen {

  import OwnerRequestConverters._
  implicit val traced: Traced = Traced.empty

  trait TestEnv {
    val queue = "queue"
    val key = "key"
    val earlyInThePast: Instant = Instant.parse("2022-03-01T09:00:00.00Z")
    val applicableSince: Instant = Instant.parse("2022-04-12T00:00:00.00Z")
    val statusChanged: Instant = Instant.parse("2022-04-12T10:00:00.00Z")
    val after39mins: Instant = Instant.parse("2022-04-12T10:39:00.00Z")
    val after40mins: Instant = Instant.parse("2022-04-12T10:40:00.00Z")
    val after41mins: Instant = Instant.parse("2022-04-12T10:41:00.00Z")

    val config: ContentStage.Config = ContentStage.Config(
      timeGapSeconds = 40 * 60,
      startrekQueue = queue,
      startrekComponent = 123,
      applicableSinceEpochSeconds = applicableSince.getEpochSecond
    )
    val startrekClientMock: StarTrackClient = mock[StarTrackClient]
    val clockMock: Clock = mock[Clock]

    private def createFlat(status: OwnerRequestStatus, changeTime: Instant) = {
      val ownerRequest = ownerRequestGen.next.copy(
        status = status,
        data = OwnerRequestData
          .newBuilder()
          .addStatusHistory {
            OwnerRequestStatusHistoryRecord
              .newBuilder()
              .setChangeTime(TimeUtils.instantToProtoTimestamp(changeTime))
              .setStatus(status.asApiProto)
          }
          .build()
      )
      flatGen(false).next
        .copy(ownerRequests = Seq(ownerRequest))
    }

    def createRelevantFlat(changeTime: Instant = statusChanged): Flat =
      createFlat(OwnerRequestStatus.WorkInProgress, changeTime)

    def createIrrelevantFlat(): Flat = createFlat(OwnerRequestStatus.Draft, statusChanged)

    def invokeStage(flat: Flat): ProcessingState[Flat] = {
      val stage = new ContentStage(config, startrekClientMock, clockMock)
      val state: ProcessingState[Flat] = ProcessingState(flat)
      stage.process(state)(Traced.empty).futureValue
    }
  }

  "ContentStage" should {

    "process flat" in new TestEnv {
      (clockMock.instant _).expects().returning(after41mins).once()
      (startrekClientMock
        .createIssue(_: CreateIssueRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(CreateIssueResponse.Success(key)))

      val flat: Flat = createRelevantFlat()

      val result: ProcessingState[Flat] = invokeStage(flat)
      result.entry.ownerRequests.head.data.getContentStartrekKey shouldBe key
    }

    "not process flat with irrelevant status" in new TestEnv {
      (clockMock.instant _).expects().never()
      (startrekClientMock.createIssue(_: CreateIssueRequest)(_: Traced)).expects(*, *).never()

      val flat: Flat = createIrrelevantFlat()

      val result: ProcessingState[Flat] = invokeStage(flat)
      result shouldBe ProcessingState(flat)
    }

    "not process flat with status changed before applicable date" in new TestEnv {
      (clockMock.instant _).expects().returning(after41mins).never()
      (startrekClientMock.createIssue(_: CreateIssueRequest)(_: Traced)).expects(*, *).never()

      val flat: Flat = createRelevantFlat(changeTime = earlyInThePast)

      val result: ProcessingState[Flat] = invokeStage(flat)
      result shouldBe ProcessingState(flat)
    }

    "not process flat sooner than exact time gap" in new TestEnv {
      (clockMock.instant _).expects().returning(after39mins).once()
      (startrekClientMock.createIssue(_: CreateIssueRequest)(_: Traced)).expects(*, *).never()

      val flat: Flat = createRelevantFlat()

      val result: ProcessingState[Flat] = invokeStage(flat)
      result shouldBe ProcessingState(flat).withVisitTime(after40mins)
    }

    "handle startrek failure" in new TestEnv {
      (clockMock.instant _).expects().returning(after41mins).once()
      (startrekClientMock
        .createIssue(_: CreateIssueRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.failed(new RuntimeException))

      val flat: Flat = createRelevantFlat()

      val result: ProcessingState[Flat] = invokeStage(flat)
      result.entry.ownerRequests.head.data.getContentStartrekKey shouldBe empty
    }

    "handle startrek conflict if task already exists" in new TestEnv {
      (clockMock.instant _).expects().returning(after41mins)
      (startrekClientMock
        .createIssue(_: CreateIssueRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(CreateIssueResponse.AlreadyExists(key)))

      val flat: Flat = createRelevantFlat()

      val result: ProcessingState[Flat] = invokeStage(flat)
      result.entry.ownerRequests.head.data.getContentStartrekKey shouldBe key
    }
  }
}
