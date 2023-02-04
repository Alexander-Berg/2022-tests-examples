package vertis.spamalot.dao

import java.time.Instant
import common.zio.logging.Logging
import com.yandex.ydb.table.transaction.TransactionMode
import ru.yandex.vertis.spamalot.inner.{OperationPayload, StoredNotification}
import ru.yandex.vertis.ydb.Ydb.ops._
import ru.yandex.vertis.ydb.{RetryOptions, Ydb}
import vertis.spamalot.SpamalotYdbTest
import vertis.spamalot.model.{ReceiverId, UserId}
import vertis.zio.BaseEnv
import vertis.zio.test.ZioSpecBase
import vertis.core.utils.NoWarnFilters
import zio.{RIO, ZIO}
import zio.duration._
import cats.syntax.option._
import org.scalacheck.magnolia._

import scala.annotation.nowarn

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
@nowarn(NoWarnFilters.Deprecation)
class OperationsQueueStorageIntSpec extends ZioSpecBase with SpamalotYdbTest {
  import OperationsQueueStorage.OperationPayloadCodec

  private lazy val storage = storages.operationStorage

  "OperationsQueueStorage" should {
    "peek operations" in ydbTest {
      val now = Instant.now()
      val receiver = random[ReceiverId]
      val partition = getPartition(receiver)
      val operations = randomOperations(now, 5, receiver)
      val queueElements = operations.map(storage.toQueueElement)
      val opType = queueElements.head.elementType
      for {
        _ <- txAutoCommit(storage.addElements[OperationPayload](partition, queueElements))
        peeked <- txAutoCommit(storage.peekElements[OperationPayload](now, partition, opType))
        _ <- check("Got all operations")(peeked.size should be(operations.size))
        _ <- txAutoCommit(storage.dropElementsInPartition[OperationPayload](partition, queueElements))
        _ <- checkM("Operations dropped")(
          Ydb
            .runTx(storage.peekElements[OperationPayload](now, partition, opType))
            .map(_ shouldBe empty)
        )
      } yield ()
    }

    // todo mv
    "peek insert and drop" in ydbTest {
      val receiverId = random[ReceiverId]
      val partition = getPartition(receiverId)
      val now = Instant.now()
      val operations = randomOperations(now, 5, receiverId)
      val queueElements = operations.map(storage.toQueueElement)
      val opType = queueElements.head.elementType

      val notifications = random[StoredNotification](5).map(_.copy(receiverId = receiverId.proto.some))
      for {
        _ <- txAutoCommit(storage.addElements[OperationPayload](partition, queueElements))
        _ <- tx[Logging.Logging, Throwable, Unit] {
          for {
            toDrop <- storage.peekElements[OperationPayload](now, partition, opType)
            toAdd <- storages.notificationStorage.findMissing(receiverId, notifications)
            _ <- storages.notificationStorage.upsert(notifications.filter(n => toAdd.contains(n.id)))
            _ <- storage.dropElementsInPartition[OperationPayload](partition, toDrop)
          } yield ()
        }
      } yield ()
    }

    "poll operations" in ydbTest {
      val now = Instant.now()
      val receiver = random[ReceiverId]
      val partition = getPartition(receiver)
      val operations = randomOperations(now, 5, receiver)
      val queueElements = operations.map(storage.toQueueElement)
      val opType = queueElements.head.elementType
      for {
        _ <- Ydb.runTx(storage.addElements[OperationPayload](partition, queueElements))
        polled <- Ydb.runTx(storage.pollElements[OperationPayload](now, partition, opType))
        _ <- check("Got all operations")(polled.size should be(operations.size))
        _ <- checkM("Operations dropped")(
          Ydb
            .runTx(storage.peekElements[OperationPayload](now, partition, opType))
            .map(_ shouldBe empty)
        )
      } yield ()
    }

    s"not invalidate inserts by polling" in ydbTest {
      val now = Instant.now()
      insertAndPoll(now, 0, now, 100, receiver = random[ReceiverId])
    }

    Iterator.from(0).take(5).foreach { i =>
      s"not invalidate inserts and polling for different time range $i" in ydbTest {
        val now = Instant.now()
        val later = now.plusSeconds(1)
        insertAndPoll(later, 0, now, 0, receiver = random[ReceiverId])
      }
    }

    "not fail on an empty batch of operations" in ydbTest {
      Ydb.runTx(storage.addReceiverOperations(ReceiverId.User(UserId("me")), Seq.empty))
    }
  }

  private def insertAndPoll(
      insertTs: Instant,
      insertRetries: Int,
      pollTs: Instant,
      pollRetries: Int,
      n: Int = 200,
      receiver: ReceiverId): RIO[Ydb with BaseEnv, Unit] = {
    val operations = randomOperations(insertTs, n, receiver)
    val queueElements = operations.map(storage.toQueueElement)
    val opType = queueElements.head.elementType
    val partition = getPartition(receiver)
    val insertRetry = RetryOptions.fast.copy(maxRetries = insertRetries)
    val pollRetry = RetryOptions.fast.copy(maxRetries = pollRetries)
    val maxTs = if (insertTs.isAfter(pollTs)) insertTs else pollTs
    for {
      inserting <- ZIO
        .foreachParN(10)(queueElements) { op =>
          for {
            rnd <- zio.random.nextIntBounded(100)
            _ <- ZIO.sleep(rnd.millis)
            _ <- Ydb.runTx(TransactionMode.SERIALIZABLE_READ_WRITE, insertRetry)(
              storage.addElement[OperationPayload](op).withAutoCommit
            )
          } yield ()
        }
        .unit
        .fork
      polling <- Ydb
        .runTx(TransactionMode.SERIALIZABLE_READ_WRITE, pollRetry)(
          storage
            .pollElements[OperationPayload](
              pollTs,
              partition = partition,
              elementType = opType,
              limit = n
            )
            .withAutoCommit
        )
        .fork

      polled <- polling.join.absorb.either
      inserted <- inserting.join.absorb.either
      notDropped <- Ydb.runTx(storage.countElements(maxTs, partition, opType, 1000))
      _ <- runTx(storage.dropElementsInPartition[OperationPayload](partition, queueElements))

      _ <- check("insert was not aborted")(inserted.isRight shouldBe true)
      _ <- check("poll was not aborted")(polled.isRight shouldBe true)

      polledOperations = polled.toOption.map(_.size).get
      _ <- logger.info(s"$notDropped operations stayed in q")
      _ <- logger.info(s"$polledOperations operations polled from q")
      _ <- check("Got all operations")(polledOperations + notDropped should be(operations.size))

    } yield ()
  }
}
