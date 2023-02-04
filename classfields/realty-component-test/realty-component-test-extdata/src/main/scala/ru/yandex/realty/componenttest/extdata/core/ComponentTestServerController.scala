package ru.yandex.realty.componenttest.extdata.core

import ru.yandex.extdata.core.event.impl.DelayedEventListener
import ru.yandex.extdata.core.event.{Event, EventListener, Notifier}
import ru.yandex.extdata.core.service.{Dispatcher, ExtDataService}
import ru.yandex.extdata.core.{DataType, Producer, ServerController, TaskId}
import ru.yandex.realty.clients.resource.SlaveController
import ru.yandex.realty.loaders.{AbstractFetcher, AbstractProvider}
import ru.yandex.realty.logging.Logging

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext

class ComponentTestServerController(
  slaveController: SlaveController,
  listenerContainer: DelayedEventListener,
  dispatcher: Dispatcher,
  val extDataService: ExtDataService
)(implicit ec: ExecutionContext)
  extends ServerController
  with Logging {

  private val registeredProviders: ArrayBuffer[AbstractProvider] = ArrayBuffer.empty[AbstractProvider]
  private val registeredFetchers: ArrayBuffer[AbstractFetcher] = ArrayBuffer.empty[AbstractFetcher]

  override def start(): Unit = {
    log.warn(s"Starting component test controller")
    slaveController.start()
    extDataService.start()
    new Notifier(this).trigger()
    listenerContainer.start()
  }

  override def close(): Unit = {
    log.warn(s"Closing component test controller")
    extDataService.close()
    slaveController.stop()
    dispatcher.shutdown()
  }

  override def replicate(dataType: DataType): Unit = {
    log.info(s"Replicating $dataType")
    slaveController.replicate(dataType)
  }

  override def register(listener: EventListener): Unit = {
    listener match {
      case p: AbstractProvider => registeredProviders += p
      case f: AbstractFetcher => registeredFetchers += f
      case _ => // nothing
    }
    listenerContainer.register(listener)
  }

  override def onEvent(e: Event): Unit = {
    listenerContainer.onEvent(e)
  }

  override def dispatch(id: TaskId, weight: Int, payload: () => Unit): Unit = {
    dispatcher.dispatch(id, weight, payload)
  }

  def providers(): Seq[AbstractProvider] =
    registeredProviders.toList

  override def fetch(dataType: DataType): Unit = {}

  override def produce(dataType: DataType, producer: Producer): Unit = {}

}
