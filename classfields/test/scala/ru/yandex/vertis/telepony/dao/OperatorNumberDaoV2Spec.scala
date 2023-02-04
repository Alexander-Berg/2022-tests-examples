package ru.yandex.vertis.telepony.dao

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.dao.OperatorNumberDaoV2.{Filter, LockRequest}
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.util.Range.Full
import ru.yandex.vertis.telepony.util.Page
import ru.yandex.vertis.telepony.util.sliced.SlicedResult
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}
import slick.dbio.DBIOAction

/**
  * Specs on [[OperatorNumberDaoV2]].
  *
  * @author dimas
  */
trait OperatorNumberDaoV2Spec extends SpecBase with BeforeAndAfterEach with DatabaseSpec {

  def dao: OperatorNumberDaoV2

  private val Moscow = 213

  override protected def beforeEach(): Unit = {
    dao.clear().databaseValue.futureValue
    super.beforeEach()
  }

  private val sampleNumber =
    OperatorNumber(
      PhoneGen.next,
      OperatorAccounts.MtsShared,
      Operators.Mts,
      Moscow,
      PhoneTypes.Local,
      Status.New(None),
      None
    )

  private val Spb = 1
  "OperatorNumberDao" should {
    "create number" in {
      dao.create(sampleNumber).databaseValue.futureValue
      dao.get(sampleNumber.number).databaseValue.futureValue.value should ===(sampleNumber)
    }

    "list numbers by empty filter" in {
      dao.create(sampleNumber).databaseValue.futureValue
      val page = Page(0, 10)
      val res = dao.list(Filter.Actual, page).databaseValue.futureValue
      res should be(SlicedResult(Iterable(sampleNumber), 1, page))
    }

    "filter by operator" in {
      dao.create(sampleNumber).databaseValue.futureValue
      val res = dao.list(Filter.ByOperatorActual(Operators.Mts), Full).databaseValue.futureValue
      res.toSeq shouldEqual Seq(sampleNumber)
    }

    "filter by deadline" in {
      val status = Status.New(Some(DateTime.now.minusDays(1)))
      val number2 = sampleNumber.copy(status = status)
      dao.create(number2).databaseValue.futureValue
      val res = dao.list(Filter.ByDeadline(), Full).databaseValue.futureValue
      res.toSeq shouldEqual Seq(number2)
    }

    "filter by operator's account" in {
      val account1 = OperatorAccounts.MtsShared
      val account2 = OperatorAccounts.MttShared
      val originOperator1 = Operators.Mts
      val originOperator2 = Operators.Mtt
      val number1 = OperatorNumberGen.next.copy(account = account1, originOperator = originOperator1)
      val number2 = OperatorNumberGen.next.copy(account = account2, originOperator = originOperator2)
      val page = Page(0, 10)

      dao.create(number1).databaseValue.futureValue
      dao.create(number2).databaseValue.futureValue

      dao.list(Filter.ByOperatorAccountAll(account1), page).databaseValue.futureValue.toSeq shouldBe Seq(number1)

      dao.list(Filter.ByOperatorAccountAll(account2), page).databaseValue.futureValue.toSeq shouldBe Seq(number2)
    }

    "filter by operator's account actual" in {
      val account1 = OperatorAccounts.MtsShared
      val account2 = OperatorAccounts.MttShared
      val originOperator1 = Operators.Mts
      val originOperator2 = Operators.Mtt
      val number1 = OperatorNumberGen.next.copy(
        account = account1,
        originOperator = originOperator1,
        status = Status.Ready(None)
      )
      val number2 = OperatorNumberGen.next.copy(
        account = account1,
        originOperator = originOperator1,
        status = Status.Deleted()
      )
      val number3 = OperatorNumberGen.next.copy(
        account = account2,
        originOperator = originOperator2,
        status = Status.Ready(None)
      )
      val page = Page(0, 10)

      dao.create(number1).databaseValue.futureValue
      dao.create(number2).databaseValue.futureValue
      dao.create(number3).databaseValue.futureValue

      dao.list(Filter.ByOperatorAccountActual(account1), page).databaseValue.futureValue.toSeq shouldBe Seq(number1)

      dao.list(Filter.ByOperatorAccountActual(account2), page).databaseValue.futureValue.toSeq shouldBe Seq(number3)

      dao
        .list(Filter.ByOperatorAccountActual(OperatorAccounts.BillingRealty), page)
        .databaseValue
        .futureValue shouldBe empty
    }

    "filter ByTypeGeoAndAccountAvailable" in {
      val gen = for {
        opn <- OperatorNumberGen
        geoId <- Gen.choose(1, 10)
      } yield opn.copy(geoId = geoId)

      val account = OperatorAccounts.MtsShared
      val originOperator = Operators.Mts
      val geoId = 1
      val phoneType = PhoneTypes.Mobile

      val markerPhone = OperatorNumberGen.next.copy(
        account = account,
        originOperator = originOperator,
        geoId = geoId,
        phoneType = phoneType,
        status = Status.Ready(None)
      )
      val phones = gen.next(20).toSeq :+ markerPhone

      DBIOAction.sequence(phones.map(opn => dao.create(opn))).databaseValue.futureValue

      val filter = Filter.AvailableForRelease(phoneType, geoId, account, Set(StatusValues.Ready, StatusValues.New))
      val found = dao.list(filter).databaseValue.futureValue

      found.contains(markerPhone) shouldBe true

      found.foreach { opn =>
        opn.account shouldBe account
        opn.geoId shouldBe geoId
        opn.phoneType shouldBe phoneType
        Set(StatusValues.New, StatusValues.Ready).contains(opn.status.value) shouldBe true
      }
    }

    "filter ByTypeGeoAndAccountAndStatusAvailable" in {
      val account = OperatorAccounts.MtsShared
      val originOperator = Operators.Mts
      val geoId = 1
      val phoneType = PhoneTypes.Mobile

      val markerPhone = OperatorNumberGen.next.copy(
        account = account,
        originOperator = originOperator,
        geoId = geoId,
        phoneType = phoneType,
        status = Status.Garbage(None)
      )

      val markerPhone2 = OperatorNumberGen.next.copy(
        account = account,
        originOperator = originOperator,
        geoId = geoId,
        phoneType = phoneType,
        status = Status.Downtimed(None)
      )

      val phones = OperatorNumberGen.next(20).toSeq :+ markerPhone :+ markerPhone2

      DBIOAction.sequence(phones.map(opn => dao.create(opn))).databaseValue.futureValue

      val filter =
        Filter.AvailableForRelease(phoneType, geoId, account, Set(StatusValues.Garbage, StatusValues.Downtimed))
      val found = dao.list(filter).databaseValue.futureValue

      found.contains(markerPhone) shouldBe true
      found.contains(markerPhone2) shouldBe true

      found.foreach { opn =>
        opn.account shouldBe account
        opn.geoId shouldBe geoId
        opn.phoneType shouldBe phoneType
        Set(StatusValues.Garbage, StatusValues.Downtimed) should contain(opn.status.value)
      }
    }

    "update" in {
      dao.create(sampleNumber).databaseValue.futureValue
      val modified = sampleNumber.copy(geoId = Spb, phoneType = PhoneTypes.Mobile)
      dao.update(modified).databaseValue.futureValue
      dao.get(sampleNumber.number).databaseValue.futureValue.value should ===(modified)
    }

    "delete number" in {
      dao.create(sampleNumber).databaseValue.futureValue
      dao.get(sampleNumber.number).databaseValue.futureValue.value should ===(sampleNumber)
      dao.delete(sampleNumber.number).databaseValue.futureValue
      dao.get(sampleNumber.number).databaseValue.futureValue shouldBe empty
    }

    "properly handle same number after few actions" in {
      dao.create(sampleNumber).databaseValue.futureValue
      dao.update(sampleNumber.copy(geoId = Spb)).databaseValue.futureValue
      val x = dao.get(sampleNumber.number).databaseValue.futureValue.value
      dao.update(x.copy(geoId = Moscow)).databaseValue.futureValue
      dao.get(sampleNumber.number).databaseValue.futureValue.value.geoId shouldEqual Moscow
    }

    "lock" in {
      val number = sampleNumber.copy(status = Status.Ready(None))
      dao.create(number).databaseValue.futureValue
      val locked = dao
        .lock(
          LockRequest.ByParameters(number.geoId, PhoneTypes.Local, sampleNumber.operator, sampleNumber.originOperator)
        )
        .databaseValue
        .futureValue
        .get
      val expected = sampleNumber.copy(status = Status.Acquiring(None))
      locked.copy(status = Status(locked.status.value, None, expected.status.updateTime)) shouldEqual expected
    }

    "lock with phone type fails if no available phone" in {
      val number = sampleNumber.copy(status = Status.Ready(None))
      dao.create(number).databaseValue.futureValue
      val f = dao
        .lock(
          LockRequest.ByParameters(number.geoId, PhoneTypes.Mobile, sampleNumber.operator, sampleNumber.originOperator)
        )
        .databaseValue
        .futureValue
      f shouldEqual None
    }
    "lock with phone type local" in {
      val number = sampleNumber.copy(status = Status.Ready(None))
      dao.create(number).databaseValue.futureValue
      val locked = dao
        .lock(
          LockRequest.ByParameters(number.geoId, PhoneTypes.Local, sampleNumber.operator, sampleNumber.originOperator)
        )
        .databaseValue
        .futureValue
        .get
      val expected = sampleNumber.copy(status = Status.Acquiring(None))
      locked.copy(
        status = Status(locked.status.value, None, expected.status.updateTime)
      ) shouldEqual expected
    }
    "lock with operator fails if no available phone" in {
      val number = sampleNumber.copy(status = Status.Ready(None))
      dao.create(number).databaseValue.futureValue
      val lockRequest =
        LockRequest.ByParameters(number.geoId, PhoneTypes.Mobile, Operators.Mtt, Operators.Mtt)
      val f = dao.lock(lockRequest).databaseValue.futureValue
      f shouldEqual None
    }
    "lock with operator" in {
      val number = sampleNumber
        .copy(
          status = Status.Ready(None),
          account = OperatorAccounts.MttShared,
          originOperator = Operators.Mtt
        )
      dao.create(number).databaseValue.futureValue
      val lockRequest =
        LockRequest.ByParameters(number.geoId, PhoneTypes.Local, Operators.Mtt, Operators.Mtt)
      val locked = dao.lock(lockRequest).databaseValue.futureValue.get
      val expected = number.copy(status = Status.Acquiring(None))
      locked.copy(
        status = Status(locked.status.value, None, expected.status.updateTime)
      ) shouldEqual expected
    }
    "lock with operator number fails if no available phone" in {
      val number = sampleNumber.copy(status = Status.Ready(None))
      val anotherNumber = PhoneGen.suchThat(n => n != number.number).next
      dao.create(number).databaseValue.futureValue
      val lockRequest = LockRequest.ByNumber(anotherNumber)
      val f = dao.lock(lockRequest).databaseValue.futureValue
      f shouldEqual None
    }
    "lock with operator number" in {
      val number = sampleNumber
        .copy(
          status = Status.Ready(None),
          account = OperatorAccounts.MttShared,
          originOperator = Operators.Mtt
        )
      dao.create(number).databaseValue.futureValue
      val lockRequest = LockRequest.ByNumber(number.number)
      val locked = dao.lock(lockRequest).databaseValue.futureValue.get
      val expected = number.copy(status = Status.Acquiring(None))
      locked.copy(
        status = Status(locked.status.value, None, expected.status.updateTime)
      ) shouldEqual expected
    }

    "compareStatusAndUpdate" when {
      "prevStatus as expected" should {
        "return true" in {
          val number = sampleNumber.copy(status = Status.Ready(None))
          dao.create(number).databaseValue.futureValue
          val isModified = dao.compareStatusAndUpdate(StatusValues.Ready, number).databaseValue.futureValue
          isModified shouldEqual true
        }
      }
      "prevStatus not equal actual" should {
        "return false" in {
          val number = sampleNumber.copy(status = Status.Ready(None))
          dao.create(number).databaseValue.futureValue
          val isModified = dao.compareStatusAndUpdate(StatusValues.Downtimed, number).databaseValue.futureValue
          isModified shouldEqual false
        }
      }
    }

    "return statistics" in {
      val number = sampleNumber.copy(status = Status.Ready(None))
      dao.create(number).databaseValue.futureValue
      val stat = dao.statistics.databaseValue.futureValue
      stat should have size 1
      val actual = stat.head
      import actual._
      operator shouldEqual number.account.operator
      phoneType shouldEqual number.phoneType
      geoId shouldEqual number.geoId
      statusValue shouldEqual number.status.value
      count shouldEqual 1
    }

    "find last used" in {
      dao.create(sampleNumber).databaseValue.futureValue
      val target = PhoneGen.next
      val number = sampleNumber.copy(status = Status.Downtimed(None), lastTarget = Some(target))
      dao.compareStatusAndUpdate(StatusValues.New, number).databaseValue.futureValue

      val actualNumber = dao.findLastUsed(target, StatusValues.Downtimed, None).databaseValue.futureValue
      actualNumber shouldEqual Some(number)

      val actualNumber2 = dao.findLastUsed(PhoneGen.next, StatusValues.Downtimed, None).databaseValue.futureValue
      actualNumber2 shouldEqual None
    }

    "find last used with phone type" in {
      val number = sampleNumber.copy(phoneType = PhoneTypes.Local)
      dao.create(number).databaseValue.futureValue
      val target = PhoneGen.next
      val downtimedNumber = number.copy(status = Status.Downtimed(None), lastTarget = Some(target))
      dao.compareStatusAndUpdate(StatusValues.New, downtimedNumber).databaseValue.futureValue

      val actualNumber =
        dao.findLastUsed(target, StatusValues.Downtimed, Some(PhoneTypes.Local)).databaseValue.futureValue
      actualNumber shouldEqual Some(downtimedNumber)

      val actualNumber2 =
        dao.findLastUsed(PhoneGen.next, StatusValues.Downtimed, Some(PhoneTypes.Mobile)).databaseValue.futureValue
      actualNumber2 shouldEqual None
    }
  }
}
