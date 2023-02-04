package ru.yandex.vertis.vsquality.hobo.service

import org.mockito.{ArgumentMatchers => m}
import ru.yandex.vertis.vsquality.hobo.dao.PhoneCallDao
import ru.yandex.vertis.vsquality.hobo.exception.NotExistException
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.vsquality.hobo.model.{PhoneCall, PhoneCallKey, QueueSettings, TeleponyDomain, UserId}
import ru.yandex.vertis.hobo.proto.Model.PhoneCall.PhoneCallStatus
import ru.yandex.vertis.vsquality.hobo.service.impl.PhoneCallServiceImpl
import ru.yandex.vertis.vsquality.hobo.service.impl.inmemory.InMemoryReadUserSupport
import ru.yandex.vertis.vsquality.hobo.telephony.impl.StubTelephonyClient
import ru.yandex.vertis.vsquality.hobo.telepony.StubAnyDomainTeleponyClient
import ru.yandex.vertis.vsquality.hobo.telepony.StubAnyDomainTeleponyClient.StubRedirect
import ru.yandex.vertis.vsquality.hobo.util.{DateTimeUtil, Range, SpecBase, Use}
import ru.yandex.vertis.vsquality.hobo.{PhoneCallFilter, PhoneCallInfo}
import ru.yandex.vertis.vsquality.hobo.telepony.protocol.utils.CreateRedirectRequest
import ru.yandex.vertis.vsquality.hobo.telepony.protocol.model.{Phone => TeleponyPhone}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Base specs on [[PhoneCallServiceImpl]]
  *
  * @author semkagtn
  */
trait PhoneCallServiceImplSpecBase extends SpecBase {

  def phoneCallDaoImpl: PhoneCallDao

  implicit private val rc: AutomatedContext = AutomatedContext("unit-test")

  private val user = UserGen.next.copy(phoneSettings = Some(PhoneSettingsGen.next))
  private val userWithoutPhone = UserGen.next.copy(phoneSettings = None)
  private val nonexistentUser = UserGen.next

  private val readUserSupport = new InMemoryReadUserSupport(Iterable(user, userWithoutPhone))
  private val telephonyClient = spy(new StubTelephonyClient)
  private val teleponyClient = spy(new StubAnyDomainTeleponyClient)
  private val queueSettingsService = mock[QueueSettingsService]
  private val taskService = mock[TaskService]
  private lazy val phoneCallDao = spy(phoneCallDaoImpl)

  private lazy val phoneCallService: PhoneCallService =
    new PhoneCallServiceImpl(
      readUserSupport = readUserSupport,
      phoneCallDao = phoneCallDao,
      telephonyClient = telephonyClient,
      teleponyClient = teleponyClient,
      redirectParams = RedirectParams.Instantly,
      queueSettingsService = queueSettingsService,
      taskService = taskService
    )

  before {
    phoneCallDao.clear().futureValue
  }

