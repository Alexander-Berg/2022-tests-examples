package infra.feature_toggles.client.test

import com.google.protobuf.empty.Empty
import common.zio.grpc.client.GrpcClient
import common.zio.grpc.client.GrpcClientLive
import ru.yandex.vertis.infra.feature_toggles.api._
import io.grpc.stub.StreamObserver
import infra.feature_toggles.client.{Feature, FeatureTogglesClient}
import common.zio.logging.Logging
import zio.clock.Clock
import zio.duration.durationInt
import zio.macros.accessible
import zio.{Has, IO, Ref, Schedule, ZIO, ZLayer}
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}

import scala.concurrent.Future

object FeatureTogglesClientSpec extends DefaultRunnableSpec {
  val service = "service"

  private def hasSameElementsInOrderAs[T](result: Seq[T])(expected: Seq[T]) = {
    assertTrue(result.size == expected.size) &&
    assertTrue(result.zip(expected).forall { case (k, v) => k == v })
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("FeatureTogglesClient")(
      testM("Подхватывает изменения") {
        val key = "key"
        val ft = Feature[String](key, "foo")
        for {
          value1 <- FeatureTogglesClient.get(ft)
          _ <- TestFeatureTogglesService.updateWatchResponse(
            WatchResponse(Seq(FeatureToggle(key, value = Some(Value(Value.Value.String("bar"))))))
          )
          _ <- ZIO.sleep(100.milliseconds)
          value2 <- FeatureTogglesClient.get(ft)
          _ <- TestFeatureTogglesService.updateWatchResponse(
            WatchResponse(Seq(FeatureToggle(key, value = Some(Value(Value.Value.String("baz"))))))
          )
          _ <- ZIO.sleep(100.milliseconds)
          value3 <- FeatureTogglesClient.get(ft)
        } yield hasSameElementsInOrderAs(Seq(value1, value2, value3))(Seq("foo", "bar", "baz"))
      },
      testM("Поддерживает boolean") {
        val key = "bool"
        val ft = Feature[Boolean](key, true)
        for {
          value1 <- FeatureTogglesClient.get(ft)
          _ <- TestFeatureTogglesService.updateWatchResponse(
            WatchResponse(Seq(FeatureToggle(key, value = Some(Value(Value.Value.Boolean(false))))))
          )
          _ <- ZIO.sleep(100.milliseconds)
          value2 <- FeatureTogglesClient.get(ft)
          _ <- TestFeatureTogglesService.updateWatchResponse(
            WatchResponse(Seq(FeatureToggle(key, value = Some(Value(Value.Value.Boolean(true))))))
          )
          _ <- ZIO.sleep(100.milliseconds)
          value3 <- FeatureTogglesClient.get(ft)
        } yield hasSameElementsInOrderAs(Seq(value1, value2, value3))(Seq(true, false, true))
      },
      testM("Поддерживает int64") {
        val key = "int64"
        val ft = Feature[Long](key, 42L)
        for {
          value1 <- FeatureTogglesClient.get(ft)
          _ <- TestFeatureTogglesService.updateWatchResponse(
            WatchResponse(Seq(FeatureToggle(key, value = Some(Value(Value.Value.Int64(Long.MinValue))))))
          )
          _ <- ZIO.sleep(100.milliseconds)
          value2 <- FeatureTogglesClient.get(ft)
          _ <- TestFeatureTogglesService.updateWatchResponse(
            WatchResponse(Seq(FeatureToggle(key, value = Some(Value(Value.Value.Int64(Long.MaxValue))))))
          )
          _ <- ZIO.sleep(100.milliseconds)
          value3 <- FeatureTogglesClient.get(ft)
        } yield hasSameElementsInOrderAs(Seq(value1, value2, value3))(Seq(42L, Long.MinValue, Long.MaxValue))
      }
    ).provideCustomLayer {
      val clock = Clock.live
      val testFeatureTogglesService = clock >>> TestFeatureTogglesService.live
      val featureTogglesClient =
        (clock ++ Logging.live ++ testFeatureTogglesService) >>> FeatureTogglesClient.withCustomClient(service)
      clock ++ featureTogglesClient ++ testFeatureTogglesService
    }
  }

  @accessible
  object TestFeatureTogglesService {

    trait Service {
      def updateWatchResponse(watchResponse: WatchResponse): IO[Nothing, Unit]
    }

    val live: ZLayer[Clock, Nothing, GrpcClient.GrpcClient[FeatureTogglesGrpc.FeatureToggles] with Has[Service]] = {
      val effect = for {
        watchResponseRef <- Ref.make[WatchResponse](WatchResponse())
        rt <- ZIO.runtime[Clock]
        client: GrpcClient.Service[FeatureTogglesGrpc.FeatureToggles] =
          new GrpcClientLive[FeatureTogglesGrpc.FeatureToggles](
            new FeatureTogglesImpl(rt, watchResponseRef),
            null
          )
        service: Service = new ServiceImpl(watchResponseRef)
      } yield Has.allOf(client, service)
      effect.toLayerMany
    }

    private class ServiceImpl(watchResponseRef: Ref[WatchResponse]) extends Service {

      override def updateWatchResponse(watchResponse: WatchResponse): IO[Nothing, Unit] = {
        watchResponseRef.set(watchResponse)
      }
    }

    private class FeatureTogglesImpl(runtime: zio.Runtime[Clock], watchResponseRef: Ref[WatchResponse])
      extends FeatureTogglesGrpc.FeatureToggles {

      override def get(request: GetRequest): Future[GetResponse] = ???

      override def list(request: ListRequest): Future[ListResponse] = ???

      override def listAll(request: ListAllRequest): Future[ListAllResponse] = ???

      override def set(request: SetRequest): Future[Empty] = ???

      override def delete(request: DeleteRequest): Future[Empty] = ???

      override def watch(request: WatchRequest, responseObserver: StreamObserver[WatchResponse]): Unit = {
        val _ = runtime.unsafeRunToFuture {
          watchResponseRef.get
            .flatMap(current => ZIO.effectTotal(responseObserver.onNext(current)))
            .repeat(Schedule.fixed(100.milliseconds))
        }
      }

      override def history(request: HistoryRequest): Future[HistoryResponse] = ???

      override def featureHistory(request: FeatureHistoryRequest): Future[HistoryResponse] = ???

      override def reportTelemetry(request: ReportTelemetryRequest): Future[Empty] = ???
    }

  }
}
