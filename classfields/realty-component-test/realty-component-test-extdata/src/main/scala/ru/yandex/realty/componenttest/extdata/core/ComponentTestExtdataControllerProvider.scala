package ru.yandex.realty.componenttest.extdata.core

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import ru.yandex.common.monitoring.HealthChecks
import ru.yandex.extdata.core.actor.DispatcherActor
import ru.yandex.extdata.core.actor.util.DispatcherLoggerActor
import ru.yandex.extdata.core.logging.LoggingDispatcher
import ru.yandex.extdata.core.monitored.ExpiringWarningReservoirConfig
import ru.yandex.extdata.core.service.impl.DelegateDispatcher
import ru.yandex.extdata.core.task.TaskWrapper
import ru.yandex.realty.application.extdata.ExtdataControllerProvider
import ru.yandex.realty.clients.resource.SlaveController
import ru.yandex.realty.component.{ResourceServiceClientConfig, ResourceServiceClientSupplier}
import ru.yandex.realty.extdata.{Barrier, BarrierListener, ClientDataSpec}
import ru.yandex.vertis.util.concurrent.Threads

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait ComponentTestExtdataControllerProvider
  extends ExtdataControllerProvider
  with ResourceServiceClientSupplier
  with ComponentTestExtdataServiceProvider {

  private val DefaultTestPoolSize = 4

  implicit val testEc: ExecutionContext =
    Threads.newForkJoinPoolExecutionContext(DefaultTestPoolSize, s"${getClass.getSimpleName}-ec")

  override lazy val resourceServiceClientConfig: ResourceServiceClientConfig =
    ResourceServiceClientConfig(
      url = extdataClientConfig.remoteUrl,
      maxConcurrent = 10
    )

  override lazy val controller: ComponentTestServerController = {
    val wrapper = TaskWrapper.empty

    val name = "component-test-extdata"
    val system = ActorSystem(name, ConfigFactory.empty())

    val dispatcher =
      system.actorOf(
        DispatcherActor.props(1000, 10),
        "extdata-dispatcher"
      )

    system.actorOf(
      DispatcherLoggerActor.props(dispatcher, 5.seconds),
      "extdata-dispatcher-logger"
    )

    val slaveController =
      new SlaveController(
        resourceServiceClient,
        extDataService,
        10,
        10.minutes,
        1.minute,
        20.minutes,
        40.minute,
        ExpiringWarningReservoirConfig(20, 40, 15.minutes, 1000),
        wrapper.process,
        HealthChecks.compoundRegistry(),
        name,
        None
      )(system, testEc)

    val dispatcherService =
      new DelegateDispatcher(dispatcher, 10, name) with LoggingDispatcher

    new ComponentTestServerController(slaveController, listener, dispatcherService, extDataService)
  }

  def start(): Unit = {
    val barrier = new Barrier

    val providers = controller.providers()
    controller.register(new BarrierListener(barrier))

    providers.foreach(p => {
      barrier.register(p.updatedEvent)

      p.dependencies.diff(controller.extDataService.specs.map(_.dataType).toSet).foreach { dt =>
        controller.extDataService.register(ClientDataSpec(dt))
      }
    })

    controller.start()

    barrier.awaitTermination(360.seconds)
  }

}
