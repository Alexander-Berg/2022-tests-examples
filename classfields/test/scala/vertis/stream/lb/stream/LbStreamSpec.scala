package vertis.stream.lb.stream

import common.zio.logging.Logging
import ru.yandex.vertis.util.collection._
import vertis.core.model.{DataCenter, DataCenters}
import vertis.logbroker.client.LbTask
import vertis.logbroker.client.consumer.config.LbConsumerSessionConfig
import vertis.logbroker.client.consumer.model.in.ReadResult
import vertis.logbroker.client.consumer.model.{Cookie, Offset}
import vertis.logbroker.client.consumer.session.lb_native.LbNativeConsumerSession.ConsumerCb
import vertis.logbroker.client.model.LogbrokerError
import vertis.logbroker.client.test.unit.ConsumerUnitTest.{TestConsumerControl, TestSourceConf}
import vertis.logbroker.client.test.unit.TestLogbrokerErrors._
import vertis.logbroker.client.test.unit.{ConsumerUnitTest, UnitTestingLogbrokerNativeFacade}
import vertis.stream.OffsetCommit.CommitsCallback
import vertis.stream.lb.model.LbStreamSourceConfig
import vertis.stream.lb.sink.LbTopicConverters
import vertis.stream.lb.stream.LbStreamSpec._
import vertis.stream.model.TopicPartition
import vertis.zio.BaseEnv
import vertis.zio.test.ZioSpecBase
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.zio.util.ZioFiberEnrichers._
import zio._
import zio.duration._
import zio.stream.{Sink, ZStream}

import java.util.concurrent.ThreadLocalRandom

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class LbStreamSpec extends ZioSpecBase {

  val repetitions = 10

  "LbStream" should {
    "stream from all dcs" in streamTest { test =>
      import test._
      for {
        q <- Queue.unbounded[ReadResult]
        n <- randomNatural()
        results <- lbStream
          .streamInto(q)(defaultConf, None)
          .use { _ =>
            ZStream.fromQueueWithShutdown(q).take(n.toLong).runCollect
          }
          .fork
        _ <- sendN(n)
        _ <- checkM(s"got $n results")(results.join.map(_.size shouldBe n))
        _ <- checkAllClosed(test)
      } yield ()
    }

    "stream from all dcs with retries" in streamTest { test =>
      import test._
      for {
        q <- Queue.unbounded[ReadResult]
        n <- randomNatural()
        results <- lbStream
          .streamInto(q)(defaultConf, None)
          .use { _ =>
            ZStream.fromQueueWithShutdown(q).take(n.toLong).runCollect
          }
          .fork
        _ <- sendFail(retryable, n * 2)
        // test limitation, some results are going to be thrown away, being from the old consumers
        _ <- sendInf.fork
        _ <- checkM(s"got $n results")(results.join.map(_.size shouldBe n))
        _ <- checkAllClosed(test)
      } yield ()
    }

    Iterator.from(0).take(repetitions).foreach { i =>
      s"close all sessions on non-retryable error in one, take $i" in
        streamTest { test =>
          import test._
          for {
            _ <- sendInf.fork
            q <- Queue.unbounded[ReadResult]
            results <- lbStream
              .streamInto(q)(defaultConf, None)
              .use { _ =>
                ZStream.fromQueueWithShutdown(q).runCollect
              }
              .fork
            _ <- sendFail(nonRetryable)
            _ <- results.join
            _ <- checkAllClosed(test)
          } yield ()
        }
    }

    s"close all sessions on non-retryable error on init" in
      customStreamTest(onInit = ZIO.fail(nonRetryable)) { test =>
        import test._
        for {
          q <- Queue.unbounded[ReadResult]
          _ <- lbStream
            .streamInto(q)(defaultConf.copy(maxInFlight = 16), None)
            .use { _ =>
              ZStream.fromQueueWithShutdown(q).runCollect
            }
          _ <- checkAllClosed(test)
        } yield ()
      }

    Iterator.from(0).take(repetitions).foreach { i =>
      s"close all sessions on external shutdown, take $i" in streamTest { test =>
        import test._
        for {
          _ <- sendInf.fork
          q <- Queue.unbounded[ReadResult]
          res <- lbStream
            .streamInto(q)(defaultConf, None)
            .use { _ =>
              ZStream.fromQueueWithShutdown(q).runCollect
            }
            .fork
          _ <- q.shutdown
          _ <- res.await
          _ <- checkAllClosed(test)
        } yield ()
      }
    }

    Iterator.from(0).take(repetitions).foreach { i =>
      s"commit, take $i" in streamTest { test =>
        import test._
        for {
          n <- randomNatural(20)
          _ <- sendN(n).fork
          q <- Queue.unbounded[ReadResult]
          res <- lbStream
            .streamInto(q)(defaultConf, None)
            .use { commit =>
              ZStream
                .fromQueueWithShutdown(q)
                .take(n.toLong)
                .run(collectAndCommit(commit))
            }
          _ <- checkAllClosed(test)
          _ <- check(s"got $n results")(res.size shouldBe n)
          _ <- checkM(s"committed $n times")(committed.map(_.values.map(_.size).sum shouldBe n))
        } yield ()
      }
    }

    Iterator.from(0).take(repetitions * 10).foreach { i =>
      s"commit on close, take $i" in streamTest { test =>
        import test._
        for {
          _ <- sendInf.fork
          q <- Queue.unbounded[ReadResult]
          res <- lbStream
            .streamInto(q)(defaultConf, None)
            .use { commit =>
              ZStream
                .fromQueueWithShutdown(q)
                .run(collectAndCommit(commit))
            }
            .forkNamed(s"stream_$i")
          _ <- randomNatural(1000).map(_.millis) >>= (d => ZIO.sleep(d))
          _ <- q.shutdown
          result <- res.join
          _ <- checkAllClosed(test)
          committedCookies <- committed
          receivedCookies = result.map(rr => dc(rr) -> rr.cookie).groupByTuple[DataCenter, Cookie, Seq]
          notCommitted = receivedCookies
            .map { case (dc, cookies) =>
              val dcCommitted = committedCookies.getOrElse(dc, Seq.empty).toSet
              dc -> cookies.filterNot(dcCommitted.contains)
            }
            .filter(_._2.nonEmpty)
          notReceived = committedCookies
            .map { case (dc, cookies) =>
              val dcReceived = receivedCookies.getOrElse(dc, Seq.empty).toSet
              dc -> cookies.filterNot(dcReceived.contains)
            }
            .filter(_._2.nonEmpty)
          _ <- check("committed all received")(notCommitted shouldBe empty)
          _ <- check("received all committed")(notReceived shouldBe empty)
        } yield ()
      }
    }
  }

  private def dc(rr: ReadResult): DataCenter =
    rr.messages.head._1.dc

  private def collectAndCommit(commit: CommitsCallback) =
    Sink
      .foldLeftM[BaseEnv, Throwable, ReadResult, List[ReadResult]](List.empty[ReadResult]) { case (acc, x) =>
        Logging.info(s"Got ${x.cookie} from ${dc(x)}") *>
          commit(offsets(x)).as(x :: acc)
      }
      .map(_.reverse)

  private def offsets(res: ReadResult*): Map[TopicPartition, Offset] =
    res.view
      .flatMap(_.lastOffsets)
      .groupBy(_._1)
      .map { case (lbTp, o) => LbTopicConverters.lbTpConverter.get(lbTp) -> o.view.map(_._2).max }

  private def checkAllClosed(test: Test) =
    checkM("all consumers closed") {
      test.activeConsumers.map { activeMap =>
        val stillActive = activeMap.filter(_._2 > 0).keys
        val doubleClosed = activeMap.filter(_._2 < 0).keys
        withClue(s"Still active")(stillActive shouldBe empty)
        withClue(s"Doulbe closed")(doubleClosed shouldBe empty)
      }
    }

  private def streamTest(body: Test => TestBody): Unit =
    customStreamTest()(body)

  private def customStreamTest(
      onInit: LbTask[Unit] = UIO.unit,
      topicConf: Set[TestSourceConf] = defaultTopics
    )(body: Test => TestBody): Unit =
    ioTest {
      for {
        test <- LbStreamSpec.create(onInit, topicConf)
        _ <- body(test)
      } yield ()
    }
}

