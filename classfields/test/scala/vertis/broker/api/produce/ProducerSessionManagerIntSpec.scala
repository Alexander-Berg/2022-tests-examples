package vertis.broker.api.produce

import broker.core.inner_api.ApiStreamConfig
import common.sraas.TestSraas.DescriptorKey
import common.sraas.{Sraas, TestSraas}
import common.sraas.DescriptorNotFound
import common.sraas.Sraas.SraasDescriptor
import ru.yandex.vertis.broker.model.convert.BrokerModelConverters._
import vertis.broker.api.produce.storage.StorageProducerBuffer.Mb
import vertis.broker.api.validate.{RecursiveMessage, SimpleMessage}
import vertis.broker.api.app
import vertis.broker.api.model.BrokerInstanceInfo
import vertis.broker.api.produce.ProducerSessionManager.SessionError.{InvalidSchema, UnknownMessageType}
import vertis.broker.api.produce.storage.StorageProducerStatistics
import vertis.broker.model.ModelGenerators
import vertis.broker.tests.ProducerSessionSupport.DummyValidator
import vertis.core.model.DataCenters
import vertis.logbroker.client.test.LbTest
import vertis.zio.test.{ZioEventually, ZioSpecBase}
import zio._

import java.time.Period

/** @author zvez
  */
class ProducerSessionManagerIntSpec extends ZioSpecBase with LbTest with ZioEventually {

  private type MessageType = SimpleMessage
  private val messageDescriptor = SimpleMessage.getDescriptor
  private val recursiveMessageDescriptor = RecursiveMessage.getDescriptor

  private val configSource = new StreamConfigSource {

    override def configForType(messageType: String): IO[UnknownMessageType, ApiStreamConfig] =
      if (messageType == messageDescriptor.getFullName || messageType == recursiveMessageDescriptor.getFullName)
        UIO(
          ApiStreamConfig(topicNameFromClassName, messageType, topicNameFromClassName, 1, Period.ofDays(7).fromJava)
        )
      else ZIO.fail(UnknownMessageType(messageType))
  }

  private val schemaVersion = "v0.0.4700"

  private lazy val makeSessionManager =
    (for {
      lbClient <- makeLbClientM
      _ <- TestSraas.setJavaDescriptor { case DescriptorKey(protoMessageName, version) =>
        protoMessageName match {
          case name if name == messageDescriptor.getFullName =>
            UIO(SraasDescriptor(messageDescriptor, protoMessageName, version.toString))
          case name if name == recursiveMessageDescriptor.getFullName =>
            UIO(SraasDescriptor(recursiveMessageDescriptor, protoMessageName, version.toString))
          case _ => ZIO.fail(new DescriptorNotFound(protoMessageName, version))
        }
      }.toManaged_
      sraas <- ZManaged.service[Sraas.Service]
      sharedComponents = app.SharedComponents(sraas, DummyValidator, configSource)
      manager <- ProducerSessionManager.make(
        lbClient,
        sharedComponents,
        BrokerInstanceInfo.Empty,
        DataCenters.Sas,
        Set.empty,
        2 * Mb
      )
    } yield manager).provideLayer(env ++ TestSraas.layer)

  "ProducerSessionManager" should {
    "init -> write -> close" in ioTest {
      val init = ProducerSessionConfig(
        messageType = messageDescriptor.getFullName,
        sourceId = "test",
        schemaVersion = schemaVersion,
        clientInfo = ProducerInfo("test")
      )
      (makeSessionManager >>= (_.openSession(init))).use { session =>
        val messages =
          ModelGenerators.writeDataSequence[MessageType].take(1000).toSeq
        for {
          responses <- ZIO.foreachPar(messages) { msg =>
            session.write(msg).either
          }
          _ <- check {
            responses.foreach { r =>
              r.isRight shouldBe true
            }
            responses.size shouldBe messages.size
          }
          _ <- checkEventually {
            session.asInstanceOf[ProducerSessionImpl].producer.statistics.flatMap { stats =>
              check {
                stats shouldBe StorageProducerStatistics.Empty
              }
            }
          }
        } yield ()
      }
    }

    "fail on unknown message type" in ioTest {
      val init =
        ProducerSessionConfig("something", "test", "1", ProducerInfo("test"))
      makeSessionManager.use { manager =>
        manager.openSession(init).either.use { result =>
          check {
            result should matchPattern { case Left(UnknownMessageType(_)) =>
            }
          }
        }
      }
    }

    "fail on recursive message type" in ioTest {
      val init =
        ProducerSessionConfig(
          "realty",
          recursiveMessageDescriptor.getFullName,
          schemaVersion,
          ProducerInfo("Leo Bloom")
        )
      makeSessionManager.use { manager =>
        manager.openSession(init).either.use { result =>
          check {
            result should matchPattern {
              case Left(InvalidSchema(msg)) if msg.contains("recursive") =>
            }
          }
        }
      }
    }
  }

}
