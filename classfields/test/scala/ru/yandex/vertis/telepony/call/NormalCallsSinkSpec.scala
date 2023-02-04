package ru.yandex.vertis.telepony.call

import org.mockito.Mockito.verify
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.TeleponyCallGenerator.TeleponyNonRedirectBlockedCallGen
import ru.yandex.vertis.telepony.journal.Journal.{EventEnvelope, KafkaMetadata}
import ru.yandex.vertis.telepony.journal.WriteJournal
import ru.yandex.vertis.telepony.model.TeleponyCall
import ru.yandex.vertis.util.concurrent.Threads

import scala.concurrent.Future

class NormalCallsSinkSpec extends SpecBase with ScalaCheckPropertyChecks with MockitoSupport {

  trait TestEnvironment {
    val brokerClientMock = mock[BrokerClient]
    when(brokerClientMock.send(?, ?, ?)(?)).thenReturn(Future.unit)

    val writeJournalMock = mock[WriteJournal[TeleponyCall]]
    when(writeJournalMock.send(?)).thenReturn(
      Future.successful(EventEnvelope[KafkaMetadata, TeleponyCall](mock[KafkaMetadata], mock[TeleponyCall]))
    )

    val normalCallsSink = new NormalCallsSink(writeJournalMock, brokerClientMock)
  }

  "NormalCallsSink.write" should {
    "always write to journal" in {
      forAll(TeleponyNonRedirectBlockedCallGen) { (call: TeleponyCall) =>
        new TestEnvironment {
          normalCallsSink.write(call)(Threads.SameThreadEc).futureValue

          verify(writeJournalMock).send(?)
        }
      }
    }

    "write to broker" when {
      "sendToBroker is true" in {
        forAll(TeleponyNonRedirectBlockedCallGen) { call: TeleponyCall =>
          new TestEnvironment {
            normalCallsSink.write(call)(Threads.SameThreadEc).futureValue

            verify(brokerClientMock).send(?, ?, ?)(?)
          }
        }
      }
    }
  }
}
