package vertis.broker.api.produce.shared

import broker.core.inner_api.ApiStreamConfig
import com.google.protobuf.ByteString
import ru.yandex.vertis.broker.api.requests.WriteEventRequest
import ru.yandex.vertis.broker.api.requests.WriteEventRequest.{Header, WriteData}
import ru.yandex.vertis.broker.model.convert.BrokerModelConverters._
import vertis.broker.api.produce.ProducerSessionManager.SessionError
import vertis.broker.api.produce.ProducerSessionManager.SessionError.{SchemaGetError, UnknownMessageType}
import vertis.broker.api.produce._
import vertis.broker.api.produce.shared.SharedProducersManager.Config
import vertis.zio.BaseEnv
import vertis.zio.test.ZioSpecBase
import zio.duration._
import zio.{IO, UIO, ZIO, ZManaged}

import java.time.Period
import java.util.concurrent.atomic.AtomicInteger
import scala.util.Random

/** @author zvez
  */
class SharedProducersManagerImplSpec extends ZioSpecBase {

  private val someHeader = Header(
    messageType = "test",
    schemaVersion = "1234"
  )

  private def someEvent = WriteEventRequest(
    header = someHeader,
    data = WriteData(
      id = Some(Random.nextString(5)),
      data = ByteString.copyFromUtf8(Random.nextString(20))
    )
  )

  private val configSource = new StreamConfigSource {

    override def configForType(messageType: String): IO[UnknownMessageType, ApiStreamConfig] =
      UIO(ApiStreamConfig("test", messageType, "test", partitionCount = 2, Period.ofDays(7).fromJava))
  }

  "SharedProducersManager" should {
    "cache producer session" in ioTest {
      val sessionManager = sessionManagerFor(TestProducerSession.make)
      SharedProducersManagerImpl.make(sessionManager, configSource).use { manager =>
        for {
          key <- manager.makeKey(someEvent)
          session1 <- manager.getSessionFor(key)
          session2 <- manager.getSessionFor(key)
          _ <- check {
            session1 eq session2 shouldBe true
          }
        } yield ()
      }
    }

    "create new session if StreamConfig has changed" in ioTest {
      val sessionManager = sessionManagerFor(TestProducerSession.make)

      val configSource: StreamConfigSource = new StreamConfigSource {

        private val version = new AtomicInteger(0)

        override def configForType(messageType: String): IO[UnknownMessageType, ApiStreamConfig] = {
          val base = ApiStreamConfig("test", messageType, "test", partitionCount = 2, Period.ofDays(7).fromJava)
          UIO {
            if (version.getAndIncrement() == 0) {
              base
            } else {
              base.copy(maxLag = Period.ofDays(42).fromJava)
            }
          }
        }
      }
      val event = someEvent
      SharedProducersManagerImpl.make(sessionManager, configSource).use { manager =>
        for {
          key1 <- manager.makeKey(event)
          session1 <- manager.getSessionFor(key1)
          key2 <- manager.makeKey(event)
          session2 <- manager.getSessionFor(key2)
          key3 <- manager.makeKey(event)
          session3 <- manager.getSessionFor(key3)
          _ <- check {
            session1 eq session2 shouldBe false
            session2 eq session3 shouldBe true
          }
        } yield ()
      }

    }

    "work" in ioTest {
      val sessionManager = sessionManagerFor(TestProducerSession.make)
      SharedProducersManagerImpl.make(sessionManager, configSource).use { manager =>
        val events = (1 to 10).map(_ => someEvent)
        ZIO.foreach(events)(manager.writeEvent)
      }
    }

    "open session for each partition" in ioTest {
      val sessionManager = sessionManagerFor(TestProducerSession.make)
      SharedProducersManagerImpl.make(sessionManager, configSource).use { manager =>
        val events = (1 to 100).map(_ => someEvent)
        for {
          _ <- ZIO.foreach(events)(manager.writeEvent)
          _ <- check(manager.sessions.size shouldBe 2)
        } yield ()

      }
    }

    "not cache failed session" in ioTest {
      val counter = new AtomicInteger(0)
      val sessionManager = sessionManagerFor {
        UIO(
          if (counter.incrementAndGet() == 1) {
            IO.fail(SchemaGetError(new RuntimeException))
          } else {
            TestProducerSession.make
          }
        ).flatten
      }
      SharedProducersManagerImpl.make(sessionManager, configSource).use { manager =>
        for {
          w1 <- manager.writeEvent(someEvent).either
          w2 <- manager.writeEvent(someEvent).either
          _ <- check {
            w1.isLeft shouldBe true
            w2.isRight shouldBe true
          }
        } yield ()
      }
    }

    "drop old sessions" in ioTest {
      val sessionManager = sessionManagerFor(TestProducerSession.make)
      SharedProducersManagerImpl.make(sessionManager, configSource, Config(200.millis, 50.millis)).use { manager =>
        for {
          _ <- manager.writeEvent(someEvent)
          _ <- check(manager.sessions.size shouldBe 1)
          _ <- ZIO.sleep(1.second)
          _ <- check(manager.sessions.size shouldBe 0)
        } yield ()
      }
    }
  }

  private def sessionManagerFor(maker: IO[SessionError, ProducerSession]) =
    new ProducerSessionManager {

      override def openSession(config: ProducerSessionConfig): ZManaged[BaseEnv, SessionError, ProducerSession] =
        maker.toManaged_

      override def close: UIO[Unit] = UIO.unit
    }

}
