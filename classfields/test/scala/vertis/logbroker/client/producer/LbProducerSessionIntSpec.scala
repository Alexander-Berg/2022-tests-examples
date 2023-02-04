package vertis.logbroker.client.producer

import java.util.concurrent.atomic.AtomicLong
import ru.yandex.kikimr.persqueue.compression.CompressionCodec.RAW
import vertis.logbroker.client.producer.config.LbProducerSessionConfig
import vertis.logbroker.client.producer.model.Message
import vertis.logbroker.client.model.LogbrokerError.SessionIsClosedError
import vertis.logbroker.client.test.LbTest

/** @author zvez
  */
class LbProducerSessionIntSpec extends LbTest {

  "LbProducerSession" should {
    "close itself in case of new session with the same sourceId was opened" in ioTest {
      producerSession("session").use { session1 =>
        // write message to make sure it is inited
        session1.write(makeMessage()).flatten *>
          producerSession("session").use { session2 =>
            for {
              _ <- session2.write(makeMessage()).flatten
              result <- session1.write(makeMessage()).flatten.either
              _ <- check {
                result.isLeft shouldBe true
                result.left.toOption.get shouldBe a[SessionIsClosedError]
              }
            } yield ()
          }
      }
    }
  }

  private def producerSession(sourceId: String, topic: String = topicNameFromClassName) = {
    for {
      balancer <- transportFactoryM
      facade <- lbLocalFacadeM(balancer)
      sessionConfig = LbProducerSessionConfig(topic, sourceId)
      makeProducer = facade.makeProducerSession(sessionConfig) _
      session <- LbProducerSession.makeSession(sessionConfig, makeProducer)
    } yield session
  }

  private val Seq = new AtomicLong(0)

  private def makeMessage() = {
    val i = Seq.incrementAndGet()
    Message.raw(i, s"some data $i".getBytes)
  }

}
