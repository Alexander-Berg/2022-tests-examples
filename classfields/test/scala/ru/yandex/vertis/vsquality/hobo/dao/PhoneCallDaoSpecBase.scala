package ru.yandex.vertis.vsquality.hobo.dao

import ru.yandex.vertis.vsquality.hobo.PhoneCallFilter
import ru.yandex.vertis.vsquality.hobo.exception.{AlreadyExistException, NotExistException}
import ru.yandex.vertis.vsquality.hobo.model.{PhoneCall, PhoneCallKey}
import ru.yandex.vertis.vsquality.hobo.util._
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.DaoGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.hobo.proto.Model.PhoneCall.PhoneCallStatus

/**
  * @author semkagtn
  */
trait PhoneCallDaoSpecBase extends SpecBase {

  def phoneCallDao: PhoneCallDao

  before {
    phoneCallDao.clear().futureValue
  }

  "insert" should {

    "correctly add new phone call" in {
      val expectedPhoneCall = PhoneCallGen.next

      val returned = phoneCallDao.insert(expectedPhoneCall).futureValue
      returned should smartEqual(expectedPhoneCall)

      val actualPhoneCall = getByKey(expectedPhoneCall.key)

      actualPhoneCall should smartEqual(expectedPhoneCall)
    }

    "throw exception if phone call already exist" in {
      val phoneCall = PhoneCallGen.next
      phoneCallDao.insert(phoneCall).futureValue

      whenReady(phoneCallDao.insert(phoneCall).failed) { e =>
        e shouldBe a[AlreadyExistException]
      }
    }
  }

