package ru.yandex.auto.vin.decoder.scheduler.workers.data

import auto.carfax.common.utils.tracing.Traced
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{never, reset, times, verify}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{Assertion, BeforeAndAfter}
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.extdata.region.{GeoRegion, Tree}
import ru.yandex.auto.vin.decoder.geo.{GeocoderClient, GeocoderManager}
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, PreparedDataState}
import ru.yandex.auto.vin.decoder.proto.VinHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.workers.WorkResult
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.auto.vin.decoder.wizard.YandexWizardManager
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageModel.{MetaData, OnlyPreparedModel, PreparedData}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.{IterableHasAsJava, ListHasAsScala}
import scala.language.implicitConversions

class FineLocationUnifyProcessorTest extends AnyWordSpecLike with Matchers with MockitoSupport with BeforeAndAfter {

  implicit private val t: Traced = Traced.empty
  private val vin = VinCode("X4X3D59430PS96744")
  private val rawStorageManager = mock[RawStorageManager[VinCode]]
  private val tree: Tree = mock[Tree]
  private val yaWizardManager = mock[YandexWizardManager]
  private val geocoderClient = mock[GeocoderClient]
  private val geocoderManager = new GeocoderManager(geocoderClient, tree)

  private val processor = new FineLocationUnifyProcessor(
    rawStorageManager,
    tree,
    yaWizardManager,
    geocoderManager,
    EventType.AUTOCODE_FINES
  )

  implicit private def toFuture[T](a: T): Future[T] = Future.successful(a)

  before {
    reset(rawStorageManager)
    reset(tree)
    reset(tree)
    reset(yaWizardManager)
  }

  "process" should {
    "do not update records" when {
      "there are no records in storage" in {

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Nil)

        val (state, defaultUpdate) = createState
        val res = processor.process(vin, state).await

        checkShouldProcess(is = false, res, defaultUpdate) // shouldProcess опустился
        verify(yaWizardManager, never()).extractLocations(?)(?) // обращений к wizard-у не было
        verify(rawStorageManager, never()).updatePrepared(?, ?, ?)(?) // апдейтов бд не было
      }

      "all locations are already processed" in {

        val fines = (0 to 5).map(_ => buildFine(unified = true)).toList
        val records = (0 to 5).map(_ => buildRecord(fines)).toList

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(records)

        val (state, defaultUpdate) = createState
        val res = processor.process(vin, state).await

        checkShouldProcess(is = false, res, defaultUpdate) // shouldProcess опустился
        verify(yaWizardManager, never()).extractLocations(?)(?) // обращений к wizard-у не было
        verify(rawStorageManager, never()).updatePrepared(?, ?, ?)(?) // апдейтов бд не было
      }
    }

