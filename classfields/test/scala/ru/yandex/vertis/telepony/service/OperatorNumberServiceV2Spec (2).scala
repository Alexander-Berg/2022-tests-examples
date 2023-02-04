package ru.yandex.vertis.telepony.service

import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.SampleHelper._
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.OperatorNumberDaoV2.Filter
import ru.yandex.vertis.telepony.generator.Generator.{originOperatorGen, OperatorAccountGen, PhoneGen}
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.Status.Downtimed
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.OperatorNumberServiceV2.DistributionKey
import ru.yandex.vertis.telepony.util.Range

/**
  * @author evans
  */
trait OperatorNumberServiceV2Spec extends SpecBase with BeforeAndAfterEach {
  def clean(): Unit

  def operatorNumberServiceV2: OperatorNumberServiceV2

  def defaultMtsAccount: OperatorAccount

  def otherMtsAccount: OperatorAccount

  override protected def beforeEach(): Unit = {
    clean()
    super.beforeEach()
  }

  val Vyborg = 15

  "Operator number service" should {
    "create number" in {
      val phone = PhoneGen.next
      val request = createNumberRequest(phone)
      operatorNumberServiceV2.create(request).futureValue
      operatorNumberServiceV2.get(phone).futureValue.number shouldEqual phone
    }
    "get number" in {
      val phone = PhoneGen.next
      val request = createNumberRequest(phone)
      operatorNumberServiceV2.create(request).futureValue
      val opn = operatorNumberServiceV2.get(phone).futureValue
      opn.account shouldEqual request.account
      opn.number shouldEqual request.number
    }
    "list numbers" in {
      val phone = PhoneGen.next
      val phone2 = PhoneGen.next
      val request = createNumberRequest(phone)
      val request2 = createNumberRequest(phone2)

      operatorNumberServiceV2.create(request).futureValue
      operatorNumberServiceV2.create(request2).futureValue
      val first = operatorNumberServiceV2.get(phone).futureValue
      val second = operatorNumberServiceV2.get(phone2).futureValue
      val list = operatorNumberServiceV2.list(Filter.Actual, Range.Full).futureValue
      list.toSet shouldEqual Set(first, second)
    }
    "list numbers by status" in {
      val phone = PhoneGen.next
      val phone2 = PhoneGen.next
      val request = createNumberRequest(phone)
      val request2 = createNumberRequest(phone2)

      operatorNumberServiceV2.create(request).futureValue
      operatorNumberServiceV2.create(request2).futureValue
      val updateRequest = OperatorNumberServiceV2.UpdateRequestV2(Some(1), None, Some(Downtimed(None)))
      operatorNumberServiceV2.update(phone, updateRequest).futureValue
      val first = operatorNumberServiceV2.get(phone).futureValue
      operatorNumberServiceV2.get(phone2).futureValue
      val list = operatorNumberServiceV2.list(Filter.ByStatus(StatusValues.Downtimed), Range.Full).futureValue
      list.toSet shouldEqual Set(first)
    }
    "update number" in {
      val phone = PhoneGen.next
      val request = createNumberRequest(phone)
      val opn = operatorNumberServiceV2.create(request).futureValue
      val updateRequest = OperatorNumberServiceV2.UpdateRequestV2(
        Some(Vyborg),
        Some(PhoneTypes.Mobile),
        None
      )
      val opn1 = operatorNumberServiceV2.update(phone, updateRequest).futureValue
      val opn2 = operatorNumberServiceV2.get(phone).futureValue
      opn1 shouldEqual opn2
      opn1 shouldNot be(opn)
      opn1.geoId shouldEqual Vyborg
      opn1.phoneType shouldEqual PhoneTypes.Mobile
    }

    "not allow to update status to busy" in {
      val phone = PhoneGen.next
      val request = createNumberRequest(phone)
      operatorNumberServiceV2.create(request).futureValue
      val updateRequest = OperatorNumberServiceV2.UpdateRequestV2(
        None,
        None,
        Some(Status.Busy(None))
      )

      operatorNumberServiceV2.update(phone, updateRequest).failed.futureValue shouldBe an[IllegalArgumentException]
    }

    "create with new status" in {
      val phone = PhoneGen.next
      val request = createNumberRequest(phone)
      operatorNumberServiceV2.create(request).futureValue.status.value shouldEqual StatusValues.New
    }
    "create phones with different accounts" in {
      val phone = PhoneGen.next
      val account = OperatorAccountGen.next
      val originOperator = originOperatorGen(account.operator).next
      operatorNumberServiceV2
        .create(createNumberRequest(phone, account = account, originOperator = originOperator))
        .futureValue
      val opn = operatorNumberServiceV2.get(phone).futureValue
      opn.number shouldBe phone
      opn.account shouldBe account
    }
    "compareStatusAndSet modify success" in {
      val phone = PhoneGen.next
      val request = createNumberRequest(phone)
      val opn = operatorNumberServiceV2.create(request).futureValue
      val isModified = operatorNumberServiceV2.compareStatusAndSet(StatusValues.New, opn).futureValue
      isModified shouldEqual true
    }
    "compareStatusAndSet no modify" in {
      val phone = PhoneGen.next
      val request = createNumberRequest(phone)
      val opn = operatorNumberServiceV2.create(request).futureValue
      val isModified = operatorNumberServiceV2.compareStatusAndSet(StatusValues.Downtimed, opn).futureValue
      isModified shouldEqual false
    }
    "return status count statistics" in {
      val request =
        OperatorNumberServiceV2.CreateRequest(
          PhoneGen.next,
          OperatorAccounts.MtsShared,
          Operators.Mts,
          Some(1),
          Some(PhoneTypes.Local),
          None
        )
      operatorNumberServiceV2.create(request).futureValue
      val map = operatorNumberServiceV2.statusDistributions().futureValue
      map should have size 1
      val key =
        DistributionKey(request.account.operator, request.originOperator, request.phoneType.get, request.geoId.get)
      val statusCount = map(key)
      import statusCount._
      total shouldEqual 1
      StatusValues.values.foreach { status =>
        if (status == StatusValues.New) {
          count(status) shouldEqual 1
          percent(status) shouldEqual 1.0
        } else {
          count(status) shouldEqual 0
          percent(status) shouldEqual 0.0
        }
      }
    }
  }
}
