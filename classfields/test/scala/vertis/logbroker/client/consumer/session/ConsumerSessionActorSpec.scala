package vertis.logbroker.client.consumer.session

import vertis.logbroker.client.consumer.config.LbConsumerSessionConfig
import vertis.logbroker.client.consumer.model.Offset
import vertis.logbroker.client.consumer.model.offsets.OffsetSource
import vertis.logbroker.client.consumer.model.out.BatchRequestMessage
import vertis.logbroker.client.consumer.session.ConsumerSessionActorSpec.{create, Test, _}
import vertis.logbroker.client.consumer.session.SessionActor.{Event, Init, Request}
import vertis.logbroker.client.test.unit.ConsumerUnitTest.TestConsumer
import vertis.logbroker.client.test.unit.TestLogbrokerErrors._
import vertis.logbroker.client.{BLbTask, LbTask}
import vertis.zio.BaseEnv
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.zio.actor.Actor
import vertis.logbroker.client.consumer.model.in.ReadResult
import vertis.logbroker.client.model.LogbrokerError
import vertis.logbroker.client.model.LogbrokerError.ClientIsClosedError
import vertis.logbroker.client.test.unit.ConsumerUnitTest
import vertis.logbroker.client.test.unit.ConsumerUnitTest.{TestConsumerControl, TestSourceConf}
import vertis.zio.test.ZioSpecBase
import zio._
import zio.duration._

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class ConsumerSessionActorSpec extends ZioSpecBase {

  "Consumer SessionActor" should {
    "init" in actorTest { test =>
      import test._
      for {
        _ <- checkGeneration(control, "no consumer yet", 0)
        _ <- actor.send(Init)
        _ <- checkGeneration(control, "consumer initialized", 1)
      } yield ()
    }

    "fulfill requests once initialized" in actorTest { test =>
      import test._
      import control._
      for {
        p <- Promise.make[LogbrokerError, ReadResult]
        _ <- actor.send(Request(requestMsg, p))
        _ <- lbSuccess >>= reply
        _ <- actor.send(Init)
        _ <- checkSuccess(p)
      } yield ()
    }

    "create new consumer on retryable error" in actorTest { test =>
      import test._
      import control._
      for {
        _ <- actor.send(Init)
        _ <- checkGeneration(control, "consumer initialized", 1)
        p <- request(actor)
        _ <- reply(retryable)
        _ <- checkGeneration(control, "consumer re-initialized", 2)
        _ <- lbSuccess >>= reply
        _ <- checkSuccess(p)
      } yield ()
    }

    "propagate non-retryable error and close" in actorTest { test =>
      import test._
      import control._
      for {
        _ <- actor.send(Init)
        _ <- checkGeneration(control, "consumer initialized", 1)
        p <- request(actor)
        _ <- reply(nonRetryable)
        _ <- lbSuccess >>= reply
        _ <- checkFailure(p)
        _ <- actor.awaitClosed
      } yield ()
    }

    "close consumers" in actorTest { test =>
      import test._
      import control._
      for {
        _ <- actor.send(Init)
        _ <- checkGeneration(control, "consumer initialized", 1)
        _ <- request(actor)
        _ <- reply(retryable).repeat(Schedule.recurs(3))
        _ <- checkGeneration(control, "recreated consumer 4 times", 5)
        _ <- checkActiveConsumers(control)
      } yield ()
    }

    "close consumers if init fails" in customActorTest(onInit = failNTimes(2).map(io => _ => io)) { test =>
      import test._
      import control._
      for {
        _ <- actor.send(Init)
        p <- request(actor)
        _ <- lbSuccess >>= reply
        _ <- checkGeneration(control, "recreated consumer 3 times", 3)
        _ <- checkSuccess(p)
        _ <- checkActiveConsumers(control)
      } yield ()
    }

    "init from offset source" in {
      val topicConf = TestSourceConf.default
      customActorTest(
        topicConf = topicConf,
        offsetSource = Some(nothingWrittenOffsetSource(topicConf)),
        onInit = UIO(initialOffsets =>
          ZIO.when(!initialOffsets.contains(topicConf.partitions.map(p => p -> 0).toMap)) {
            IO.fail(
              LogbrokerError
                .translateLbException(new IllegalStateException(s"Expected 0 init offsets, but got $initialOffsets"))
            )
          }
        )
      ) { test =>
        import test._
        import control._
        for {
          _ <- actor.send(Init)
          _ <- request(actor)
          _ <- checkGeneration(control, "consumer initialized", 1)
          _ <- reply(retryable)
          _ <- checkGeneration(control, "consumer re-initialized", 2)
          _ <- checkActiveConsumers(control)
          _ <- checkM("actor is not closed")(actor.isClosed.map(_ shouldBe false))
        } yield ()
      }
    }
  }

  private def actorTest(body: Test => TestBody): Unit =
    customActorTest()(body)

  private def customActorTest(
      topicConf: TestSourceConf = TestSourceConf.default,
      onInit: UIO[Option[Map[Int, Long]] => LbTask[Unit]] = UIO(_ => UIO.unit),
      onClose: UIO[UIO[Unit]] = UIO(UIO.unit),
      offsetSource: Option[OffsetSource] = None
    )(body: Test => TestBody): Unit =
    ioTest {
      for {
        init <- onInit
        close <- onClose
        test <- create(topicConf, init, close, offsetSource)
        _ <- body(test)
      } yield ()
    }

  private def checkGeneration(control: TestConsumerControl, clue: String, expectedGen: Int) =
    checkEventually(clue)(control.generation >>= (g => Task(g shouldBe expectedGen)))(patience)

  private def checkActiveConsumers(
      control: TestConsumerControl,
      clue: String = "closed old consumers",
      expectActive: Int = 1) =
    checkM(clue)(control.activeConsumers.map(_ shouldBe expectActive))

  private def checkSuccess(p: Promise[LogbrokerError, ReadResult]) =
    checkM("request succeeded")(p.await.either.map(res => res.isRight shouldBe true))

  private def checkFailure(p: Promise[LogbrokerError, ReadResult]) =
    checkM(s"request failed with closed") {
      p.await.either.map(res => res.left.exists(_ == ClientIsClosedError) shouldBe true)
    }

  private def request(actor: Actor[Event]): UIO[Promise[LogbrokerError, ReadResult]] =
    Promise.make[LogbrokerError, ReadResult] >>= { p =>
      actor.send(Request(requestMsg, p)).as(p)
    }
}

object ConsumerSessionActorSpec {

  private def patience = Schedule.spaced(100.millis)

  private val requestMsg = BatchRequestMessage(16, Int.MaxValue)

  private def nothingWrittenOffsetSource(topicConf: TestSourceConf): OffsetSource =
    new OffsetSource {

      override def lastWrittenOffsets: BLbTask[Map[Int, Offset]] =
        UIO(topicConf.partitions.map(p => p -> -1L).toMap)
    }

  private def create(
      topicConf: TestSourceConf,
      consumerOnInit: Option[Map[Int, Long]] => LbTask[Unit],
      consumerOnClose: UIO[Unit],
      offsetSource: Option[OffsetSource]): RIO[BaseEnv, Test] = {
    ConsumerUnitTest.create(topicConf, consumerOnInit, consumerOnClose) >>= { case TestConsumer(make, control) =>
      val config = LbConsumerSessionConfig(
        "test-consumer",
        topicConf.dc,
        topicConf.topic,
        topicConf.partitions.map(_ + 1),
        100500,
        true
      )
      SessionActor
        .create(
          config,
          make,
          offsetSource
        )
        .map(actor => Test(actor, control))
    }
  }

  case class Test(actor: Actor[Event], control: TestConsumerControl)
}
