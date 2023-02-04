package ru.yandex.vertis.moderation.api.v1.service.scheduler

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.api.SchedulerHandlerSpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model.Service

/**
  * Spec for service specific scheduler API
  *
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class ServiceSchedulerHandlerSpec extends SchedulerHandlerSpecBase {

  private val service: Service = ServiceGen.next

  override def basePath: String = s"/api/1.x/$service/task"

  override protected def schedulerService: SchedulerService = environmentRegistry(service).schedulerService()
}
