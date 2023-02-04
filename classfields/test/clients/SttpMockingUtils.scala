package auto.dealers.match_maker.logic.clients

import ru.yandex.vertis.mockito.MockitoSupport._
import common.zio.sttp.{Sttp, SttpLive}
import common.zio.sttp.Sttp.ZioSttpBackendStub
import sttp.client3.Response
import sttp.model.StatusCode
import zio.test.{Spec, TestFailure, TestSuccess, ZSpec}
import zio.{Has, Layer, Task, UIO, ZIO}

import scala.collection.immutable

object SttpMockingUtils {

  def withSttpBackendProvided[E, R](
      getClient: Sttp.Service => Layer[TestFailure[E], Has[R]]
    )(test: ZioSttpBackendStub => ZSpec[Has[R], E]): Spec[Any, TestFailure[E], TestSuccess] = {
    val sttpBack = mock[ZioSttpBackendStub]
    val sttp = new SttpLive(sttpBack)

    test(sttpBack).provideLayer(getClient(sttp))
  }

  def mockSttpSend[A](sttpBackend: ZioSttpBackendStub, response: Response[A]): Task[Unit] =
    ZIO.effect(when(sttpBackend.send[A, Any](?)).thenReturn(Task(response))).unit

  def createDefaultResponse[T](entity: T, statusCode: StatusCode, statusText: String): UIO[Response[T]] =
    ZIO.effectTotal {
      new Response[T](
        entity,
        statusCode,
        statusText,
        headers = immutable.Seq.empty,
        history = List.empty,
        Response.ExampleGet
      )
    }
}