    "update records and finish" when {
      "wizard successfully respond for all vendor names (even if response was empty)" in {

        val fine1 = buildFine(unified = false, vendorName = "one")
        val fine2 = buildFine(unified = false, vendorName = "two")
        val records = List(buildRecord(List(fine1, fine2)))
        val geoRegion = buildGeoRegion

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(records)
        when(yaWizardManager.extractLocations(eq(fine1.getVendorName))(?)).thenReturn(List(geoRegion))
        when(yaWizardManager.extractLocations(eq(fine2.getVendorName))(?)).thenReturn(Nil)
        when(tree.findUpByTree(eq(geoRegion.id), ?)).thenReturn(Some(geoRegion))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)

        val (state, defaultUpdate) = createState
        val res = processor.process(vin, state).await

        checkShouldProcess(is = false, res, defaultUpdate) // shouldProcess опустился
        verify(yaWizardManager, times(2)).extractLocations(?)(?) // wizard вызывался 2 раза
        // updatePrepared вызывался 1 раз, с правильным новым VinInfoHistory
        verify(rawStorageManager, times(1)).updatePrepared(?, captureFines(), ?)(?)
        checkCapturedFines(
          _.size shouldBe 2,
          _.head.getGeo.getRaw shouldBe fine1.getVendorName,
          _.head.getGeo.getGeobaseId shouldBe geoRegion.id,
          _.head.getGeoUnifyTimestamp shouldNot be(0),
          _(1).hasGeo shouldBe false,
          _(1).getGeoUnifyTimestamp shouldNot be(0)
        )
      }
    }

    "update records and reschedule" when {
      "wizard request failed on some vendor name" in {

        val fine1 = buildFine(unified = false, vendorName = "one")
        val fine2 = buildFine(unified = false, vendorName = "two")
        val records = List(buildRecord(List(fine1, fine2)))
        val geoRegion = buildGeoRegion

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(records)
        when(yaWizardManager.extractLocations(eq(fine1.getVendorName))(?)).thenReturn(List(geoRegion))
        when(yaWizardManager.extractLocations(eq(fine2.getVendorName))(?))
          .thenReturn(Future.failed(new RuntimeException))
        when(tree.findUpByTree(eq(geoRegion.id), ?)).thenReturn(Some(geoRegion))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)

        val (state, defaultUpdate) = createState
        val res = processor.process(vin, state).await

        checkShouldProcess(is = true, res, defaultUpdate) // shouldProcess остался поднят
        verify(yaWizardManager, times(2)).extractLocations(?)(?) // wizard вызывался 2 раза
        // updatePrepared вызывался 1 раз, с правильным новым VinInfoHistory
        verify(rawStorageManager, times(1)).updatePrepared(?, captureFines(), ?)(?)
        checkCapturedFines(
          _.size shouldBe 2,
          _.head.getGeo.getRaw shouldBe fine1.getVendorName,
          _.head.getGeo.getGeobaseId shouldBe geoRegion.id,
          _.head.getGeoUnifyTimestamp shouldNot be(0),
          _(1).getGeoUnifyTimestamp shouldBe 0
        )
      }

      "update failed" in {

        val fineNotUnified = buildFine(unified = false)
        val records = List(buildRecord(List(fineNotUnified)))
        val geoRegion = buildGeoRegion

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(records)
        when(yaWizardManager.extractLocations(eq(fineNotUnified.getVendorName))(?)).thenReturn(List(geoRegion))
        when(tree.findUpByTree(eq(geoRegion.id), ?)).thenReturn(Some(geoRegion))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.failed(new RuntimeException))

        val (state, defaultUpdate) = createState
        val res = processor.process(vin, state).await

        // shouldProcess остался поднят
        checkShouldProcess(is = true, res, defaultUpdate)
        verify(yaWizardManager, times(1)).extractLocations(?)(?) // wizard вызывался 1 раз
        // updatePrepared вызывался с правильным новым VinInfoHistory
        verify(rawStorageManager, times(1)).updatePrepared(?, captureFines(), ?)(?)
        checkCapturedFines(
          _.size shouldBe 1,
          _.head.getGeo.getRaw shouldBe fineNotUnified.getVendorName,
          _.head.getGeo.getGeobaseId shouldBe geoRegion.id,
          _.head.getGeoUnifyTimestamp shouldNot be(0)
        )
      }
    }

    "minimize wizard calls in case of equal fine vendor names" in {

      val fine1 = buildFine(unified = false, vendorName = "one")
      val fine2 = buildFine(unified = false, vendorName = "one")
      val fine3 = buildFine(unified = false, vendorName = "two")
      val records = List(buildRecord(List(fine1, fine2, fine3)))

      when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(records)
      when(yaWizardManager.extractLocations(eq(fine1.getVendorName))(?)).thenReturn(Nil)
      when(yaWizardManager.extractLocations(eq(fine3.getVendorName))(?)).thenReturn(Nil)
      when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)

      val (state, defaultUpdate) = createState
      val res = processor.process(vin, state).await

      checkShouldProcess(is = false, res, defaultUpdate) // shouldProcess опустился
      verify(yaWizardManager, times(2)).extractLocations(?)(?) // wizard вызывался 2 раза
      // updatePrepared вызывался 1 раз, с правильным новым VinInfoHistory
      verify(rawStorageManager, times(1)).updatePrepared(?, captureFines(), ?)(?)
      checkCapturedFines(
        _.size shouldBe 3,
        _.head.getGeoUnifyTimestamp shouldNot be(0),
        _(1).getGeoUnifyTimestamp shouldNot be(0),
        _(2).getGeoUnifyTimestamp shouldNot be(0)
      )
    }
  }

  private def createState: (PreparedDataState, WatchingStateUpdate[CompoundState]) = {
    val prepDataState = PreparedDataState.newBuilder
      .setEventType(EventType.AUTOCODE_FINES)
      .setShouldProcess(true)
      .build
    val compoundState = CompoundState.newBuilder
      .addPreparedDataState(prepDataState)
      .build
    val watchingStateUpdate = WatchingStateUpdate(compoundState, DefaultDelay(1.hour))
    (prepDataState, watchingStateUpdate)
  }

  private def buildRecord(fines: List[VinHistory.Fine]): OnlyPreparedModel[VinCode] = {
    OnlyPreparedModel(
      vin,
      PreparedData(buildVh(fines)),
      MetaData("", EventType.AUTOCODE_FINES, "", 123L, 123L, 123L)
    )
  }

  private def buildVh(fines: List[VinHistory.Fine]) = {
    VinInfoHistory.newBuilder
      .addAllFines(fines.asJava)
      .build
  }

  private def buildFine(unified: Boolean, vendorName: String = "don't care"): VinHistory.Fine = {
    val fineB = VinHistory.Fine.newBuilder
      .setVendorName(vendorName)
    if (unified) {
      fineB
        .setGeoUnifyTimestamp(1)
        .setGeo(VinHistory.Geo.newBuilder.build)
    }
    fineB.build
  }

  private def buildGeoRegion: GeoRegion = {
    GeoRegion(1L, "", 0L, "", "", "", 0, "", "", 0)
  }

  private def buildGeo(raw: String): VinHistory.Geo = {
    VinHistory.Geo.newBuilder
      .setGeobaseId(1L)
      .setRaw(raw)
      .build
  }

  private val vihCaptured: ArgumentCaptor[VinInfoHistory] = ArgumentCaptor.forClass(classOf[VinInfoHistory])

  private def captureFines(): VinInfoHistory = {
    vihCaptured.capture()
  }

  private def checkShouldProcess(
      is: Boolean,
      workRes: WorkResult[CompoundState],
      defaultUpd: WatchingStateUpdate[CompoundState]) = {
    workRes.updater.nonEmpty shouldBe true
    val updated = workRes.updater.get.apply(defaultUpd)
    updated.state.getPreparedDataStateCount shouldBe 1
    updated.state.getPreparedDataState(0).getShouldProcess shouldBe is
  }

  private def checkCapturedFines(finesCheck: (List[VinHistory.Fine] => Assertion)*) = {
    val fines = vihCaptured.getValue.getFinesList.asScala.toList
    finesCheck.foreach(_(fines))
  }
}
