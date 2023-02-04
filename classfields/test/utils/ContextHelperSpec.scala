package vertis.palma.utils

import common.zio.ops.tracing.Tracing
import common.zio.ops.tracing.Tracing.Tracing
import io.opentracing.SpanContext
import ru.yandex.vertis.palma.service_common.{RequestContext => ProtoRequestContext}
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.palma.service.model.RequestContext
import vertis.zio.test.ZioSpecBase
import zio.{Task, ZIO}

class ContextHelperSpec extends ZioSpecBase {

  private def clearEnv: ZIO[Tracing, Nothing, Unit] = Tracing.setSpan(None)

  private def isolatedIoTest(body: => TestBody): Unit =
    ioTest(for {
      _ <- clearEnv
      res <- body
      _ <- clearEnv
    } yield res)

  private def provideRequestId(requestId: String): ZIO[Tracing, Throwable, Unit] =
    for {
      span <- Task {
        new NoopSpan {
          override def context(): SpanContext = new NoopSpanContext {
            override def toTraceId: String = requestId
          }
        }
      }
      _ <- Tracing.setSpan(Some(span))
    } yield ()

  "ContextHelper" should {
    "provide empty ctx" in isolatedIoTest {
      for {
        ctx <- ContextHelper.extractContext(None, None)
        _ <- check(ctx shouldBe RequestContext.Empty)
      } yield ()
    }

    "provide correct ctx" in isolatedIoTest {
      for {
        _ <- provideRequestId("request_2")
        ctx <- ContextHelper.extractContext(Some(ProtoRequestContext("user_1", "v1")), Some("palma-controller"))
        _ <- check(
          ctx shouldBe RequestContext(
            userId = Some("user_1"),
            schemaVersion = Some("v1"),
            serviceName = Some("palma-controller"),
            requestId = Some("request_2")
          )
        )
      } yield ()
    }

    "fill service name for empty proto" in isolatedIoTest {
      for {
        ctx <- ContextHelper.extractContext(None, Some("palma-www"))
        _ <- check(ctx shouldBe RequestContext.Empty.copy(serviceName = Some("palma-www")))
      } yield ()
    }

    "fill request id from env for empty proto" in isolatedIoTest {
      for {
        _ <- provideRequestId("request_1")
        ctx <- ContextHelper.extractContext(None, None)
        _ <- check(ctx shouldBe RequestContext.Empty.copy(requestId = Some("request_1")))
      } yield ()
    }

  }

}
