package ru.yandex.vertis.vsquality.callgate.api

import cats.data.Kleisli
import cats.effect.{Blocker, IO}
import org.http4s._
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import ru.yandex.vertis.vsquality.callgate.Globals
import ru.yandex.vertis.vsquality.callgate.api.service.CleanWebConverterImpl
import ru.yandex.vertis.vsquality.callgate.api.swagger.SwaggerUiVersionExtractor
import ru.yandex.vertis.vsquality.callgate.model.TaskDescriptor
import ru.yandex.vertis.vsquality.callgate.model.api.ApiResponse
import ru.yandex.vertis.vsquality.callgate.util.JsonProtoSupport
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase
import ru.yandex.vertis.vsquality.utils.tvm_utils.TvmTicketProvider
import ru.yandex.vertis.vsquality.utils.tvm_utils.TvmTicketProvider._

import scala.concurrent.ExecutionContext.Implicits.global

abstract class AbstractTasksApiSpec extends SpecBase {

  protected val jsonProtoSupport: JsonProtoSupport[IO] = JsonProtoSupport[IO]

  protected val mockedTasksController: ApiTasksController[IO] = mock[ApiTasksController[IO]]
  protected val mockedTvmTicketProvider: TvmTicketProvider[IO] = mock[TvmTicketProvider[IO]]

  implicit protected val httpApp: HttpApp[IO] = {
    val swaggerVersion = SwaggerUiVersionExtractor.extract.unsafeRunSync()
    swaggerVersion shouldNot be(empty)
    new RoutesBuilder(
      mockedTasksController,
      mockedTvmTicketProvider,
      new CleanWebConverterImpl[F],
      Blocker.liftExecutionContext(global),
      swaggerVersion
    ).httpApp
  }
}

object AbstractTasksApiSpec {

  val Some5xxErrorText: String = "Some 5xx error"

  def check[A](
      actual: IO[Response[IO]],
      expectedStatus: Status,
      expectedBody: Option[A]
    )(implicit ev: EntityDecoder[IO, A]): Boolean = {
    val exceptionHandler = new DomainExceptionHandler[IO].handleError
    val actualResp =
      actual.handleErrorWith { e =>
        if (exceptionHandler.isDefinedAt(e)) exceptionHandler(e)
        else IO.raiseError(new RuntimeException(s"Unknown exception", e))
      }.await
    val statusCheck = actualResp.status == expectedStatus
    val bodyCheck =
      expectedBody.fold(actualResp.body.compile.toVector.await.isEmpty) { expected =>
        actualResp.as[A].await == expected
      }
    statusCheck && bodyCheck
  }

  def checkNotFound(
      taskKey: TaskDescriptor,
      actual: IO[Response[IO]]
    )(implicit ev: EntityDecoder[IO, ApiResponse.Error]): Assertion =
    check(actual, Status.NotFound, Some(ApiResponse.Error(taskNotFoundText(taskKey)))) shouldBe true

  def checkInternalServerError(actual: IO[Response[IO]])(implicit ev: EntityDecoder[IO, ApiResponse.Error]): Assertion =
    check(actual, Status.InternalServerError, Some(ApiResponse.Error(Some5xxErrorText))) shouldBe true

  def checkForbiddenError(
      actual: IO[Response[IO]],
      msg: String
    )(implicit ev: EntityDecoder[IO, ApiResponse.Error]): Assertion =
    check(actual, Status.Forbidden, Some(msg)) shouldBe true

  def checkEmptyOk(actual: IO[Response[IO]])(implicit ev: EntityDecoder[IO, ApiResponse.Empty.type]): Assertion =
    check(actual, Status.Ok, Some(ApiResponse.Empty)) shouldBe true

  def uri(s: String): Uri = Uri.fromString(s).toOption.get

  def taskNotFoundText(taskKey: TaskDescriptor): String = s"Task [$taskKey] not found"

  def runRequest[T](
      method: Method,
      path: String,
      entity: T,
      headers: Header*
    )(implicit httpApp: Kleisli[IO, Request[IO], Response[IO]],
      ev: EntityEncoder[IO, T]): IO[Response[IO]] =
    httpApp.run(
      Request(method, uri(path))
        .withHeaders(headers: _*)
        .withEntity(entity)
    )

  def runRequestWithTvmHeader[T](
      method: Method,
      path: String,
      entity: T,
      tvmTicket: Globals.Header
    )(implicit httpApp: Kleisli[IO, Request[IO], Response[IO]],
      ev: EntityEncoder[IO, T]): IO[Response[IO]] = runRequest(method, path, entity, Header(TvmTicketHeader, tvmTicket))
}
