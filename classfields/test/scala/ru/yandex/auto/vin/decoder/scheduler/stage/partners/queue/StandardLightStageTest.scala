package ru.yandex.auto.vin.decoder.scheduler.stage.partners.queue

import org.mockito.Mockito._
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.cache.AudatexDealersCache
import ru.yandex.auto.vin.decoder.model.{AudatexDealers, VinCode}
import ru.yandex.auto.vin.decoder.partners.acat.Acat
import ru.yandex.auto.vin.decoder.partners.audatex.Audatex.AudatexPartner
import ru.yandex.auto.vin.decoder.partners.audatex.{AudatexAudaHistory, AudatexFraudCheck}
import ru.yandex.auto.vin.decoder.partners.autocode.MosAutocode
import ru.yandex.auto.vin.decoder.partners.avilon.Avilon
import ru.yandex.auto.vin.decoder.partners.avtonomer.Avtonomer
import ru.yandex.auto.vin.decoder.partners.bmw.BMW
import ru.yandex.auto.vin.decoder.partners.carprice.Carprice
import ru.yandex.auto.vin.decoder.partners.filter.Filter
import ru.yandex.auto.vin.decoder.partners.fitauto.Fitauto
import ru.yandex.auto.vin.decoder.partners.infiniti.Infiniti
import ru.yandex.auto.vin.decoder.partners.jlr.JLR
import ru.yandex.auto.vin.decoder.partners.mazda.Mazda
import ru.yandex.auto.vin.decoder.partners.mitsubishi.Mitsubishi
import ru.yandex.auto.vin.decoder.partners.nbki.NbkiPledges
import ru.yandex.auto.vin.decoder.partners.nissan.Nissan
import ru.yandex.auto.vin.decoder.partners.rusauto.RusAuto
import ru.yandex.auto.vin.decoder.partners.suzuki.{Suzuki, SuzukiVehicle}
import ru.yandex.auto.vin.decoder.partners.tradesoft.Tradesoft
import ru.yandex.auto.vin.decoder.partners.uremont.UremontMisc
import ru.yandex.auto.vin.decoder.partners.wilgood.Wilgood
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, StandardState}
import ru.yandex.auto.vin.decoder.scheduler.engine.ProcessingState
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.stage.CompoundStageSupport
import ru.yandex.auto.vin.decoder.scheduler.stage.StageSupport.createDefaultProcessingState
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.state.StandardPartnerState
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.duration._

class StandardLightStageTest
  extends AnyFunSuite
  with MockitoSupport
  with CompoundStageSupport[VinCode, StandardLightStage[VinCode]] {

  private val queue = mock[WorkersQueue[VinCode, CompoundState]]
  private val audatexCache = mock[AudatexDealersCache]
  private val vin = VinCode("X4X3D59430PS96744")
  when(queue.enqueue(?, ?)).thenReturn(true)
  when(queue.queueName).thenReturn("partner")
  when(audatexCache.get).thenReturn(AudatexDealers(List.empty[AudatexPartner]))

  private val standardStages = Set(
    Carprice,
    Filter,
    Wilgood,
    Fitauto,
    Mazda,
    RusAuto,
    Acat,
    NbkiPledges,
    MosAutocode,
    Nissan,
    Infiniti,
    Suzuki,
    SuzukiVehicle,
    BMW,
    Tradesoft,
    JLR,
    Avilon,
    UremontMisc,
    Mitsubishi,
    AudatexAudaHistory(audatexCache),
    AudatexFraudCheck(audatexCache),
    Avtonomer
  ).map(p => p -> StandardLightStage[VinCode](queue, p)).toMap

  def prepareState(
      partner: StandardPartnerState
    )(prepare: StandardState.Builder => StandardState.Builder): CompoundState = {
    val csB = CompoundState.newBuilder
    val s = prepare(partner.getState(csB.build).toBuilder).build
    partner.setState(csB.build(), s)
  }

  test("ignore with empty state") {
    def stageTest(stage: StandardLightStage[VinCode]) = {
      val b = CompoundState.newBuilder().build()
      val state = createProcessingState(b)
      assert(!stage.shouldProcess(state))
      verify(queue, never()).enqueue(?, ?)
    }
    standardStages.foreach(st => {
      stageTest(st._2)
    })
  }

  test("ignore if state without shouldProcess or forceUpdate") {
    def stageTest(
        stage: StandardLightStage[VinCode],
        compoundState: CompoundState) = {
      val state = createProcessingState(compoundState)
      assert(!stage.shouldProcess(state))
      verify(queue, never()).enqueue(?, ?)
    }

    standardStages.foreach(st => {
      val state = prepareState(st._1)(_.setShouldProcess(false))
      stageTest(st._2, state)
    })

  }

  test("ignore when empty state") {
    val b = CompoundState.newBuilder().build()
    val processingState = createDefaultProcessingState(b)
    checkIgnored(processingState)
  }

  test("ignore when last_check undefined") {
    def stageTest(cs: CompoundState) = {
      val processingState = createDefaultProcessingState(cs)
      checkIgnored(processingState)
    }

    standardStages.foreach(st => {
      val state = prepareState(st._1)(_.setLastCheck(0))
      stageTest(state)
    })
  }

  test("process if state with shouldProcess") {
    def stageTest(
        stage: StandardLightStage[VinCode],
        cs: CompoundState) = {

      val state = createProcessingState(cs)
      stage.processWithAsync(vin, state)
      assert(stage.shouldProcess(state))
    }

    standardStages.foreach(st => {
      val state = prepareState(st._1)(_.setShouldProcess(true))
      stageTest(st._2, state)
    })
    verify(queue, times(standardStages.size)).enqueue(?, ?)
  }

  private def createProcessingState(compoundState: CompoundState): ProcessingState[CompoundState] = {
    ProcessingState(WatchingStateUpdate(compoundState, DefaultDelay(25.hours)))
  }

  override def createProcessingStage(): StandardLightStage[VinCode] = new StandardLightStage(queue) {
    override def shouldProcess(processingState: ProcessingState[CompoundState]): Boolean = false
  }
}
