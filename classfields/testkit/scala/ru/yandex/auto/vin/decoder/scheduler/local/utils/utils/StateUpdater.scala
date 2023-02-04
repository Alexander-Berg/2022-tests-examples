package ru.yandex.auto.vin.decoder.scheduler.local.utils

import ru.yandex.auto.vin.decoder.components.DefaultCoreComponents
import ru.yandex.auto.vin.decoder.extdata.IO
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.scheduler.models.{WatchingStateHolder, WatchingStateUpdate}

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

object StateUpdater extends LocalScript {

  /* PARAMS */

  private val vins =
    IO.using(Source.fromFile("/home/plomovtsev/Desktop/vins_to_clear_state"))(_.getLines().toList.map(VinCode.apply))
  private val updateDelaySeconds = 10
  private val batchSize = 20
  override protected val itemsWord: String = "vins"

  private def updateState(state: WatchingStateUpdate[CompoundState]): WatchingStateUpdate[CompoundState] = {
    state.withZeroDelay
  }

  /* DEPENDENCIES */

  private val coreComponents = new DefaultCoreComponents()

  override def action: Future[Any] = {
    pause(updateDelaySeconds, s"Start updating states for ${vins.size} vins")
    progressBar.start(totalItems = vins.size)
    updateStates(vins)
    Future.unit
  }

  private def updateStates(vins: Seq[VinCode]): Unit = {
    vins.grouped(batchSize).toList.par.foreach { vins =>
      try {
        val updatesByVin = vins
          .map(_ -> { holder: WatchingStateHolder[VinCode, CompoundState] =>
            Some(updateState(holder.toUpdate))
          })
          .toMap
        coreComponents.vinUpdateService.batchUpsertUpdate(vins.toSet)(updatesByVin)
        progressBar.inc(vins.size)
      } catch {
        case e: Exception =>
          println(s"ERROR FOR VINS ${vins.mkString(", ")}")
          println(e.getMessage)
      }
    }
  }
}
