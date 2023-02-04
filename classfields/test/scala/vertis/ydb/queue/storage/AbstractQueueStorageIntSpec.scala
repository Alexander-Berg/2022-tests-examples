package vertis.ydb.queue.storage

import com.yandex.ydb.table.transaction.TransactionMode
import ru.yandex.vertis.ydb.RetryOptions
import ru.yandex.vertis.ydb.Ydb
import vertis.ydb.partitioning.manual.ManualPartition
import vertis.ydb.queue.QueueStorageTest
import vertis.zio.BaseEnv
import vertis.zio.test.ZioSpecBase
import zio.{RIO, ZIO}
import zio.duration.durationInt

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class AbstractQueueStorageIntSpec extends ZioSpecBase with QueueStorageTest {

  def makeElement(partition: ManualPartition, ts: Instant, eType: String): QueueElement[Unit] = {
    QueueElement[Unit](partition.id, UUID.randomUUID().toString, ts.truncatedTo(ChronoUnit.MICROS), None, eType, ())
  }

  def makeElements(partition: ManualPartition, ts: Instant, eType: String)(n: Int): List[QueueElement[Unit]] = {
    (1 to n).map { _ =>
      makeElement(partition, ts, eType)
    }.toList
  }

  "AbstractQueueStorage" should {
    "add element" in ydbTest {
      for {
        now <- zio.clock.instant
        partition = partitioning.getByIndex(0)
        eType = "add_element"
        element = makeElement(partition, now, eType)
        _ <- runTx(storage.addElement[Unit](element))
        polled <- runTx(storage.pollElements[Unit](now, partition, eType))
        _ <- check(polled should contain theSameElementsAs Seq(element))
      } yield ()
    }

    "add elements" in ydbTest {
      for {
        now <- zio.clock.instant
        partition = partitioning.getByIndex(0)
        eType = "add_elements"
        element = makeElement(partition, now, eType)
        _ <- runTx(storage.addElement[Unit](element))
        elements = makeElements(partition, now, eType)(2)
        _ <- runTx(storage.addElements[Unit](partition, elements))
        polled <- runTx(storage.pollElements[Unit](now, partition, eType))
        _ <- check(polled should contain theSameElementsAs (element :: elements))
      } yield ()
    }

    "count by partition and type" in ydbTest {
      for {
        now <- zio.clock.instant
        partition0 = partitioning.getByIndex(0)
        partition1 = partitioning.getByIndex(1)
        type0 = "count_0"
        type1 = "count_1"
        part0Elements = makeElement(partition0, now, type1) :: makeElements(partition0, now, type0)(3)
        part1Elements = makeElements(partition1, now, type0)(2) ::: makeElements(partition1, now, type1)(4)
        _ <- runTx(storage.addElements[Unit](partition0, part0Elements))
        _ <- runTx(storage.addElements[Unit](partition1, part1Elements))

        p0t0Count <- runTx(storage.countElements(now, partition0, type0, 1000))
        p1t0Count <- runTx(storage.countElements(now, partition1, type0, 1000))
        p0t1Count <- runTx(storage.countElements(now, partition0, type1, 1000))
        p1t1Count <- runTx(storage.countElements(now, partition1, type1, 1000))

        _ <- check("partition 0 type 0")(p0t0Count shouldBe 3)
        _ <- check("partition 0 type 1")(p0t1Count shouldBe 1)
        _ <- check("partition 1 type 0")(p1t0Count shouldBe 2)
        _ <- check("partition 1 type 1")(p1t1Count shouldBe 4)
      } yield ()
    }

    "drop elements" in ydbTest {
      for {
        now <- zio.clock.instant
        partition = partitioning.getByIndex(0)
        type1 = "drop_1"
        type2 = "drop_2"
        elements = makeElement(partition, now, type2) :: makeElements(partition, now, type1)(3)
        _ <- runTx(storage.addElements[Unit](partition, elements))

        beforeDropCount <- runTx(storage.countElements(now, partition, type1, 1000))
        _ <- check("before drop")(beforeDropCount shouldBe 3)

        toDrop = elements.filter(_.elementType == type1).take(2)
        _ <- runTx(storage.dropElementsInPartition[Unit](partition, toDrop))

        afterDropCount <- runTx(storage.countElements(now, partition, type2, 1000))
        _ <- check("after drop")(afterDropCount shouldBe 1)
      } yield ()
    }

    "fail to drop elements with different types" in ydbTest {
      for {
        now <- zio.clock.instant
        partition = partitioning.getByIndex(0)
        type1 = "drop_failed_1"
        type2 = "drop_failed_2"
        t1Elements = makeElements(partition, now, type1)(3)
        t2Element = makeElement(partition, now, type2)
        elements = t2Element :: t1Elements
        _ <- runTx(storage.addElements[Unit](partition, elements))

        toDrop = Seq(t1Elements.head, t2Element)
        _ <- runTx(storage.dropElementsInPartition[Unit](partition, toDrop))
          .foldM(
            {
              case _: IllegalArgumentException => ZIO.unit
              case ex => ZIO.die(ex)
            },
            _ => ZIO.dieMessage("expect IllegalArgumentException")
          )
      } yield ()
    }

    "peek elements" in ydbTest {
      for {
        now <- zio.clock.instant
        partition0 = partitioning.getByIndex(0)
        partition1 = partitioning.getByIndex(1)
        type0 = "peek_0"
        type1 = "peek_1"

        part0Elements = makeElement(partition0, now, type1) :: makeElements(partition0, now, type0)(3)
        part1Element = makeElement(partition1, now, type0)
        elements = part1Element :: part0Elements
        _ <- runTx(storage.addElements[Unit](partition0, part0Elements))
        _ <- runTx(storage.addElement[Unit](part1Element))

        expected = elements.filter(p => p.partitionId == partition0.id && p.elementType == type0)
        peeked1 <- runTx(storage.peekElements[Unit](now, partition0, type0))
        peeked2 <- runTx(storage.peekElements[Unit](now, partition0, type0))
        _ <- check("first peek")(peeked1 should contain theSameElementsAs expected)
        _ <- check("second peek")(peeked2 should contain theSameElementsAs expected)
      } yield ()
    }

    "poll elements" in ydbTest {
      for {
        now <- zio.clock.instant
        partition0 = partitioning.getByIndex(0)
        partition1 = partitioning.getByIndex(1)
        type0 = "poll_0"
        type1 = "poll_1"

        part0Elements = makeElement(partition0, now, type1) :: makeElements(partition0, now, type0)(3)
        part1Element = makeElement(partition1, now, type0)
        elements = part1Element :: part0Elements
        _ <- runTx(storage.addElements[Unit](partition0, part0Elements))
        _ <- runTx(storage.addElement[Unit](part1Element))

        polled1 <- runTx(storage.pollElements[Unit](now, partition0, type0))
        expected = elements.filter(p => p.partitionId == partition0.id && p.elementType == type0)
        _ <- check("poll all in partition")(polled1 should contain theSameElementsAs expected)

        polled2 <- runTx(storage.pollElements[Unit](now, partition0, type0))
        _ <- check("got empty")(polled2 shouldBe empty)
      } yield ()
    }

    "not invalidate inserts by polling" in ydbTest {
      val now = Instant.now()
      val partition = partitioning.getByIndex(0)
      insertAndPoll(partition, now, 0, now, 100)
    }

    Iterator.from(0).take(5).foreach { i =>
      s"not invalidate inserts and polling for different time range $i" in ydbTest {
        val now = Instant.now()
        val later = now.plusSeconds(1)
        val partition = partitioning.getByIndex(0)
        insertAndPoll(partition, later, 0, now, 0)
      }
    }

  }

  // scalastyle:off method.length
  private def insertAndPoll(
      partition: ManualPartition,
      insertTs: Instant,
      insertRetries: Int,
      pollTs: Instant,
      pollRetries: Int,
      n: Int = 200): RIO[Ydb with BaseEnv, Unit] = {
    import ydbWrapper.ops._

    val elementType = "q_type"
    val queueElements = makeElements(partition, insertTs, elementType)(n)
    val insertRetry = RetryOptions.fast.copy(maxRetries = insertRetries)
    val pollRetry = RetryOptions.fast.copy(maxRetries = pollRetries)
    val maxTs = if (insertTs.isAfter(pollTs)) insertTs else pollTs
    for {
      inserting <- ZIO
        .foreachParN(10)(queueElements) { element =>
          for {
            rnd <- zio.random.nextIntBounded(100)
            _ <- ZIO.sleep(rnd.millis)
            _ <- Ydb.runTx(TransactionMode.SERIALIZABLE_READ_WRITE, insertRetry)(
              storage.addElement[Unit](element).withAutoCommit
            )
          } yield ()
        }
        .unit
        .fork
      polling <- Ydb
        .runTx(TransactionMode.SERIALIZABLE_READ_WRITE, pollRetry)(
          storage
            .pollElements[Unit](
              pollTs,
              partition = partition,
              elementType = elementType,
              limit = n
            )
            .withAutoCommit
        )
        .fork

      polled <- polling.join.absorb.either
      inserted <- inserting.join.absorb.either
      notDropped <- Ydb.runTx(storage.countElements(maxTs, partition, elementType, 1000))
      _ <- runTx(storage.dropElementsInPartition[Unit](partition, queueElements))

      _ <- check("insert was not aborted")(inserted.isRight shouldBe true)
      _ <- check("poll was not aborted")(polled.isRight shouldBe true)

      polledElements = polled.toOption.map(_.size).get
      _ <- logger.info(s"$notDropped elements stayed in q")
      _ <- logger.info(s"$polledElements elements polled from q")
      _ <- check("Got all elements")(polledElements + notDropped should be(queueElements.size))

    } yield ()
  }
  // scalastyle:on

}
