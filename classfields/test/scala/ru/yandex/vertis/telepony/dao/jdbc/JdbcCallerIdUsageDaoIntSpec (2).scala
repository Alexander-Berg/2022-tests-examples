package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.CallerIdUsageDao.FindRequest
import ru.yandex.vertis.telepony.dao.{CallerIdUsageDao, OperatorNumberDaoV2}
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.util.{JdbcSpecTemplate, Threads}
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}

import scala.concurrent.Future
import scala.concurrent.duration._

class JdbcCallerIdUsageDaoIntSpec extends SpecBase with JdbcSpecTemplate with DatabaseSpec {
  import Threads.lightWeightTasksEc

  private lazy val numberDao: OperatorNumberDaoV2 = new JdbcOperatorNumberDaoV2(TypedDomains.autoru_def)
  private lazy val callerIdDao: CallerIdUsageDao = new JdbcCallerIdUsageDao(dualDb, TypedDomains.autoru_def)

  override def beforeEach(): Unit = {
    super.beforeEach()
    numberDao.clear().databaseValue.futureValue
    callerIdDao.asInstanceOf[JdbcCallerIdUsageDao].clear().futureValue
  }

  "CallerIdUsageDao" should {
    "create" in {
      callerIdDao.markUsed("usage-id", Phone("+79817757575")).futureValue
    }

    "find" in {
      val newNumber = OperatorNumber(
        Phone("+79818889901"),
        OperatorAccounts.MtsShared,
        Operators.Mts,
        geoId = 1,
        PhoneTypes.Mobile,
        Status.New.forDuration(1.hour),
        None
      )
      val readyNumber = OperatorNumber(
        Phone("+79818889902"),
        OperatorAccounts.MtsShared,
        Operators.Mts,
        geoId = 1,
        PhoneTypes.Mobile,
        Status.Ready.forDuration(3.hour),
        None
      )
      val busyNumber = OperatorNumber(
        Phone("+79818889903"),
        OperatorAccounts.MtsShared,
        Operators.Mts,
        geoId = 1,
        PhoneTypes.Mobile,
        Status.Busy.forDuration(2.hour),
        None
      )
      Future
        .sequence(
          Seq(newNumber, readyNumber, busyNumber).map(numberDao.create).map(_.databaseValue)
        )
        .futureValue

      val findRequest =
        FindRequest("usage-id", geoIds = Set(1), PhoneTypes.Mobile, Set(Operators.Mts), Set(Operators.Mts), limit = 10)
      val phones = callerIdDao.find(findRequest).futureValue

      phones should have size 1
      phones.head should ===(readyNumber.number)
    }

    "find not used numbers first" in {
      val Seq(number1, number2, number3) = Seq("+79818889904", "+79818889905", "+79818889906")
        .map(Phone.apply)
        .map { number =>
          OperatorNumber(
            number,
            OperatorAccounts.MtsShared,
            Operators.Mts,
            geoId = 10174,
            PhoneTypes.Mobile,
            Status.Ready.forDuration(1.hour),
            None
          )
        }

      Future
        .sequence(
          Seq(number1, number2, number3).map(numberDao.create).map(_.databaseValue)
        )
        .futureValue

      val usageId = "usage-id"
      callerIdDao.markUsed(usageId, number1.number).futureValue
      callerIdDao.markUsed("another-usage-id", number2.number).futureValue

      val findRequest = FindRequest(
        usageId,
        geoIds = Set(1, 10174),
        PhoneTypes.Mobile,
        Set(Operators.Mts),
        Set(Operators.Mts),
        limit = 10
      )
      val phones = callerIdDao.find(findRequest).futureValue

      phones should have size 3
      phones should ===(Seq(number2.number, number3.number, number1.number))
    }

    "find the least recently used" in {
      val Seq(number1, number2) = Seq("+79818889904", "+79818889905")
        .map(Phone.apply)
        .map { number =>
          OperatorNumber(
            number,
            OperatorAccounts.MtsShared,
            Operators.Mts,
            geoId = 10174,
            PhoneTypes.Mobile,
            Status.Ready.forDuration(1.hour),
            None
          )
        }

      Future
        .sequence(
          Seq(number1, number2).map(numberDao.create).map(_.databaseValue)
        )
        .futureValue

      val usageId = "least"
      callerIdDao.markUsed(usageId, number1.number).futureValue
      Thread.sleep(1000)
      callerIdDao.markUsed(usageId, number2.number).futureValue

      val findRequest = FindRequest(
        usageId,
        geoIds = Set(1, 10174),
        PhoneTypes.Mobile,
        Set(Operators.Mts),
        Set(Operators.Mts),
        limit = 10
      )
      val phones = callerIdDao.find(findRequest).futureValue

      phones should have size 2
      (phones.toSeq should contain).theSameElementsInOrderAs(Seq(number1.number, number2.number))

      //new usage of the same phone
      Thread.sleep(1000)
      callerIdDao.markUsed(usageId, number1.number).futureValue
      val phones2 = callerIdDao.find(findRequest).futureValue
      (phones2.toSeq should contain).theSameElementsInOrderAs(Seq(number2.number, number1.number))
    }
  }

}