  "call" should {

    "correctly execute method without provider" in {
      val taskKey = Some(TaskKeyGen.next)
      val resourceId = Some(ResourceIdGen.next)
      val recipient = PhoneGen.next

      val expectedPhoneCall = phoneCallService.call(user.key, None, taskKey, None, resourceId, recipient).futureValue
      val actualPhoneCall = getPhone(expectedPhoneCall.key)
      expectedPhoneCall should be(actualPhoneCall)

      val key = actualPhoneCall.key
      val provider = user.phoneSettings.get.provider
      val from = user.phoneSettings.get.phone
      val to = recipient
      there.was(one(telephonyClient).makeCall(key, provider, from, to))
    }

    "correctly execute method with provider" in {
      val taskKey = Some(TaskKeyGen.next)
      val provider = ProviderGen.filter(_ != user.phoneSettings.get.provider).next
      val resourceId = Some(ResourceIdGen.next)
      val recipient = PhoneGen.next

      val expectedPhoneCall =
        phoneCallService.call(user.key, Some(provider), taskKey, None, resourceId, recipient).futureValue
      val actualPhoneCall = getPhone(expectedPhoneCall.key)
      expectedPhoneCall should be(actualPhoneCall)

      val key = actualPhoneCall.key
      val from = user.phoneSettings.get.phone
      val to = recipient
      there.was(one(telephonyClient).makeCall(key, provider, from, to))
    }

    "throw an exception if user doesn't have phone" in {
      val taskKey = Some(TaskKeyGen.next)
      val resourceId = Some(ResourceIdGen.next)
      val recipient = PhoneGen.next

      whenReady(phoneCallService.call(userWithoutPhone.key, None, taskKey, None, resourceId, recipient).failed) { e =>
        e shouldBe a[IllegalArgumentException]
      }
    }

    "throw an exception if user doesn't exist" in {
      val taskKey = Some(TaskKeyGen.next)
      val resourceId = Some(ResourceIdGen.next)
      val recipient = PhoneGen.next

      whenReady(phoneCallService.call(nonexistentUser.key, None, taskKey, None, resourceId, recipient).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "correctly use proxy when redirect is enabled for queue" in {
      val taskKey = TaskKeyGen.next
      val queueId = QueueIdGen.next
      val resourceId = Some(ResourceIdGen.next)
      val recipient = CorrectPhoneGen.next

      val defaultSettings = QueueSettings.defaultForQueue(queueId)
      val teleponyDomain = TeleponyDomainGen.suchThat(_ != TeleponyDomain.NoneDomain).next

      doReturn(Future.successful(TaskGen.next))
        .when(taskService)
        .get(queueId, taskKey)
      doReturn(
        Future.successful(defaultSettings.copy(mutable = defaultSettings.mutable.copy(teleponyDomain = teleponyDomain)))
      )
        .when(queueSettingsService)
        .get(queueId)

      val expectedPhoneCall =
        phoneCallService.call(user.key, None, Some(taskKey), Some(queueId), resourceId, recipient).futureValue
      val actualPhoneCall = getPhone(expectedPhoneCall.key)
      expectedPhoneCall should be(actualPhoneCall)

      val key = actualPhoneCall.key
      val provider = user.phoneSettings.get.provider
      val from = user.phoneSettings.get.phone
      val to = StubRedirect.source.value
      there.was(one(telephonyClient).makeCall(key, provider, from, to))
      there.was(
        one(teleponyClient).getOrCreateRedirect(
          teleponyDomain,
          user.key.key,
          CreateRedirectRequest(
            target = TeleponyPhone(recipient),
            preferredOperator = Some(CreateRedirectRequest.Operators.Mts),
            ttl = Some(0),
            options = Some(CreateRedirectRequest.RedirectOptions(callerIdMode = true, needAnswer = true))
          )
        )
      )
    }

    "do not use proxy when specified queue with disabled redirect" in {
      val taskKey = TaskKeyGen.next
      val queueId = QueueIdGen.next
      val resourceId = Some(ResourceIdGen.next)
      val recipient = CorrectPhoneGen.next

      val defaultSettings = QueueSettings.defaultForQueue(queueId)

      doReturn(Future.successful(TaskGen.next))
        .when(taskService)
        .get(queueId, taskKey)
      doReturn(Future.successful(defaultSettings))
        .when(queueSettingsService)
        .get(queueId)

      val expectedPhoneCall =
        phoneCallService.call(user.key, None, Some(taskKey), Some(queueId), resourceId, recipient).futureValue
      val actualPhoneCall = getPhone(expectedPhoneCall.key)
      expectedPhoneCall should be(actualPhoneCall)

      val key = actualPhoneCall.key
      val provider = user.phoneSettings.get.provider
      val from = user.phoneSettings.get.phone
      val to = recipient
      there.was(one(telephonyClient).makeCall(key, provider, from, to))
    }

    "throw exception if task is not from specified queue" in {
      val taskKey = TaskKeyGen.next
      val queueId = QueueIdGen.next
      val resourceId = Some(ResourceIdGen.next)
      val recipient = CorrectPhoneGen.next

      doReturn(Future.failed(NotExistException(""))).when(taskService).get(queueId, taskKey)

      val a =
        phoneCallService.call(user.key, None, Some(taskKey), Some(queueId), resourceId, recipient).failed.futureValue
      println(a)
      phoneCallService
        .call(user.key, None, Some(taskKey), Some(queueId), resourceId, recipient)
        .shouldCompleteWithException[NotExistException]
    }
  }

  "find" should {

    "execute correct method" in {
      val filter = PhoneCallFilter.Composite(user = Use(UserId.Hobo("test")))
      val slice = Range(0, 10)
      phoneCallService.find(filter, slice).futureValue
      there.was(one(phoneCallDao).find(filter, slice))
    }
  }

  "remove" should {

    "execute correct method" in {
      val keys = Set.empty[PhoneCallKey]
      phoneCallService.remove(keys).futureValue
      there.was(one(phoneCallDao).remove(keys))
    }
  }

  "update" should {

    "correctly update phone call" in {
      val phoneCall = PhoneCallGen.next
      val key = phoneCall.key
      phoneCallDao.insert(phoneCall).futureValue

      phoneCallService.update(key).futureValue

      there.was(one(telephonyClient).getInfo(key))
      there.was(one(phoneCallDao).update(m.eq(key), m.any[PhoneCallInfo]))
    }

    "throw an exception if phone call doesn't exist" in {
      val key = PhoneCallKeyGen.next

      whenReady(phoneCallService.update(key).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }
  }

  "getReadyToUpdate" should {

    "return correct keys" in {
      val phoneCall1 =
        PhoneCallGen.next.copy(
          status = PhoneCallStatus.UNKNOWN,
          createTime = DateTimeUtil.now().minusDays(1)
        )
      val phoneCall2 =
        PhoneCallGen.next.copy(
          status = PhoneCallStatusGen.filter(_ != PhoneCallStatus.UNKNOWN).next,
          createTime = DateTimeUtil.now().minusDays(1)
        )
      phoneCallDao.insert(phoneCall1).futureValue
      phoneCallDao.insert(phoneCall2).futureValue

      val actualResult = phoneCallService.getReadyToUpdate(limit = 2, olderThan = 0.days).futureValue
      val expectedResult = Set(phoneCall1.key)

      actualResult should be(expectedResult)
    }

    "return no more than limit" in {
      val phoneCall1 =
        PhoneCallGen.next.copy(
          status = PhoneCallStatus.UNKNOWN,
          createTime = DateTimeUtil.now().minusDays(1)
        )
      val phoneCall2 =
        PhoneCallGen.next.copy(
          status = PhoneCallStatus.UNKNOWN,
          createTime = DateTimeUtil.now().minusDays(1)
        )
      phoneCallDao.insert(phoneCall1).futureValue
      phoneCallDao.insert(phoneCall2).futureValue

      val actualResult = phoneCallService.getReadyToUpdate(limit = 1, olderThan = 0.days).futureValue
      actualResult.size should be(1)
    }

    "return only older than specified duration" in {
      val oldPhoneCall =
        PhoneCallGen.next.copy(
          status = PhoneCallStatus.UNKNOWN,
          createTime = DateTimeUtil.now().minusHours(2)
        )
      val newPhoneCall =
        PhoneCallGen.next.copy(
          status = PhoneCallStatus.UNKNOWN,
          createTime = DateTimeUtil.now().minusMinutes(20)
        )
      phoneCallDao.insert(oldPhoneCall).futureValue
      phoneCallDao.insert(newPhoneCall).futureValue

      val actualResult = phoneCallService.getReadyToUpdate(limit = 1, olderThan = 1.hour).futureValue
      val expectedResult = Set(oldPhoneCall.key)
      actualResult should be(expectedResult)
    }
  }

  private def getPhone(key: PhoneCallKey): PhoneCall =
    phoneCallDao
      .find(PhoneCallFilter.Empty, Range(0, 10))
      .futureValue
      .values
      .find(_.key == key)
      .getOrElse(throw new AssertionError(s"Phone call $key not found"))
}
