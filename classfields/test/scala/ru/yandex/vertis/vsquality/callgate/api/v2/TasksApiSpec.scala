package ru.yandex.vertis.vsquality.callgate.api.v2

import cats.effect.IO
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import ru.yandex.vertis.vsquality.callgate.Globals
import ru.yandex.vertis.vsquality.callgate.api.AbstractTasksApiSpec._
import ru.yandex.vertis.vsquality.callgate.api.v2.TasksApiSpec._
import ru.yandex.vertis.vsquality.callgate.api.{AbstractTasksApiSpec, TvmTicketException}
import ru.yandex.vertis.vsquality.callgate.converters.CleanWebFormat._
import ru.yandex.vertis.vsquality.callgate.converters.ProtoFormat._
import ru.yandex.vertis.vsquality.callgate.generators.Arbitraries._
import ru.yandex.vertis.vsquality.callgate.model.TaskDescriptor
import ru.yandex.vertis.vsquality.callgate.model.api.{ApiError, ApiRequestV2, ApiResponse, RequestContext}
import ru.yandex.vertis.hobo.proto.model.QueueId
import ru.yandex.vertis.vsquality.utils.test_utils.MockitoUtil._

class TasksApiSpec extends AbstractTasksApiSpec {
  import jsonProtoSupport._

  private val taskDescriptor = TaskDescriptor(QueueId.GENERAL_CALLGATE_CLEAN_WEB, "singleton_verdict_key")

  "Tasks handlers /api/v2/tasks" should {

    "put Ok" in {
      val tvmTicket = generate[Globals.Header]
      when(mockedTasksController.applyCleanWebResult(taskDescriptor, ?)(RequestContext(?, ?)))
        .thenReturnOnly(IO.pure(ApiResponse.Empty))
      when(mockedTvmTicketProvider.checkTvmTicket(eq(tvmTicket)))
        .thenReturn(IO.pure(true))
      val actualResponse =
        runRequestWithTvmHeader(
          Method.POST,
          CleanWebPath,
          generate[ApiRequestV2.CleanWebApplyTaskResult],
          tvmTicket: Globals.Header
        )
      checkEmptyOk(actualResponse)
    }

    "put NotFound" in {
      val tvmTicket = generate[Globals.Header]
      when(mockedTasksController.applyCleanWebResult(taskDescriptor, ?)(RequestContext(?, ?)))
        .thenReturnOnly(IO.raiseError(ApiError.TaskNotFound(taskDescriptor)))
      when(mockedTvmTicketProvider.checkTvmTicket(eq(tvmTicket)))
        .thenReturn(IO.pure(true))
      val actualResponse =
        runRequestWithTvmHeader(Method.POST, CleanWebPath, generate[ApiRequestV2.CleanWebApplyTaskResult], tvmTicket)
      checkNotFound(taskDescriptor, actualResponse)
    }

    "put InternalServerError" in {
      val tvmTicket = generate[Globals.Header]
      when(mockedTasksController.applyCleanWebResult(taskDescriptor, ?)(RequestContext(?, ?)))
        .thenReturnOnly(IO.raiseError(new RuntimeException(Some5xxErrorText)))
      when(mockedTvmTicketProvider.checkTvmTicket(eq(tvmTicket)))
        .thenReturn(IO.pure(true))
      val actualResponse =
        runRequestWithTvmHeader(Method.POST, CleanWebPath, generate[ApiRequestV2.CleanWebApplyTaskResult], tvmTicket)
      checkInternalServerError(actualResponse)
    }

    "put Forbidden if no tvm header" in {
      when(mockedTasksController.applyCleanWebResult(taskDescriptor, ?)(RequestContext(?, ?)))
        .thenReturnOnly(IO.raiseError(new RuntimeException(Some5xxErrorText)))
      val actualResponse = runRequest(Method.POST, CleanWebPath, generate[ApiRequestV2.CleanWebApplyTaskResult])
      checkForbiddenError(actualResponse, TvmTicketException.NoHeader.message)
    }

    "put Forbidden if tvm header is invalid" in {
      val tvmTicket = generate[Globals.Header]
      when(mockedTvmTicketProvider.checkTvmTicket(eq(tvmTicket)))
        .thenReturn(IO.pure(false))
      val actualResponse =
        runRequestWithTvmHeader(
          Method.POST,
          CleanWebPath,
          generate[ApiRequestV2.CleanWebApplyTaskResult],
          tvmTicket: Globals.Header
        )
      checkForbiddenError(actualResponse, TvmTicketException.Invalid.message)
    }
  }
}

object TasksApiSpec {

  private val TasksApiPath: String = "/api/v2/tasks"

  private val CleanWebPath: String = s"$TasksApiPath/clean_web"
}