  "update" should {

    "correctly update phone call" in {
      val phoneCall = PhoneCallGen.next
      val phoneCallInfo = PhoneCallInfoGen.next
      val expectedPhoneCall =
        phoneCall.copy(
          status = phoneCallInfo.status,
          duration = phoneCallInfo.duration
        )

      phoneCallDao.insert(phoneCall).futureValue
      phoneCallDao.update(phoneCall.key, phoneCallInfo).futureValue

      val actualPhoneCall = getByKey(phoneCall.key)
      actualPhoneCall should smartEqual(expectedPhoneCall)
    }

    "throw exception if phone call doesn't exist" in {
      val key = PhoneCallKeyGen.next
      val phoneCallInfo = PhoneCallInfoGen.next

      whenReady(phoneCallDao.update(key, phoneCallInfo).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }
  }

  "find" should {

    "return correct phone calls by user" in {
      val phoneCall1 = PhoneCallGen.next
      val phoneCall2 = PhoneCallGen.next
      assume(phoneCall1.user != phoneCall2.user, "Users must be different")
      phoneCallDao.insert(phoneCall1).futureValue
      phoneCallDao.insert(phoneCall2).futureValue

      val slice = Range(0, 2)
      val filter = PhoneCallFilter.Composite(user = Use(phoneCall1.user))
      val actualPhoneCalls = phoneCallDao.find(filter, slice).futureValue
      val expectedPhoneCalls = SlicedResult(Seq(phoneCall1), 1, slice)
      actualPhoneCalls should smartEqual(expectedPhoneCalls)
    }

    "return correct phone calls by task = Some(task)" in {
      val phoneCall1 = PhoneCallGen.next.copy(task = Some(TaskKeyGen.next))
      val phoneCall2 = PhoneCallGen.next.copy(task = Some(TaskKeyGen.next))
      assume(phoneCall1.task != phoneCall2.task, "Tasks must be different")
      phoneCallDao.insert(phoneCall1).futureValue
      phoneCallDao.insert(phoneCall2).futureValue

      val slice = Range(0, 2)
      val filter = PhoneCallFilter.Composite(task = Use(phoneCall1.task))
      val actualPhoneCalls = phoneCallDao.find(filter, slice).futureValue
      val expectedPhoneCalls = SlicedResult(Seq(phoneCall1), 1, slice)
      actualPhoneCalls should smartEqual(expectedPhoneCalls)
    }

    "return correct phone calls by task = None" in {
      val phoneCall1 = PhoneCallGen.next.copy(task = Some(TaskKeyGen.next))
      val phoneCall2 = PhoneCallGen.next.copy(task = None)
      phoneCallDao.insert(phoneCall1).futureValue
      phoneCallDao.insert(phoneCall2).futureValue

      val slice = Range(0, 2)
      val filter = PhoneCallFilter.Composite(task = Use(None))
      val actualPhoneCalls = phoneCallDao.find(filter, slice).futureValue
      val expectedPhoneCalls = SlicedResult(Seq(phoneCall2), 1, slice)
      actualPhoneCalls should smartEqual(expectedPhoneCalls)
    }

    "return correct phone calls by Some(resourceId)" in {
      val phoneCall1 = PhoneCallGen.next.copy(resourceId = Some(ResourceIdGen.next))
      val phoneCall2 = PhoneCallGen.next.copy(resourceId = Some(ResourceIdGen.next))
      assume(phoneCall1.resourceId != phoneCall2.resourceId, "Resource ids must be different")
      phoneCallDao.insert(phoneCall1).futureValue
      phoneCallDao.insert(phoneCall2).futureValue

      val slice = Range(0, 2)
      val filter = PhoneCallFilter.Composite(resourceId = Use(phoneCall1.resourceId))
      val actualPhoneCalls = phoneCallDao.find(filter, slice).futureValue
      val expectedPhoneCalls = SlicedResult(Seq(phoneCall1), 1, slice)
      actualPhoneCalls should smartEqual(expectedPhoneCalls)
    }

    "return correct phone calls by resourceId = None" in {
      val phoneCall1 = PhoneCallGen.next.copy(resourceId = Some(ResourceIdGen.next))
      val phoneCall2 = PhoneCallGen.next.copy(resourceId = None)
      phoneCallDao.insert(phoneCall1).futureValue
      phoneCallDao.insert(phoneCall2).futureValue

      val slice = Range(0, 2)
      val filter = PhoneCallFilter.Composite(resourceId = Use(None))
      val actualPhoneCalls = phoneCallDao.find(filter, slice).futureValue
      val expectedPhoneCalls = SlicedResult(Seq(phoneCall2), 1, slice)
      actualPhoneCalls should smartEqual(expectedPhoneCalls)
    }

    "return correct phone calls by status" in {
      val phoneCall1 = PhoneCallGen.next.copy(status = PhoneCallStatus.ANSWER)
      val phoneCall2 = PhoneCallGen.next.copy(status = PhoneCallStatus.BUSY)
      phoneCallDao.insert(phoneCall1).futureValue
      phoneCallDao.insert(phoneCall2).futureValue

      val slice = Range(0, 2)
      val filter = PhoneCallFilter.Composite(status = Use(phoneCall1.status))
      val actualPhoneCalls = phoneCallDao.find(filter, slice).futureValue
      val expectedPhoneCalls = SlicedResult(Seq(phoneCall1), 1, slice)
      actualPhoneCalls should smartEqual(expectedPhoneCalls)
    }

    "return correct phone calls by create time" in {
      val now = DateTimeUtil.now()
      val phoneCall1 = PhoneCallGen.next.copy(createTime = now)
      val phoneCall2 = PhoneCallGen.next.copy(createTime = now.plusDays(2))
      phoneCallDao.insert(phoneCall1).futureValue
      phoneCallDao.insert(phoneCall2).futureValue

      val slice = Range(0, 2)
      val interval = TimeInterval(Some(now.minusDays(1)), Some(now.plusDays(1)))
      val filter = PhoneCallFilter.Composite(createTime = Use(interval))
      val actualPhoneCalls = phoneCallDao.find(filter, slice).futureValue
      val expectedPhoneCalls = SlicedResult(Seq(phoneCall1), 1, slice)
      actualPhoneCalls should smartEqual(expectedPhoneCalls)
    }

    "return correct phone calls by several filters" in {
      val user = UserIdGen.next
      val task = Some(TaskKeyGen.next)
      val phoneCall1 = PhoneCallGen.next.copy(user = user, task = task)
      val phoneCall2 = PhoneCallGen.next.copy(user = user)
      val phoneCall3 = PhoneCallGen.next.copy(task = task)
      assume(task != phoneCall2.task, "tasks must be different")
      assume(user != phoneCall3.user, "users must be different")

      phoneCallDao.insert(phoneCall1).futureValue
      phoneCallDao.insert(phoneCall2).futureValue
      phoneCallDao.insert(phoneCall3).futureValue

      val slice = Range(0, 3)
      val filter = PhoneCallFilter.Composite(user = Use(user), task = Use(task))
      val actualPhoneCalls = phoneCallDao.find(filter, slice).futureValue
      val expectedPhoneCalls = SlicedResult(Seq(phoneCall1), 1, slice)
      actualPhoneCalls should smartEqual(expectedPhoneCalls)
    }

    "return correct slice" in {
      val phoneCalls = PhoneCallGen.next(2).toList

      for (phoneCall <- phoneCalls) phoneCallDao.insert(phoneCall).futureValue

      val filter = PhoneCallFilter.Composite()
      val allPhoneCalls = phoneCallDao.find(filter, Range(0, 2)).futureValue.values
      allPhoneCalls.size should smartEqual(2)

      val firstPhoneCall = phoneCallDao.find(filter, Range(0, 1)).futureValue.values
      firstPhoneCall should smartEqual(allPhoneCalls.dropRight(1))

      val secondPhoneCall = phoneCallDao.find(filter, Range(1, 2)).futureValue.values
      secondPhoneCall should smartEqual(allPhoneCalls.drop(1))
    }
  }

  "remove before" should {

    "remove correct phone calls" in {
      val createdBefore = DateTimeGen.next
      val phoneCall = PhoneCallGen.next.copy(createTime = createdBefore.plus(1))
      val phoneCallsToDelete = PhoneCallGen.next(2).toList.map(_.copy(createTime = createdBefore.minus(1)))

      phoneCallDao.insert(phoneCall).futureValue
      for (call <- phoneCallsToDelete) phoneCallDao.insert(call).futureValue

      phoneCallDao.remove(createdBefore).futureValue

      val slice = Range(0, 3)
      val actualPhoneCalls = phoneCallDao.find(PhoneCallFilter.Composite(), slice).futureValue
      val expectedPhoneCalls = SlicedResult(Seq(phoneCall), 1, slice)

      actualPhoneCalls should smartEqual(expectedPhoneCalls)
    }
  }

  "remove by keys" should {

    "remove correct phone calls" in {
      val n = 10
      val phoneCalls = PhoneCallGen.next(n)
      val keysToRemove = phoneCalls.map(_.key).slice(0, phoneCalls.size / 2).toSet
      val expectedRemainingCalls = phoneCalls.filterNot(call => keysToRemove.contains(call.key)).toSet
      phoneCalls.foreach(phoneCallDao.insert(_).futureValue)
      phoneCallDao.remove(keysToRemove).futureValue
      val actualRemainingCalls = phoneCallDao.find(PhoneCallFilter.Empty, Range(0, n)).futureValue.values.toSet

      actualRemainingCalls shouldBe expectedRemainingCalls
    }
  }

  private def getByKey(key: PhoneCallKey): PhoneCall =
    phoneCallDao
      .find(PhoneCallFilter.Composite(), Range(0, 10))
      .futureValue
      .values
      .find(_.key == key)
      .getOrElse(throw new AssertionError(s"Phone call $key not found"))
}
