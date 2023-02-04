package ru.yandex.vertis.moderation.api.v1.service

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.api.SchedulerHandlerSpecBase
import ru.yandex.vertis.moderation.api.v1.service.scheduler.SchedulerService

/**
  * Spec for common scheduler API
  *
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class CommonSchedulerHandlerSpec extends SchedulerHandlerSpecBase {

  override protected def schedulerService: SchedulerService = sharedEnvironment.schedulerService

  override def basePath: String = "/api/1.x/task"
}
