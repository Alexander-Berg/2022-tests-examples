package ru.yandex.vertis.vsquality.callgate.api.v1

import cats.effect.IO
import org.http4s._
import ru.yandex.vertis.vsquality.callgate.api.AbstractTasksApiSpec
import ru.yandex.vertis.vsquality.callgate.api.AbstractTasksApiSpec._
import ru.yandex.vertis.vsquality.callgate.api.v1.TasksApiSpec._
import ru.yandex.vertis.vsquality.callgate.generators.Arbitraries._
import ru.yandex.vertis.vsquality.callgate.converters.ProtoFormat._
import ru.yandex.vertis.vsquality.callgate.model.TaskDescriptor
import ru.yandex.vertis.vsquality.callgate.model.api.{ApiError, ApiRequest, ApiResponse, RequestContext}
import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoSyntax._
import ru.yandex.vertis.vsquality.utils.test_utils.MockitoUtil._

class TasksApiSpec extends AbstractTasksApiSpec {
  import jsonProtoSupport._

  "Tasks handlers /api/v1/tasks" should {

    "put Ok" in {
      forAll { (taskDescriptor: TaskDescriptor, request: ApiRequest.ApplyTaskResult) =>
        when(mockedTasksController.applyResult(taskDescriptor, ?)(RequestContext(?, ?)))
          .thenReturnOnly(IO.pure(ApiResponse.Empty))
        val actualResponse = runRequest(Method.PUT, tasksApiPathWithKey(taskDescriptor), request.toProtoMessage)
        checkEmptyOk(actualResponse)
      }
    }

    "put NotFound" in {
      forAll { (taskDescriptor: TaskDescriptor, request: ApiRequest.ApplyTaskResult) =>
        when(mockedTasksController.applyResult(taskDescriptor, ?)(RequestContext(?, ?)))
          .thenReturnOnly(IO.raiseError(ApiError.TaskNotFound(taskDescriptor)))
        val actualResponse = runRequest(Method.PUT, tasksApiPathWithKey(taskDescriptor), request.toProtoMessage)
        checkNotFound(taskDescriptor, actualResponse)
      }
    }

    "put InternalServerError" in {
      forAll { (taskDescriptor: TaskDescriptor, request: ApiRequest.ApplyTaskResult) =>
        when(mockedTasksController.applyResult(taskDescriptor, ?)(RequestContext(?, ?)))
          .thenReturnOnly(IO.raiseError(new RuntimeException(Some5xxErrorText)))
        val actualResponse = runRequest(Method.PUT, tasksApiPathWithKey(taskDescriptor), request.toProtoMessage)
        checkInternalServerError(actualResponse)
      }
    }

  }
}

object TasksApiSpec {

  private val TasksApiPath: String = "/api/v1/tasks"

  private def tasksApiPathWithKey(taskKey: TaskDescriptor): String = s"$TasksApiPath/$taskKey"

}