object LbStreamSpec {

  private val defaultTopics =
    DataCenters.logbrokerDcs.iterator.map(dc => TestSourceConf.default.copy(dc = dc)).toSet

  private val defaultConf =
    LbStreamSourceConfig(
      TestSourceConf.default.topic,
      "test-consumer",
      defaultTopics.map(_.dc),
      TestSourceConf.default.partitions
    )

  private def create(onInit: LbTask[Unit], topics: Set[TestSourceConf]): RIO[BaseEnv, Test] =
    ZIO
      .foreach(topics) { topic =>
        ConsumerUnitTest.create(topic, _ => onInit).map(topic.dc -> _)
      }
      .map(_.toMap)
      .map { tests =>
        val make = (conf: LbConsumerSessionConfig, cb: ConsumerCb) => tests(conf.dc).make(cb)
        val facade = new UnitTestingLogbrokerNativeFacade(make)
        Test(new LbStream(facade), tests.view.mapValues(_.control).toMap)
      }

  case class Test(lbStream: LbStream, controls: Map[DataCenter, TestConsumerControl]) {
    private val dcSeq = controls.keys.toIndexedSeq

    def rndDc: DataCenters.Value = dcSeq(ThreadLocalRandom.current().nextInt(dcSeq.size))

    def rndControl: UIO[TestConsumerControl] = UIO(controls(rndDc))

    def activeConsumers: URIO[Logging.Logging, Map[DataCenter, Int]] =
      Logging.info("Collecting active consumers") *> ZIO
        .foreach(controls.toSeq) { case (dc, control) => control.activeConsumers.map(dc -> _) }
        .map(_.toMap)

    def sendN(n: Int): ZIO[BaseEnv, Nothing, Unit] =
      sendOnSchedule(Schedule.recurs(n - 1) && Schedule.spaced(20.millis))

    val sendInf: ZIO[BaseEnv, Nothing, Unit] =
      sendOnSchedule(Schedule.spaced(20.millis))

    // todo || send to diff dc?
    def sendOnSchedule[R](schedule: Schedule[R, Any, Any]): ZIO[R with BaseEnv, Nothing, Unit] =
      (rndControl >>= (c => c.lbSuccess >>= c.reply))
        .repeat(schedule)
        .unit

    def sendFail(err: LogbrokerError, n: Int = 1): ZIO[BaseEnv, Nothing, Unit] =
      (rndControl >>= (_.reply(err))).fork
        .repeat(Schedule.recurs(n - 1) && Schedule.spaced(20.millis))
        .unit

    val committed: UIO[Map[DataCenter, Seq[Cookie]]] =
      ZIO.foreach(controls) { case (dc, control) => control.committed.map(dc -> _) }.map(_.toMap)
  }

}
