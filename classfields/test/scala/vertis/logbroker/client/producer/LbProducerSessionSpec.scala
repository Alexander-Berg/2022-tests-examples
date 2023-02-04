package vertis.logbroker.client.producer

import vertis.logbroker.client.model.LbSuccess
import vertis.logbroker.client.model.LogbrokerError.UnexpectedLogbrokerError
import vertis.logbroker.client.producer.LbNativeProducerSession.ProducerCb
import vertis.logbroker.client.producer.config.LbProducerSessionConfig
import vertis.logbroker.client.producer.model.{InitResult, Message, WriteResult}
import vertis.logbroker.client.{LbTask, RLbTask}
import vertis.zio.BaseEnv
import vertis.zio.actor.ActorTest
import vertis.zio.test.{ZioEventually, ZioSpecBase}
import zio._
import zio.duration._

/** @author zvez
  */
class LbProducerSessionSpec extends ZioSpecBase with ZioEventually with ActorTest {

  "LbProducerSession" should {
    "open -> process -> close" in ioTest {
      val seqNums = 1L to 100L
      val messages = seqNums.map(i => Message.raw(i, Array.empty))
      for {
        initedRef <- Ref.make(false)
        closedRef <- Ref.make(false)
        responseCb <- Ref.make[ProducerCb](_ => ZIO.unit)
        q <- Queue.unbounded[Message]
        nativeSession = new LbNativeProducerSession {
          override def init: LbTask[InitResult] = initedRef.set(true) *> UIO(InitResult(0, "234", 0))

          override def send(msg: Message): RLbTask[BaseEnv, Unit] = {
            q.offer(msg) *> responseCb.get.flatMap { cb =>
              val res = LbSuccess(WriteResult(msg.seqNo, 0L, alreadyWritten = false))
              cb(res)
            }
          }

          override def close: UIO[Unit] = closedRef.set(true)
        }
        results <- LbProducerSession
          .makeSession(
            LbProducerSessionConfig("test", "test"),
            cb => responseCb.set(cb).as(nativeSession)
          )
          .use { session =>
            ZIO.foreach(messages)(session.write).flatMap(ZIO.collectAllPar(_))
          }

        wasInited <- initedRef.get
        messagesReceived <- q.takeAll
        wasClosed <- closedRef.get
        _ <- check {
          wasInited shouldBe true
          (messagesReceived should contain).theSameElementsInOrderAs(messages)
          results.collect { case wr: WriteResult => wr.seqNo } should contain theSameElementsAs seqNums
          wasClosed shouldBe true
        }
      } yield ()
    }
  }

  "reconnect on init error" in ioTest {
    val (messages1, messages2) = (1L to 1000L).map(Message.raw(_, Array.empty)).splitAt(500)
    for {
      shouldFail <- Ref.make(true)
      responseCb <- Ref.make[ProducerCb](_ => ZIO.unit)

      responseActor <- createActor[BaseEnv, LbSuccess[WriteResult], Unit](()) { case (_, _, res) =>
        responseCb.get.flatMap(_.apply(res))
      }

      nativeSession = new LbNativeProducerSession {
        override def init: LbTask[InitResult] =
          shouldFail.get.flatMap {
            case false => UIO(InitResult(0, "234", 0))
            case true => ZIO.fail(UnexpectedLogbrokerError(new RuntimeException("fail")))
          }

        override def send(msg: Message): LbTask[Unit] = {
          responseCb.get *> {
            val res = LbSuccess(WriteResult(msg.seqNo, 0L, alreadyWritten = false))
            responseActor.send(res)
          }
        }

        override def close: UIO[Unit] = ZIO.unit
      }

      _ <- LbProducerSession
        .makeSession(
          LbProducerSessionConfig("test", "reconnect-test", retryInitDelay = 100.millis),
          cb => responseCb.set(cb).as(nativeSession)
        )
        .use { session =>
          for {
            results1 <- ZIO
              .foreach(messages1)(msg => session.write(msg).map(_.either))
              .flatMap(xs => ZIO.collectAll(xs))

            _ <- shouldFail.set(false)
            results2 <- ZIO
              .foreach(messages2)(msg => session.write(msg).map(_.either))
              .flatMap(xs => ZIO.collectAll(xs))

            _ <- check {
              results1.forall(_.isLeft) shouldBe true
              results2.forall(_.isRight) shouldBe true
            }
          } yield ()
        }
    } yield ()
  }

  "reconnect on out of order seqNo" in ioTest {
    val messages = (1L to 1000L).map(Message.raw(_, Array.empty))

    for {
      inOrder <- Ref.make(false)
      responder = { msg: Message =>
        inOrder.get.map {
          case false if msg.seqNo == 501 => LbSuccess(WriteResult.simple(999L))
          case _ => LbSuccess(WriteResult.simple(msg.seqNo))
        }
      }
      _ <- LbProducerSession
        .makeSession(
          LbProducerSessionConfig("test", "foo", retryInitDelay = 20.millis),
          cb => UIO(new TestNativeProducerSession(cb, responder))
        )
        .use { session =>
          for {
            promises <- ZIO.foreach(messages) { msg =>
              for {
                p <- Promise.make[Any, Boolean]
                result <- session.write(msg)
                _ <- p.complete(result.fold(_ => false, _ => true)).fork
              } yield p
            }
            _ <- checkEventually {
              ZIO.foreach(promises)(_.isDone).flatMap { completed =>
                check {
                  completed.take(500).forall(_ == true) shouldBe true
                  completed.drop(500).forall(_ == false) shouldBe true
                }
              }
            }
            _ <- inOrder.set(true)
            _ <- checkEventually {
              ZIO.foreach(promises)(_.isDone).flatMap { completed =>
                check {
                  completed.forall(_ == true) shouldBe true
                }
              }
            }
          } yield ()
        }
    } yield ()
  }

}
