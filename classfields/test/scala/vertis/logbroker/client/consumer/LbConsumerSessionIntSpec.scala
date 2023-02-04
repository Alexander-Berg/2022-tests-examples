package vertis.logbroker.client.consumer

import common.zio.logging.Logging
import org.scalatest.Assertion
import ru.yandex.kikimr.persqueue.compression.CompressionCodec.RAW
import ru.yandex.kikimr.persqueue.consumer.transport.ConsumerMessageListener
import ru.yandex.kikimr.persqueue.consumer.transport.message.CommitMessage
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.{
  ConsumerInitResponse,
  ConsumerLockMessage,
  ConsumerReadResponse,
  ConsumerReleaseMessage
}
import vertis.logbroker.client.BLbTask
import vertis.logbroker.client.consumer.model.in.InitResult
import vertis.logbroker.client.consumer.model.offsets.OffsetSource
import vertis.logbroker.client.consumer.model.{Offset, Partition}
import vertis.logbroker.client.consumer.session.lb_native.LbNativeConsumerSession.ConsumerListenerParams
import vertis.logbroker.client.model.LogbrokerError
import vertis.logbroker.client.producer.model.Message
import vertis.logbroker.client.test.{LbRead, LbTest, LbWrite}
import vertis.zio.BTask
import vertis.zio.util.ZioRunUtils
import zio._
import zio.duration.durationInt

import java.util.concurrent.atomic.AtomicLong
import scala.util.{Failure, Success}

/** @author kusaeva
  */
class LbConsumerSessionIntSpec extends LbWrite with LbRead with LbTest {
  private val topic = topicNameFromClassName
  private val n = 3
  private val readOffset = new AtomicLong(-1L)
  private val awaitFor = 5.seconds

  "LbConsumerSession" should {

    /** if 0,1,2 written and read (so in yt we have last written offset = 2),
      * then after initialization read offset should be 3
      */
    "commit offsets on init" in ioTest {
      val group = 1
      write(topic, Map(group -> groupMessages(group, n))) *>
        checkOffset(group, 0L, "initial offset") *>
        commitOffset(group, n - 1L) *>
        checkOffset(group, n.toLong, "committed offset").eventually
    }

    /** if 0,1,2 written, and 0,1 read (so in yt we have last written offset = 1),
      * then after initialization we will read only one last message - 2
      */
    "read after commit" in ioTest {
      val group = 2
      val offset = 1L
      write(topic, Map(group -> groupMessages(group, n))) *>
        read(topic, Seq(group), awaitFor, None, 1, 1, Some(getOffsetSource(group, offset)), None).flatMap { c =>
          check(s"read:")(c shouldBe (n - offset - 1))
        }.eventually
    }
    "not fail if offset in yt is higher than endOffset" in ioTest {
      val group = 3
      write(topic, Map(group -> groupMessages(group, n))) *>
        read(topic, Seq(group), awaitFor, None, 1, 1, Some(getOffsetSource(group, n.toLong)), None).flatMap { c =>
          check(s"read:")(c shouldBe 0L)
        }.eventually
    }

    "not commit on init if offset in yt is lower than an offset to read" in ioTest {
      val group = 4
      for {
        _ <- write(topic, Map(group -> groupMessages(group, 3)))
        _ <- read(topic, Seq(group), awaitFor, None, 1, 1, Some(getOffsetSource(group, -1L, 2L)), None)
          .flatMap(c => check(s"read:")(c shouldBe 1L))
        _ <- write(topic, Map(group -> groupMessages(group, 4, 10)))
        _ <- read(topic, Seq(group), awaitFor, None, 1, 1, Some(getOffsetSource(group, -1L)), None)
          .flatMap(c => check(s"read:")(c shouldBe 13L))
          .eventually
      } yield ()
    }
  }

  private def getOffsetSource(group: Int, lastOffset: Long): OffsetSource =
    new OffsetSource {
      val partitions: Set[Partition] = Set(group - 1)

      override def lastWrittenOffsets: BLbTask[Map[Partition, Offset]] =
        UIO(partitions.map(_ -> lastOffset).toMap)
    }

  private def getOffsetSource(group: Int, lastWrittenOffset: Long, offsetToRead: Long): OffsetSource =
    new OffsetSource {
      val partitions: Set[Partition] = Set(group - 1)

      override def offsetsToRead(lastRead: Map[Partition, Offset]): BLbTask[Map[Partition, Offset]] =
        UIO(partitions.map(_ -> offsetToRead).toMap)

      override def lastWrittenOffsets: BLbTask[Map[Partition, Offset]] =
        UIO(partitions.map(_ -> lastWrittenOffset).toMap)
    }

  private def checkOffset(group: Int, expected: Long, clue: String): BTask[Assertion] =
    read(topic, Seq(group), awaitFor, None, 1, 1, None, Some(createLockListener)) *>
      check(s"$clue:")(readOffset.get() shouldBe expected)

  private def commitOffset(group: Int, lastOffset: Long): BTask[Unit] = {
    val offsetSource = getOffsetSource(group, lastOffset)
    read(topic, Seq(group), awaitFor, None, 1, 1, Some(offsetSource), None).unit
  }

  private def groupMessages(group: Int, n: Int): Seq[Message] =
    groupMessages(group, 1, n)

  private def groupMessages(group: Int, from: Int, n: Int): Seq[Message] =
    Iterator
      .from(from)
      .map(i => Message.raw(i.toLong, s"some data $i for group $group".getBytes))
      .take(n)
      .toSeq

  /** Dummy listener saves `readOffset` received on lock to AtomicLong
    */
  private class LockListener[R <: Logging.Logging](
      rt: Runtime[R],
      initedPromise: Promise[LogbrokerError, InitResult]
    )(offset: AtomicLong)
    extends ConsumerMessageListener {

    private def tryRun[T](task: RIO[R, T]): Unit = {
      ZioRunUtils.runSync(rt)(task) match {
        case Success(_) =>
        case Failure(e) =>
          ZioRunUtils.runSync(rt)(Logging.error(s"Error in listener: $e")): Unit
      }
    }

    override def onInit(init: ConsumerInitResponse): Unit =
      tryRun {
        initedPromise.succeed(InitResult.fromLb(init))
      }

    override def onLock(lock: ConsumerLockMessage): Unit =
      tryRun {
        Logging
          .info(s"Lock for partition ${lock.getPartition} acknowledged, offset: ${lock.getReadOffset}")
          .as(offset.set(lock.getReadOffset))
      }

    override def onData(read: ConsumerReadResponse): Unit = ???

    override def onCommit(commit: CommitMessage): Unit = ???

    override def onRelease(release: ConsumerReleaseMessage): Unit = ()

    override def onError(t: Throwable): Unit = ()

    override def onCompleted(): Unit = ()
  }

  private def createLockListener(params: ConsumerListenerParams) = {
    import params._
    new LockListener(rt, initedPromise)(readOffset)
  }
}
