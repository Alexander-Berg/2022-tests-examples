package amogus.logic.queue.deadletters

import amogus.model.errors.FailedRequest
import amogus.storage.pg.PgFailedRequestRepository
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie.Transactor
import amogus.model.ValueTypes.{RequestId, ServiceId}
import common.zio.uuid.UUID
import ru.yandex.vertis.amogus.amo_request.AmoRequest
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{Has, Task, URIO, ZIO, ZRef}
import doobie.implicits._
import zio.interop.catz._

object DeadLettersQueueSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = (suite("DeadLettersQueueSpec")(
    testM("count dead letters") {
      for {
        requestId <- UUID.randomUUID.map(RequestId(_))
        serviceId <- UUID.randomUUID.map(ServiceId(_))
        entity = FailedRequest(
          requestId = requestId,
          serviceId = serviceId,
          requestType = "create",
          amoRequestBinary = AmoRequest.defaultInstance,
          amoRequestText = "text",
          fullExceptionMessage = ""
        )
        _ <- DeadLettersQueue(_.write(entity))
        counter <- ZRef.make(0)
        count <- DeadLettersQueue(_.processCount((id, c) => counter.update(_ + c).when(serviceId == id)))
        result <- counter.get
      } yield assertTrue(result == 1)
    },
    testM("write request text correctly") {
      for {
        requestId <- UUID.randomUUID.map(RequestId(_))
        serviceId <- UUID.randomUUID.map(ServiceId(_))
        entity = FailedRequest(
          requestId = requestId,
          serviceId = serviceId,
          requestType = "update",
          amoRequestBinary = AmoRequest.defaultInstance,
          amoRequestText = "text",
          fullExceptionMessage = ""
        )
        _ <- DeadLettersQueue(_.write(entity))
        xa <- ZIO.service[Transactor[Task]]
        result <- sql"select amo_request_text from failed_request where request_type = 'update'"
          .query[String]
          .option
          .transact(xa)
      } yield assertTrue(result.contains("text"))
    }
  ) @@ beforeAll(dbInit) @@ sequential).provideCustomLayerShared(
    UUID.live ++ TestPostgresql.managedTransactor >+> PgFailedRequestRepository.live >+> DeadLettersQueueLive.live
  )

  private val dbInit: URIO[Has[doobie.Transactor[Task]], Unit] =
    ZIO
      .service[Transactor[Task]]
      .flatMap(InitSchema("/schema.sql", _))
      .orDie
}
