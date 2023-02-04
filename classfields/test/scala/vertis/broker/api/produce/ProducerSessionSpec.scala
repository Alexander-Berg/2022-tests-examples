package vertis.broker.api.produce

import broker.core.inner_api.ApiStreamConfig
import com.google.protobuf.ByteString
import com.google.protobuf.util.Timestamps
import common.zio.logging.Logging
import ru.yandex.vertis.broker.model.convert.BrokerModelConverters._
import ru.yandex.vertis.broker.requests.WriteRequest.WriteData
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.proto.util.RandomProtobufGenerator
import vertis.broker.api.validate.SimpleMessage
import vertis.broker.api.model.ProduceError
import vertis.broker.api.parse.Parser
import vertis.broker.api.produce.storage.DummyStorageProducer
import vertis.broker.api.validate.Validator.{MessageTooLargeError, MessageTooOldError}
import vertis.broker.tests.ProducerSessionSupport._
import vertis.zio.test.ZioSpecBase
import zio.ZIO

import java.time.{Instant, Period}

/** @author kusaeva
 */
class ProducerSessionSpec extends ZioSpecBase {

  private val testSessionConfig = ProducerSessionConfig(
    messageType = "test",
    sourceId = "test",
    schemaVersion = "0",
    clientInfo = ProducerInfo("test")
  )

  private val streamConfig = ApiStreamConfig("test", "test", "test", 1, Period.ofDays(7).fromJava)

  "ProducerSession" should {
    "fail with parse error when parsing fails" in ioTest {
      for {
        logger <- ZIO.service[Logging.Service]
        producer <- DummyStorageProducer.make
        session = new ProducerSessionImpl(
          testSessionConfig,
          streamConfig,
          producer,
          FailingParser,
          logger
        )
        r <- session.write(WriteData.defaultInstance).either
        _ <- check {
          r should matchPattern { case Left(ProduceError.Parse(_)) =>
          }
        }
      } yield ()
    }

    "fail when message is too old" in ioTest {
      for {
        logger <- ZIO.service[Logging.Service]
        producer <- DummyStorageProducer.make
        parser <- Parser.make(SimpleMessage.getDescriptor)
        session = new ProducerSessionImpl(
          testSessionConfig,
          streamConfig,
          producer,
          parser,
          logger
        )
        oldMessage = makeMessage(Instant.now().minus(Period.ofDays(90)))
        r <- session.write(oldMessage).either
        _ <- check {
          r should matchPattern { case Left(ProduceError.Validation(_: MessageTooOldError)) =>
          }
        }
        _ <- producer.q.size.flatMap { count =>
          check("should be zero valid messages") {
            count shouldBe 0
          }
        }

        newMessage = makeMessage(Instant.now().minus(Period.ofDays(3)))
        _ <- session.write(newMessage).either.fork // DummyStorageProducer requires explicit ack
        _ <- producer.q.take
      } yield ()
    }

    "fail when message is too large" in ioTest {
      for {
        logger <- ZIO.service[Logging.Service]
        producer <- DummyStorageProducer.make
        parser <- Parser.make(SimpleMessage.getDescriptor)
        session = new ProducerSessionImpl(
          testSessionConfig.copy(maxMessageSize = 10),
          streamConfig,
          producer,
          parser,
          logger
        )
        oldMessage = makeMessage(Instant.now())
        r <- session.write(oldMessage).either
        _ <- check {
          r should matchPattern { case Left(ProduceError.Validation(_: MessageTooLargeError)) =>
          }
        }
      } yield ()
    }

    // todo enable after validator get enabled
    "fail with validation error when validation fails" ignore ioTest {
      for {
        logger <- ZIO.service[Logging.Service]
        producer <- DummyStorageProducer.make
        session = new ProducerSessionImpl(
          testSessionConfig,
          streamConfig,
          producer,
          DummyParser,
          logger
        )
        r <- session.write(WriteData.defaultInstance)
      } yield {
        r should matchPattern { case Left(ProduceError.Validation(_)) =>
        }
      }
    }
  }

  def makeMessage(ts: Instant): WriteData = {
    val payload = RandomProtobufGenerator
      .genForAuto[SimpleMessage]
      .next
      .toBuilder
      .setTimestamp(Timestamps.fromMillis(ts.toEpochMilli))
      .setData("some data for size test")
      .build()
    WriteData(
      seqNo = 1,
      data = ByteString.copyFrom(payload.toByteArray),
      createTimeMs = System.currentTimeMillis()
    )
  }
}
