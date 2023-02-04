package ru.yandex.vertis.vsquality.hobo.service.impl.mysql

import org.mockito.Mockito
import ru.yandex.vertis.vsquality.hobo.model.{User, UserId}
import ru.yandex.vertis.vsquality.hobo.service.{ReadUserSupport, RequestContext, TaskService, TaskServiceSpecBase}
import ru.yandex.vertis.vsquality.hobo.util.MySqlSpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Specs on [[MySqlTaskService]]
  *
  * @author semkagtn
  */

class MySqlTaskServiceSpec extends TaskServiceSpecBase with MySqlSpecBase {

  override val taskService: TaskService = {
    val readUserSupport = mock[ReadUserSupport]
    Mockito
      .when(readUserSupport.get(any[UserId])(any[RequestContext]))
      .thenReturn(Future.failed(new IllegalArgumentException))
    new MySqlTaskService(
      database,
      maxBatchSize = 100,
      needSaveHistory = true,
      taskFactory = taskFactory,
      onCompleteActionsFactory = onCompleteActionsFactory,
      readUserSupport = readUserSupport
    )
  }
}
