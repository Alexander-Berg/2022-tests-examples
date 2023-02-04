package vertis.pushnoy.services.xiva

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.BeforeAndAfterEach
import ru.auto.api.ResponseModel.{ResponseStatus, SuccessResponse}
import ru.yandex.vertis.generators.BasicGenerators.readableString
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.ops.test.TestOperationalSupport
import vertis.pushnoy.dao.TestDao.TestException
import vertis.pushnoy.dao.{Dao, TestDao}
import vertis.pushnoy.gen.ModelGenerators._
import vertis.pushnoy.model.request.PushMessageV1
import vertis.pushnoy.model.request.params.{AllTarget, Devices}
import vertis.pushnoy.model.response.PushCounterResponse
import vertis.pushnoy.model.template.Template
import vertis.pushnoy.services.PushBanChecker
import vertis.pushnoy.services.xiva.TestXivaClient.InvalidId
import vertis.pushnoy.{PushnoySpecBase, TestClients, TestEnvironment}

/** testing logic of adding token
  *
  * @author kusaeva
  */
class XivaManagerSpec
  extends PushnoySpecBase
  with TestClients
  with ProducerProvider
  with TestEnvironment
  with XivaManagerSupport
  with BeforeAndAfterEach
  with TypeCheckedTripleEquals {

  override def dao: Dao = new TestDao

  override def pushBanChecker: PushBanChecker = new PushBanChecker {
    override def isPushBanned(push: PushMessageV1): Boolean = false

    override def isPushBanned(template: Template): Boolean = false
  }

  trait XivaManagerWithBannedTopic {
    import scala.concurrent.ExecutionContext.Implicits.global

    protected val bannedEvent: String

    private val pushBanCheckerWithBannedEvent = new PushBanChecker {
      override def isPushBanned(push: PushMessageV1): Boolean = push.event === bannedEvent

      override def isPushBanned(template: Template): Boolean = false
    }

    protected val xivaManager = new XivaManager(
      xivaConfig,
      dao,
      xivaClient,
      eventsManager,
      appleDeviceCheckClient,
      deviceChecker,
      pushBanCheckerWithBannedEvent,
      TestOperationalSupport.prometheusRegistry
    )
  }

  private val success = SuccessResponse
    .newBuilder()
    .setStatus(ResponseStatus.SUCCESS)
    .build()

  private val pushCounter = PushCounterResponse(1)
  private val zeroPushCounter = PushCounterResponse(0)

  "XivaManager" when {
    val device = DeviceGen.next
    val token = TokenInfoGen.next

    Seq("add new token", "add changed token").foreach { testName =>
      testName should {
        "save to db if subscribe succeed" in {
          xivaManager.addToken(device, token)(ctx).futureValue shouldBe success
        }
        "not save to db if subscribe failed" in {
          val invalidDevice = device.copy(id = InvalidId)
          xivaManager.addToken(invalidDevice, token).failed.futureValue shouldBe a[TestException]
        }
      }
    }

    "add existing token" should {
      "return success" in {
        xivaManager.addToken(device, token).futureValue shouldBe success
      }
    }

    "change hidden status" should {
      "save to db" in {
        xivaManager.addToken(device, token).futureValue shouldBe success
      }
    }

    "push to device" should {
      "save history" in {
        val pushName = readableString.next
        val pushMessage = PushMessageGen.next

        xivaManager
          .pushToDevice(device, pushMessage, None, pushName = Some(pushName))(ctx)
          .futureValue shouldBe pushCounter
      }
    }

    "push to user with target devices" should {
      "save push history" in {
        val user = userGen(device.client).next
        val pushName = readableString.next
        val pushMessage = PushMessageGen.next

        xivaManager
          .pushToUser(user, target = Devices, pushMessage, None, pushName = Some(pushName))(ctx)
          .futureValue shouldBe pushCounter
      }
    }

    "push to user with target all" should {
      "save push history" in {
        val user = userGen(device.client).next
        val pushName = readableString.next
        val pushMessage = PushMessageGen.next

        xivaManager
          .pushToUser(user, target = AllTarget, pushMessage, None, pushName = Some(pushName))(ctx)
          .futureValue shouldBe pushCounter
      }
    }

    "saving failed not push" should {
      val invalidDevice = device.copy(id = InvalidId)
      "to device" in {
        val pushName = readableString.next
        val pushMessage = PushMessageGen.next

        xivaManager
          .pushToDevice(invalidDevice, pushMessage, None, pushName = Some(pushName))(ctx)
          .futureValue shouldBe zeroPushCounter
      }
      "to user" in {
        val user = userGen(device.client).next.copy(id = InvalidId)
        val pushName = readableString.next
        val pushMessage = PushMessageGen.next

        xivaManager
          .pushToUser(user, target = Devices, pushMessage, None, pushName = Some(pushName))(ctx)
          .futureValue shouldBe zeroPushCounter

        xivaManager
          .pushToUser(user, target = AllTarget, pushMessage, None, pushName = Some(pushName))(ctx)
          .futureValue shouldBe zeroPushCounter
      }
    }

    "has no push name" should {
      "don't save history" in {

        xivaManager
          .savePushHistory(None, Seq(device))(ctx)
          .futureValue shouldBe ()
      }
    }

    "an event is banned" should {
      "not send pushes to a user" in new XivaManagerWithBannedTopic {
        override val bannedEvent: String = "banned_event"

        private val user = userGen(device.client).next
        private val pushName = readableString.next
        private val pushMessage = PushMessageGen.next.copy(event = bannedEvent)
        xivaManager
          .pushToUser(user, AllTarget, pushMessage, None, pushName = Some(pushName))
          .futureValue shouldBe zeroPushCounter
      }

      "not send pushes to a device" in new XivaManagerWithBannedTopic {
        override val bannedEvent: String = "banned_event"

        private val pushName = readableString.next
        private val pushMessage = PushMessageGen.next.copy(event = bannedEvent)
        xivaManager
          .pushToDevice(device, pushMessage, None, pushName = Some(pushName))
          .futureValue shouldBe zeroPushCounter
      }
    }
  }
}
