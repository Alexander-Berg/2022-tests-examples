package ru.yandex.vertis.telepony.dao

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.CallService.Filter
import ru.yandex.vertis.telepony.util.Page
import ru.yandex.vertis.telepony.util.Range.Full
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}

import scala.concurrent.duration._

/**
  * @author evans
  */
trait BannedCallDaoV2Spec extends SpecBase with BeforeAndAfterEach with DatabaseSpec {

  def callDao: BannedCallDao

  def numberDao: OperatorNumberDaoV2

  def redirectDao: RedirectDaoV2

  private val Moscow: GeoId = 213

  private def forNumber(phone: Phone) = {
    OperatorNumber(phone, OperatorAccounts.MtsShared, Operators.Mts, Moscow, PhoneTypes.Local, Status.New(None), None)
  }

  override protected def beforeEach(): Unit = {
//    callDao.clear().databaseValue.futureValue
//    redirectDao.clear().databaseValue.futureValue
//    numberDao.clear().databaseValue.futureValue
    super.beforeEach()
  }

  override protected def afterEach(): Unit = {
//    callDao.clear().databaseValue.futureValue
//    redirectDao.clear().databaseValue.futureValue
//    numberDao.clear().databaseValue.futureValue
    super.afterEach()
  }

//  private val phone: OperatorNumber = forNumber(PhoneGen.next)
//  private val target = PhoneGen.next

  private def sampleFor(from: RefinedSource, proxy: ActualRedirect) = {
    val id = ShortStr.next
    BannedCall(id, DateTime.now, DateTime.now, id, Some(from), proxy.asHistoryRedirect, DateTime.now, 20.seconds)
  }

  def withPhoneAndTarget(f: (OperatorNumber, Phone) => Unit) = {
    f(forNumber(PhoneGen.next), PhoneGen.next)
  }

  "CallDao" should {
    "find call" in withPhoneAndTarget { (phone, target) =>
      numberDao.create(phone).databaseValue.futureValue
      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue
      val call = sampleFor(RefinedSourceGen.next, red)
      callDao.create(call).databaseValue.futureValue
      callDao.exists(call.id).databaseValue.futureValue shouldEqual true
    }

    "not find call" in withPhoneAndTarget { (phone, target) =>
      numberDao.create(phone).databaseValue.futureValue
      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue
      val call = sampleFor(RefinedSourceGen.next, red)
      callDao.exists(call.id).databaseValue.futureValue shouldEqual false
    }

    "create call" in withPhoneAndTarget { (phone, target) =>
      numberDao.create(phone).databaseValue.futureValue
      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue
      val call = sampleFor(RefinedSourceGen.next, red)
      callDao.create(call).databaseValue.futureValue
    }

    "list calls" in withPhoneAndTarget { (phone, target) =>
      numberDao.create(phone).databaseValue.futureValue
      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue
      val call = sampleFor(RefinedSourceGen.next, red)
      callDao.create(call).databaseValue.futureValue
      callDao.list(Filter.Empty, Full).databaseValue.futureValue should contain(call)
    }

    "list by redirect id" in withPhoneAndTarget { (phone, target) =>
      numberDao.create(phone).databaseValue.futureValue

      val redirect = generateRedirectV2(phone, target).next
      redirectDao.create(redirect).databaseValue.futureValue

      val call1 = sampleFor(RefinedSourceGen.next, redirect)
      callDao.create(call1).databaseValue.futureValue

      val call2 = sampleFor(RefinedSourceGen.next, redirect)
      callDao.create(call2).databaseValue.futureValue

      val filter =
        Filter.ByRedirectId(redirect.id, DateTime.now.minusYears(1), DateTime.now)
      callDao.list(filter, Page(0, 10)).databaseValue.futureValue.size shouldEqual 2
    }

    "list by qualifier" in withPhoneAndTarget { (phone, target) =>
      val redirect1 = generateRedirectV2(makeSomePhone(), target).next
      redirectDao.create(redirect1).databaseValue.futureValue
      val call1 = sampleFor(RefinedSourceGen.next, redirect1)
      callDao.create(call1).databaseValue.futureValue

      val next: ActualRedirect = generateRedirectV2(makeSomePhone(), target).next
      val objectId: ObjectId = QualifierGen.next
      val redirect2 = next.copy(key = next.key.copy(objectId = objectId))
      redirectDao.create(redirect2).databaseValue.futureValue
      val call2 = sampleFor(RefinedSourceGen.next, redirect2)
      callDao.create(call2).databaseValue.futureValue

      val filter = Filter.ByObjectId(objectId, DateTime.now.minusYears(1), DateTime.now)
      callDao.list(filter, Page(0, 10)).databaseValue.futureValue.toList shouldEqual List(call2)
    }

    "skip creation if exists" in withPhoneAndTarget { (phone, target) =>
      numberDao.create(phone).databaseValue.futureValue

      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue
      val call = sampleFor(RefinedSourceGen.next, red)

      callDao.create(call).databaseValue.futureValue shouldEqual true
      callDao.create(call).databaseValue.futureValue shouldEqual false
//      callDao.list(Filter.Empty, Page(0, 10)).databaseValue.futureValue.size shouldEqual 1
    }

    "ignore insert call if exists" in withPhoneAndTarget { (phone, target) =>
      numberDao.create(phone).databaseValue.futureValue

      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue
      val call = sampleFor(RefinedSourceGen.next, red)
      callDao.create(call).databaseValue.futureValue

      val newCall = call.copy(externalId = "1234")
      val res = callDao.create(newCall).databaseValue.futureValue
      res shouldEqual false
    }

    "list limit call" in withPhoneAndTarget { (phone, target) =>
      numberDao.create(phone).databaseValue.futureValue

      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue

      val call = sampleFor(RefinedSourceGen.next, red)
      callDao.create(call).databaseValue.futureValue
      val call2 = sampleFor(RefinedSourceGen.next, red)
      callDao.create(call2).databaseValue.futureValue
      val actual = callDao.list(Filter.Empty, 1).databaseValue
      actual.futureValue.size shouldEqual 1
    }
  }

  def makeSomePhone(): OperatorNumber = {
    val phone = forNumber(PhoneGen.next)
    numberDao.create(phone).databaseValue.futureValue
    phone
  }
}
