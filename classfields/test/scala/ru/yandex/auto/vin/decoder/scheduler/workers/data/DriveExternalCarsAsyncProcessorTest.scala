package ru.yandex.auto.vin.decoder.scheduler.workers.data

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.LicensePlate
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeReportType
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, PreparedDataState}
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Carsharing, VinInfoHistory}
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageModel.{MetaData, PreparedData, RawData}
import ru.yandex.auto.vin.decoder.ydb.raw.model.RowModel
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.IterableHasAsJava

class DriveExternalCarsAsyncProcessorTest
  extends AnyWordSpecLike
  with Matchers
  with MockitoSupport
  with BeforeAndAfter {

  implicit val t: Traced = Traced.empty
  private val rawStorageManager = mock[RawStorageManager[LicensePlate]]
  val processor = new DriveExternalCarsAsyncProcessor(rawStorageManager)

  private val lp = LicensePlate("A123AA77")

  "process" should {
    "request relationship and finish" when {
      "exist active carsharing without fresh relationship" in {
        val updatedState = process(
          List(buildCarsharing(System.currentTimeMillis() - 1.day.toMillis, None)),
          Some(System.currentTimeMillis() - 2.days.toMillis)
        )

        updatedState.getAutocodeState.getAutocodeReportsCount shouldBe 1
        val report = updatedState.getAutocodeState.findReport(AutocodeReportType.Identifiers.id)
        report.nonEmpty shouldBe true
        report.get.isInProgress shouldBe true

        checkPreparedStateFinished(updatedState)
      }
    }
    "finish without request relationship" when {
      "doesnt exist active carsharing (there are no fresh relationship)" in {
        val updatedState = process(
          List(buildCarsharing(System.currentTimeMillis() - 1.day.toMillis, Some(System.currentTimeMillis()))),
          Some(System.currentTimeMillis() - 2.days.toMillis)
        )

        updatedState.getAutocodeState.getAutocodeReportsCount shouldBe 1
        val report = updatedState.getAutocodeState.findReport(AutocodeReportType.Identifiers.id)
        report.nonEmpty shouldBe true
        report.get.isInProgress shouldBe false

        checkPreparedStateFinished(updatedState)
      }
      "doesnt exist active carsharing (there are no relationships)" in {
        val updatedState = process(
          List(buildCarsharing(System.currentTimeMillis() - 1.day.toMillis, Some(System.currentTimeMillis()))),
          None
        )

        updatedState.getAutocodeState.getAutocodeReportsCount shouldBe 0

        checkPreparedStateFinished(updatedState)
      }
      "exist active carsaring and there are fresh relationshup" in {
        val updatedState = process(
          List(buildCarsharing(System.currentTimeMillis() - 1.day.toMillis, None)),
          Some(System.currentTimeMillis())
        )

        updatedState.getAutocodeState.getAutocodeReportsCount shouldBe 1
        val report = updatedState.getAutocodeState.findReport(AutocodeReportType.Identifiers.id)
        report.nonEmpty shouldBe true
        report.get.isInProgress shouldBe false

        checkPreparedStateFinished(updatedState)
      }
    }
  }

  private def checkPreparedStateFinished(updatedState: CompoundState) = {
    updatedState.getPreparedDataStateCount shouldBe 1
    updatedState.getPreparedDataState(0).getShouldProcess shouldBe false
  }

  private def process(carsharing: List[Carsharing], lastRelationshipUpdate: Option[Long]) = {
    when(rawStorageManager.getAllLatestByIdentifierAndSource[LicensePlate](?, ?)(?))
      .thenReturn(Future.successful(List(buildRawStorageRecord(carsharing))))

    val state = createPreparedDataState(EventType.ACAT_INFO, true)
    val compoundState = buildState(lastRelationshipUpdate, state)
    val holder = WatchingStateHolder(lp, compoundState, System.currentTimeMillis())
    val result = processor.process(lp, state).await
    result.updater.get.apply(holder.toUpdate).state
  }

  private def createPreparedDataState(eventType: EventType, shouldProcess: Boolean): PreparedDataState = {
    PreparedDataState.newBuilder().setEventType(eventType).setShouldProcess(shouldProcess).build()
  }

  private def buildState(lastRelationshipUpdate: Option[Long], preparedDataState: PreparedDataState) = {
    val builder = CompoundState.newBuilder()
    lastRelationshipUpdate.foreach(ts => {
      val autocodeBuilder = builder.getAutocodeStateBuilder.addAutocodeReportsBuilder()
      autocodeBuilder
        .setReportType(AutocodeReportType.Identifiers.id)
        .setProcessRequested(ts - 1)
        .setReportArrived(ts)
    })

    builder.addPreparedDataState(preparedDataState)
    builder.build()
  }

  private def buildRawStorageRecord(carsharing: Seq[Carsharing]): RowModel[LicensePlate] = {
    val vh = VinInfoHistory.newBuilder().addAllCarsharing(carsharing.asJava).build()
    RowModel(lp, RawData("", ""), PreparedData(vh), MetaData("", EventType.UNKNOWN, "", 123L, 123L, 123L))
  }

  private def buildCarsharing(date: Long, dateFinish: Option[Long]) = {
    val builder = Carsharing.newBuilder().setDate(date)
    dateFinish.foreach(builder.setDateFinish)
    builder.build()
  }

}
