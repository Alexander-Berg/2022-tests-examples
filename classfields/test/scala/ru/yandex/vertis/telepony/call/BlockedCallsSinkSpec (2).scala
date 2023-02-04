package ru.yandex.vertis.telepony.call

import org.mockito.Mockito.verify
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.model.TeleponyCall
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.generator.TeleponyCallGenerator.TeleponyRedirectBlockedCallGen
import ru.yandex.vertis.util.concurrent.Threads

import scala.concurrent.Future

class BlockedCallsSinkSpec extends SpecBase with ScalaCheckPropertyChecks with MockitoSupport {

  trait TestEnvironment {
    val brokerClientMock = mock[BrokerClient]
    when(brokerClientMock.send(?[Option[String]], ?)(?)).thenReturn(Future.unit)

    val blockedCallsSink = new BlockedCallsSink(brokerClientMock)
  }

  "BlockedCallsSink.write" should {
    "write to broker" in {
      val call: TeleponyCall = TeleponyRedirectBlockedCallGen.next
      new TestEnvironment {
        blockedCallsSink.write(call)(Threads.SameThreadEc).futureValue

        verify(brokerClientMock).send(?[Option[String]], ?)(?)
      }
    }
  }
}
