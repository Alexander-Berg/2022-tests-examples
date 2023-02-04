package vsquality.complaints.storage.test

import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.test.TestAspect._
import zio.{Task, ZIO}
import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import doobie.util.transactor.Transactor
import vsquality.complaints.storage.QueueDao
import vsquality.complaints.model._
import vsquality.complaints.model.types.OfferId
import vsquality.complaints.storage.postgresql.PgQueueDao
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.util.Random

object PgQueueDaoSpec extends DefaultRunnableSpec {

  def createRecord(offerId: OfferId, status: Status, timestamp: Instant) =
    QueueRecord(
      id = Random.nextLong().toString,
      offerId = offerId,
      offerOwnerId = Random.nextLong().toString,
      complainantId = Random.nextLong().toString,
      domain = Domain.REALTY,
      status = status,
      processAfter = timestamp,
      updatedAt = timestamp
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgQueueDao")(
      testM("create and get record") {
        for {
          dao <- ZIO.service[QueueDao.Service]
          timestamp = Instant.now.truncatedTo(ChronoUnit.SECONDS)
          r1        = createRecord("1", Status.AWAIT, timestamp)
          _   <- dao.insert(r1).transactIO
          res <- dao.getRecord(r1.offerId, r1.domain).transactIO
        } yield assertTrue(res == r1)
      },
      testM("update record") {
        for {
          dao <- ZIO.service[QueueDao.Service]
          timestamp = Instant.now.truncatedTo(ChronoUnit.SECONDS)
          orig      = createRecord("2", Status.AWAIT, timestamp)
          updated = orig.copy(
            status = Status.COOL_DOWN,
            updatedAt = timestamp,
            processAfter = timestamp
          )
          _   <- dao.insert(orig).transactIO
          -   <- dao.update(updated).transactIO
          res <- dao.getRecord(orig.offerId, orig.domain).transactIO
        } yield assertTrue(res == updated)
      },
      testM("check if offer is in queue") {
        for {
          dao          <- ZIO.service[QueueDao.Service]
          isInQueue    <- dao.isOfferInQueue("2", Domain.REALTY).transactIO
          isNotInQueue <- dao.isOfferInQueue("3", Domain.REALTY).transactIO
        } yield assertTrue(isInQueue && !isNotInQueue)
      },
      testM("clear queue from record with COOL DOWN status") {
        for {
          dao <- ZIO.service[QueueDao.Service]
          timestamp = Instant.now.truncatedTo(ChronoUnit.SECONDS)
          _       <- dao.insert(createRecord("4", Status.IN_PROGRESS, timestamp)).transactIO
          _       <- dao.insert(createRecord("5", Status.IN_PROGRESS, timestamp)).transactIO
          _       <- dao.insert(createRecord("6", Status.IN_PROGRESS, timestamp)).transactIO
          records <- dao.getRecordsInStatus(Status.IN_PROGRESS, timestamp).transactIO
          deleted <- dao.clear(Status.IN_PROGRESS, Instant.now.plusSeconds(60)).transactIO
          empty   <- dao.getRecordsInStatus(Status.IN_PROGRESS, timestamp).transactIO
        } yield assertTrue(records == deleted && empty.isEmpty)
      },
      testM("get queue metrics") {
        for {
          dao <- ZIO.service[QueueDao.Service]
          timestamp = Instant.now.truncatedTo(ChronoUnit.SECONDS)
          _       <- dao.insert(createRecord("4", Status.IN_PROGRESS, timestamp)).transactIO
          _       <- dao.insert(createRecord("5", Status.IN_PROGRESS, timestamp)).transactIO
          _       <- dao.insert(createRecord("6", Status.COOL_DOWN, timestamp)).transactIO
          _       <- dao.insert(createRecord("7", Status.IN_PROGRESS, timestamp)).transactIO
          records <- dao.getQueueMetrics().transactIO
        } yield assertTrue(records.size == 3)
      }
    ) @@ sequential)
      .provideCustomLayerShared(TestPostgresql.managedTransactor >+> PgQueueDao.live)
  }
}
