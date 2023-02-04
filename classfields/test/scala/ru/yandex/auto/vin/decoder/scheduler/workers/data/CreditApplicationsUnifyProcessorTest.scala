package ru.yandex.auto.vin.decoder.scheduler.workers.data

import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito.{never, reset, times, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.extdata.region.GeoRegion
import ru.yandex.auto.vin.decoder.geo.GeocoderManager
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, PreparedDataState}
import ru.yandex.auto.vin.decoder.proto.VinHistory._
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator.MarkModelResult
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageModel.{MetaData, OnlyPreparedModel, PreparedData}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class CreditApplicationsUnifyProcessorTest
  extends AnyWordSpecLike
  with Matchers
  with MockitoSupport
  with BeforeAndAfter {

  implicit private val t: Traced = Traced.empty
  private val vin = VinCode("X4X3D59430PS96744")
  private val rawStorageManager = mock[RawStorageManager[VinCode]]
  private val geocoder = mock[GeocoderManager]
  private val unificator = mock[Unificator]
  private val processor = new CreditApplicationsUnifyProcessor(rawStorageManager, geocoder, unificator)
  private val delay = DefaultDelay(1.hour)
  private val region = "Moscow"
  private val mark = "BMW"
  private val model = "M3"

  before {
    reset(rawStorageManager)
    reset(geocoder)
    reset(unificator)
    when(geocoder.toGeo(?, ?)).thenCallRealMethod()
  }

  "process" should {
    "do not update records" when {
      "there are not records in storage" in {
        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List.empty))

        val state = createPreparedDataState
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe false

        verify(rawStorageManager, never()).updatePrepared(?, ?, ?)(?)
      }
      "all data already processed" in {

        val record =
          buildRecord(buildMM(), buildGeo(), processedMark = true, processedGeo = true)

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List(record)))

        val state = createPreparedDataState
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe false

        verify(rawStorageManager, never()).updatePrepared(?, ?, ?)(?)
      }
    }
    "update records and finish" when {
      "all data processed after processing" in {

        val geoRaw = buildGeoRaw
        val mmRaw = buildMMRaw
        val record = buildRecord(mmRaw, geoRaw)
        val updatedPrepared = buildVh(buildMM(), buildGeo(), processedMark = true, processedGeo = true)

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List(record)))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)
        when(geocoder.findRegionBy(s"$region"))
          .thenReturn(Future.successful(Some(GeoRegion(1, region, 1, "", "", "", 2, "", "", 0))))
        when(unificator.unifyHeadOption(s"$mark $model"))
          .thenReturn(Future.successful(Some(MarkModelResult(mark, model, s"$mark $model", unclear = false))))

        val state = createPreparedDataState
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe false

        verify(rawStorageManager, times(1)).updatePrepared(?, eq(updatedPrepared), ?)(?)
      }

      "only mark model has raw data and should be processed" in {

        val mmRaw = buildMMRaw
        val record = buildRecord(mmRaw, Geo.newBuilder().build())
        val updatedPrepared = buildVh(buildMM(), Geo.newBuilder().build(), processedMark = true, processedGeo = false)

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List(record)))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)
        when(unificator.unifyHeadOption(s"$mark $model"))
          .thenReturn(Future.successful(Some(MarkModelResult(mark, model, s"$mark $model", unclear = false))))

        val state = createPreparedDataState
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe false

        verify(rawStorageManager, times(1)).updatePrepared(?, eq(updatedPrepared), ?)(?)
      }

      "only geo has raw data and should be processed" in {

        val geoRaw = buildGeoRaw
        val record = buildRecord(MarkModelInfo.newBuilder().build(), geoRaw)
        val updatedPrepared =
          buildVh(MarkModelInfo.newBuilder().build(), buildGeo(), processedMark = false, processedGeo = true)

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List(record)))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)
        when(geocoder.findRegionBy(s"$region"))
          .thenReturn(Future.successful(Some(GeoRegion(1, region, 1, "", "", "", 2, "", "", 0))))

        val state = createPreparedDataState
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe false

        verify(rawStorageManager, times(1)).updatePrepared(?, eq(updatedPrepared), ?)(?)
      }

      "only for mark model unified data found when processing" in {

        val mmRaw = buildMMRaw
        val geoRaw = buildGeoRaw
        val record = buildRecord(mmRaw, geoRaw)
        val updatedPrepared = buildVh(buildMM(), geoRaw, processedMark = true, processedGeo = true)

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List(record)))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)
        when(unificator.unifyHeadOption(s"$mark $model"))
          .thenReturn(Future.successful(Some(MarkModelResult(mark, model, s"$mark $model", unclear = false))))
        when(geocoder.findRegionBy(s"$region"))
          .thenReturn(Future.successful(None))

        val state = createPreparedDataState
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe false

        verify(rawStorageManager, times(1)).updatePrepared(?, eq(updatedPrepared), ?)(?)
      }

      "only for geo unified data found when processing" in {

        val mmRaw = buildMMRaw
        val geoRaw = buildGeoRaw
        val record = buildRecord(mmRaw, geoRaw)
        val updatedPrepared = buildVh(mmRaw, buildGeo(), processedMark = true, processedGeo = true)

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List(record)))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)
        when(unificator.unifyHeadOption(s"$mark $model"))
          .thenReturn(Future.successful(None))
        when(geocoder.findRegionBy(s"$region"))
          .thenReturn(Future.successful(Some(GeoRegion(1, region, 1, "", "", "", 2, "", "", 0))))

        val state = createPreparedDataState
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe false

        verify(rawStorageManager, times(1)).updatePrepared(?, eq(updatedPrepared), ?)(?)
      }
      "geo was already processed update only mark model" in {

        val mmRaw = buildMMRaw
        val record = buildRecord(mmRaw, buildGeo(), processedGeo = true)
        val updatedPrepared = buildVh(buildMM(), buildGeo(), processedMark = true, processedGeo = true)

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List(record)))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)
        when(unificator.unifyHeadOption(s"$mark $model"))
          .thenReturn(Future.successful(Some(MarkModelResult(mark, model, s"$mark $model", unclear = false))))

        val state = createPreparedDataState
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe false

        verify(rawStorageManager, times(1)).updatePrepared(?, eq(updatedPrepared), ?)(?)
      }
    }
    "update records and reschedule" when {
      "geo unify process failed when processing" in {

        val geoRaw = buildGeoRaw
        val markRaw = buildMMRaw
        val record = buildRecord(markRaw, geoRaw)
        val updatedPrepared = buildVh(buildMM(), geoRaw, processedMark = true, processedGeo = false)

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List(record)))
        when(geocoder.findRegionBy(s"$region")).thenReturn(Future.failed(new RuntimeException))
        when(unificator.unifyHeadOption(s"$mark $model"))
          .thenReturn(Future.successful(Some(MarkModelResult(mark, model, s"$mark $model", unclear = false))))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)

        val state = createPreparedDataState
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe true

        verify(rawStorageManager, times(1)).updatePrepared(?, eq(updatedPrepared), ?)(?)
      }
      "mark model unify process failed when processing" in {

        val geoRaw = buildGeoRaw
        val markRaw = buildMMRaw
        val record = buildRecord(markRaw, geoRaw)
        val updatedPrepared = buildVh(markRaw, buildGeo(), processedMark = false, processedGeo = true)

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List(record)))
        when(geocoder.findRegionBy(s"$region"))
          .thenReturn(Future.successful(Some(GeoRegion(1, region, 1, "", "", "", 2, "", "", 0))))
        when(unificator.unifyHeadOption(s"$mark $model")).thenReturn(Future.failed(new RuntimeException))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)

        val state = createPreparedDataState
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe true

        verify(rawStorageManager, times(1)).updatePrepared(?, eq(updatedPrepared), ?)(?)
      }
      "update failed" in {

        val mmRaw = buildMMRaw
        val geoRaw = buildGeoRaw
        val record = buildRecord(mmRaw, geoRaw)
        val updatedPrepared = buildVh(buildMM(), buildGeo(), processedMark = true, processedGeo = true)

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List(record)))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.failed(new RuntimeException))
        when(unificator.unifyHeadOption(s"$mark $model"))
          .thenReturn(Future.successful(Some(MarkModelResult(mark, model, s"$mark $model", unclear = false))))
        when(geocoder.findRegionBy(s"$region"))
          .thenReturn(Future.successful(Some(GeoRegion(1, region, 1, "", "", "", 2, "", "", 0))))

        val state = createPreparedDataState
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe true

        verify(rawStorageManager, times(1)).updatePrepared(?, eq(updatedPrepared), ?)(?)
      }
    }
  }

  private def buildGeoRaw = {
    Geo.newBuilder().setRaw(s"$region").build()
  }

  private def buildMMRaw = {
    MarkModelInfo.newBuilder().setRaw(s"$mark $model").build()
  }

  private def createPreparedDataState: PreparedDataState = {
    PreparedDataState.newBuilder().setEventType(EventType.TINKOFF_MILEAGE).setShouldProcess(true).build()
  }

  private def buildRecord(
      markModelInfo: MarkModelInfo,
      geo: Geo,
      processedMark: Boolean = false,
      processedGeo: Boolean = false): OnlyPreparedModel[VinCode] = {
    OnlyPreparedModel(
      vin,
      PreparedData(buildVh(markModelInfo, geo, processedMark, processedGeo)),
      MetaData("", EventType.TINKOFF_MILEAGE, "", 123L, 123L, 123L)
    )
  }

  private def buildVh(
      markModelInfo: MarkModelInfo,
      geo: Geo,
      processedMark: Boolean,
      processedGeo: Boolean) = {
    val vh = VinInfoHistory.newBuilder()
    val creditApplication = CreditApplication.newBuilder()
    creditApplication.setMarkModelInfo(markModelInfo)
    creditApplication.setGeo(geo)
    creditApplication.getMarkModelInfoBuilder.setProcessed(processedMark)
    creditApplication.getGeoBuilder.setProcessed(processedGeo)
    vh.addCreditApplications(creditApplication)
    vh.build()
  }

  private def buildGeo(): Geo = {
    val builder = Geo.newBuilder().setRaw(s"$region").setRegionName(region).setGeobaseId(1)
    builder.build()
  }

  private def buildMM(): MarkModelInfo = {
    val builder = MarkModelInfo.newBuilder().setRaw(s"$mark $model").setMark(mark).setModel(model)
    builder.build()
  }

}
